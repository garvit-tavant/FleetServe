package com.example.backend.AssetManagamentService.service.impl;

import com.example.backend.AssetManagamentService.dto.duemaintenance.DueMaintenanceFilter;
import com.example.backend.AssetManagamentService.dto.duemaintenance.DueMaintenanceResponse;
import com.example.backend.AssetManagamentService.exception.BusinessValidationException;
import com.example.backend.AssetManagamentService.repository.DueMaintenanceRepository;
import com.example.backend.AssetManagamentService.repository.projection.DueMaintenanceProjection;
import com.example.backend.AssetManagamentService.service.DueMaintenanceService;
import com.example.backend.AssetManagamentService.status.DueStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
public class DueMaintenanceServiceImpl
        implements DueMaintenanceService {

    private final DueMaintenanceRepository dueRepository;
    private final Clock clock;

    public DueMaintenanceServiceImpl(
            DueMaintenanceRepository dueRepository,
            Clock clock
    ) {
        this.dueRepository = dueRepository;
        this.clock = clock;
    }

    @Override
    public Page<DueMaintenanceResponse> getDueMaintenance(
            DueMaintenanceFilter filter,
            Pageable pageable
    ) {
        validateOverrides(filter);

        // The due list is ordered by urgency inside the query. Passing a sorted
        // Pageable through would make Spring Data append a second ORDER BY to the
        // native statement, so the sort is deliberately dropped here.
        Pageable effective = PageRequest.of(
                pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), MAX_PAGE_SIZE)
        );

        return dueRepository.findDueMaintenance(
                        LocalDate.now(clock),
                        filter.distanceSoonThresholdOverride(),
                        filter.timeSoonThresholdOverride(),
                        filter.depotId(),
                        filter.assetClassCode(),
                        filter.status() == null ? null : filter.status().name(),
                        filter.dueOnly(),
                        effective
                )
                .map(this::toResponse);
    }

    /**
     * Thresholds normally come from the plan row. Only a supplied override needs
     * checking, and a negative window is meaningless rather than merely unusual.
     */
    private void validateOverrides(DueMaintenanceFilter filter) {
        BigDecimal distance = filter.distanceSoonThresholdOverride();
        if (distance != null && distance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessValidationException(
                    "Distance threshold cannot be negative"
            );
        }

        Integer days = filter.timeSoonThresholdOverride();
        if (days != null && days < 0) {
            throw new BusinessValidationException(
                    "Time threshold cannot be negative"
            );
        }
    }

    private DueMaintenanceResponse toResponse(
            DueMaintenanceProjection projection
    ) {
        DueMaintenanceResponse response =
                new DueMaintenanceResponse();

        response.setAssetId(projection.getAssetId());
        response.setVin(projection.getVin());
        response.setAssetClassCode(projection.getAssetClassCode());
        response.setMaintenancePlanCode(projection.getMaintenancePlanCode());
        response.setNextDueDate(projection.getNextDueDate());
        response.setNextDueKm(projection.getNextDueKm());
        response.setCurrentOdometerKm(projection.getCurrentOdometerKm());
        response.setKmRemaining(projection.getKmRemaining());
        response.setDaysRemaining(projection.getDaysRemaining());
        response.setDueStatus(DueStatus.valueOf(projection.getDueStatus()));

        return response;
    }
}
