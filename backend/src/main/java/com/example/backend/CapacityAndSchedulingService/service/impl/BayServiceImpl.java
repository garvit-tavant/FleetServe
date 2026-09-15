package com.example.backend.CapacityAndSchedulingService.service.impl;

import com.example.backend.AssetManagamentService.exception.BusinessValidationException;
import com.example.backend.AssetManagamentService.exception.DuplicateResourceException;
import com.example.backend.AssetManagamentService.exception.ResourceNotFoundException;
import com.example.backend.CapacityAndSchedulingService.dto.bay.BayResponse;
import com.example.backend.CapacityAndSchedulingService.dto.bay.CreateBayRequest;
import com.example.backend.CapacityAndSchedulingService.entity.Bay;
import com.example.backend.CapacityAndSchedulingService.entity.Workshop;
import com.example.backend.CapacityAndSchedulingService.mapper.CapacitySchedulingMapper;
import com.example.backend.CapacityAndSchedulingService.repository.BayRepository;
import com.example.backend.CapacityAndSchedulingService.repository.WorkshopRepository;
import com.example.backend.CapacityAndSchedulingService.service.BayService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class BayServiceImpl implements BayService {

    private final BayRepository bayRepository;
    private final WorkshopRepository workshopRepository;
    private final CapacitySchedulingMapper mapper;

    public BayServiceImpl(
            BayRepository bayRepository,
            WorkshopRepository workshopRepository,
            CapacitySchedulingMapper mapper
    ) {
        this.bayRepository = bayRepository;
        this.workshopRepository = workshopRepository;
        this.mapper = mapper;
    }

    /**
     * Creates a Bay for exactly one Workshop.
     *
     * Bay codes are globally unique across FleetServe.
     * A code already assigned to any Workshop cannot be reused.
     */
    @Override
    @Transactional
    public BayResponse createBay(
            Long workshopId,
            CreateBayRequest request
    ) {
        validateWorkshopId(workshopId);
        validateCreateBayRequest(request);

        Workshop workshop = getWorkshopEntity(workshopId);

        if (!Boolean.TRUE.equals(workshop.getActive())) {
            throw new BusinessValidationException(
                    "Cannot create a bay inside inactive workshop "
                            + workshopId
            );
        }

        String normalizedBayCode =
                normalizeBayCode(request.getBayCode());

        if (bayRepository.existsByBayCode(normalizedBayCode)) {
            throw new DuplicateResourceException(
                    "Bay already exists with code "
                            + normalizedBayCode
            );
        }

        Bay bay = new Bay();

        bay.setWorkshop(workshop);
        bay.setBayCode(normalizedBayCode);
        bay.setIsActive(true);

        Bay savedBay = bayRepository.save(bay);

        return mapper.toBayResponse(savedBay);
    }

    @Override
    public BayResponse getBay(Long bayId) {
        Bay bay = getBayEntity(bayId);

        return mapper.toBayResponse(bay);
    }

    @Override
    public List<BayResponse> getBaysByWorkshop(
            Long workshopId
    ) {
        validateWorkshopId(workshopId);

        getWorkshopEntity(workshopId);

        return bayRepository
                .findByWorkshop_Id(workshopId)
                .stream()
                .map(mapper::toBayResponse)
                .toList();
    }

    @Override
    @Transactional
    public void activateBay(Long bayId) {
        Bay bay = getBayEntity(bayId);

        if (Boolean.TRUE.equals(bay.getIsActive())) {
            return;
        }

        Workshop workshop = bay.getWorkshop();

        if (!Boolean.TRUE.equals(workshop.getActive())) {
            throw new BusinessValidationException(
                    "Cannot activate bay "
                            + bayId
                            + " because workshop "
                            + workshop.getId()
                            + " is inactive"
            );
        }

        bay.setIsActive(true);

        bayRepository.save(bay);
    }

    @Override
    @Transactional
    public void deactivateBay(Long bayId) {
        Bay bay = getBayEntity(bayId);

        if (Boolean.FALSE.equals(bay.getIsActive())) {
            return;
        }

        /*
         * Add the Booking-module blocker check later:
         *
         * Reject deactivation when active or future
         * bookings reference this Bay.
         */

        bay.setIsActive(false);

        bayRepository.save(bay);
    }

    private Bay getBayEntity(Long bayId) {
        validateBayId(bayId);

        return bayRepository
                .findById(bayId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Bay not found with id "
                                        + bayId
                        )
                );
    }

    private Workshop getWorkshopEntity(Long workshopId) {
        return workshopRepository
                .findById(workshopId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Workshop not found with id "
                                        + workshopId
                        )
                );
    }

    private void validateCreateBayRequest(
            CreateBayRequest request
    ) {
        if (request == null) {
            throw new BusinessValidationException(
                    "Create-bay request is required"
            );
        }

        if (request.getBayCode() == null
                || request.getBayCode().isBlank()) {
            throw new BusinessValidationException(
                    "Bay code is required"
            );
        }

        if (request.getBayCode().trim().length() > 30) {
            throw new BusinessValidationException(
                    "Bay code cannot exceed 30 characters"
            );
        }
    }

    private void validateBayId(Long bayId) {
        if (bayId == null || bayId <= 0) {
            throw new BusinessValidationException(
                    "Bay ID must be a positive number"
            );
        }
    }

    private void validateWorkshopId(Long workshopId) {
        if (workshopId == null || workshopId <= 0) {
            throw new BusinessValidationException(
                    "Workshop ID must be a positive number"
            );
        }
    }

    private String normalizeBayCode(String bayCode) {
        return bayCode
                .trim()
                .toUpperCase();
    }
}