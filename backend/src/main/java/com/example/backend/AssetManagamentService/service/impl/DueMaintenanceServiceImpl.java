package com.example.backend.AssetManagamentService.service.impl;

import com.example.backend.AssetManagamentService.dto.duemaintenance.DueMaintenanceResponse;
import com.example.backend.AssetManagamentService.exception.BusinessValidationException;
import com.example.backend.AssetManagamentService.repository.DueMaintenanceRepository;
import com.example.backend.AssetManagamentService.repository.projection.DueMaintenanceProjection;
import com.example.backend.AssetManagamentService.service.DueMaintenanceService;
import com.example.backend.AssetManagamentService.status.DueStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional(readOnly = true)
public class DueMaintenanceServiceImpl
        implements DueMaintenanceService {

    private final DueMaintenanceRepository dueRepository;

    public DueMaintenanceServiceImpl(
            DueMaintenanceRepository dueRepository
    ) {
        this.dueRepository = dueRepository;
    }

    @Override
    public Page<DueMaintenanceResponse> getDueMaintenance(
            BigDecimal distanceSoonThreshold,
            Integer timeSoonThresholdDays,
            Pageable pageable
    ) {
        if (distanceSoonThreshold == null
                || distanceSoonThreshold
                .compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessValidationException(
                    "Distance threshold cannot be negative"
            );
        }

        if (timeSoonThresholdDays == null
                || timeSoonThresholdDays < 0) {
            throw new BusinessValidationException(
                    "Time threshold cannot be negative"
            );
        }

        return dueRepository.findDueMaintenance(
                        distanceSoonThreshold,
                        timeSoonThresholdDays,
                        pageable
                )
                .map(this::toResponse);
    }

    private DueMaintenanceResponse toResponse(
            DueMaintenanceProjection projection
    ) {
        DueMaintenanceResponse response =
                new DueMaintenanceResponse();

        response.setAssetId(projection.getAssetId());
        response.setVin(projection.getVin());
        response.setAssetClassCode(
                projection.getAssetClassCode()
        );
        response.setMaintenancePlanCode(
                projection.getMaintenancePlanCode()
        );
        response.setNextDueDate(
                projection.getNextDueDate()
        );
        response.setNextDueKm(
                projection.getNextDueKm()
        );
        response.setDueStatus(
                DueStatus.valueOf(projection.getDueStatus())
        );

        return response;
    }
}