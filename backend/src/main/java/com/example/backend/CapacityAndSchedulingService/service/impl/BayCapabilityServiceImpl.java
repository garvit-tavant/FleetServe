package com.example.backend.CapacityAndSchedulingService.service.impl;

import com.example.backend.AssetManagamentService.exception.BusinessValidationException;
import com.example.backend.AssetManagamentService.exception.DuplicateResourceException;
import com.example.backend.AssetManagamentService.exception.ResourceNotFoundException;
import com.example.backend.CapacityAndSchedulingService.dto.bayCapability.BayCapabilityResponse;
import com.example.backend.CapacityAndSchedulingService.entity.Bay;
import com.example.backend.CapacityAndSchedulingService.entity.BayCapability;
import com.example.backend.CapacityAndSchedulingService.entity.Capability;
import com.example.backend.CapacityAndSchedulingService.mapper.CapacitySchedulingMapper;
import com.example.backend.CapacityAndSchedulingService.repository.BayCapabilityRepository;
import com.example.backend.CapacityAndSchedulingService.repository.BayRepository;
import com.example.backend.CapacityAndSchedulingService.repository.CapabilityRepository;
import com.example.backend.CapacityAndSchedulingService.service.BayCapabilityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class BayCapabilityServiceImpl
        implements BayCapabilityService {

    private final BayCapabilityRepository bayCapabilityRepository;
    private final BayRepository bayRepository;
    private final CapabilityRepository capabilityRepository;
    private final CapacitySchedulingMapper mapper;

    public BayCapabilityServiceImpl(
            BayCapabilityRepository bayCapabilityRepository,
            BayRepository bayRepository,
            CapabilityRepository capabilityRepository,
            CapacitySchedulingMapper mapper
    ) {
        this.bayCapabilityRepository = bayCapabilityRepository;
        this.bayRepository = bayRepository;
        this.capabilityRepository = capabilityRepository;
        this.mapper = mapper;
    }

    /**
     * Assigns an existing Capability to an existing Bay.
     *
     * The combination of bay_id and capability_code
     * must be unique.
     */
    @Override
    @Transactional
    public void assignCapabilityToBay(
            Long bayId,
            String capabilityCode
    ) {
        validateBayId(bayId);
        validateCapabilityCode(capabilityCode);

        String normalizedCapabilityCode =
                normalizeCapabilityCode(capabilityCode);

        Bay bay = getBayEntity(bayId);

        Capability capability =
                getCapabilityEntity(normalizedCapabilityCode);

        boolean mappingAlreadyExists =
                bayCapabilityRepository
                        .existsByBay_IdAndCapability_CapabilityCode(
                                bayId,
                                normalizedCapabilityCode
                        );

        if (mappingAlreadyExists) {
            throw new DuplicateResourceException(
                    "Capability "
                            + normalizedCapabilityCode
                            + " is already assigned to bay "
                            + bayId
            );
        }

        BayCapability bayCapability = new BayCapability();

        bayCapability.setBay(bay);
        bayCapability.setCapability(capability);

        bayCapabilityRepository.save(bayCapability);
    }

    /**
     * Removes only the Bay-Capability mapping.
     *
     * The Bay and Capability records are not deleted.
     */
    @Override
    @Transactional
    public void removeCapabilityFromBay(
            Long bayId,
            String capabilityCode
    ) {
        validateBayId(bayId);
        validateCapabilityCode(capabilityCode);

        String normalizedCapabilityCode =
                normalizeCapabilityCode(capabilityCode);

        BayCapability bayCapability =
                bayCapabilityRepository
                        .findByBay_IdAndCapability_CapabilityCode(
                                bayId,
                                normalizedCapabilityCode
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "No mapping exists between bay "
                                                + bayId
                                                + " and capability "
                                                + normalizedCapabilityCode
                                )
                        );

        bayCapabilityRepository.delete(bayCapability);
    }

    /**
     * Returns all Capabilities assigned to the specified Bay.
     *
     * A nonexistent Bay returns 404.
     * An existing Bay without Capabilities returns an empty list.
     */
    @Override
    public List<BayCapabilityResponse> getCapabilitiesForBay(
            Long bayId
    ) {
        validateBayId(bayId);

        getBayEntity(bayId);

        return bayCapabilityRepository
                .findByBay_Id(bayId)
                .stream()
                .map(mapper::toBayCapabilityResponse)
                .toList();
    }

    private Bay getBayEntity(Long bayId) {
        return bayRepository
                .findById(bayId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Bay not found with id "
                                        + bayId
                        )
                );
    }

    private Capability getCapabilityEntity(
            String capabilityCode
    ) {
        return capabilityRepository
                .findById(capabilityCode)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Capability not found with code "
                                        + capabilityCode
                        )
                );
    }

    private void validateBayId(Long bayId) {
        if (bayId == null || bayId <= 0) {
            throw new BusinessValidationException(
                    "Bay ID must be a positive number"
            );
        }
    }

    private void validateCapabilityCode(
            String capabilityCode
    ) {
        if (capabilityCode == null
                || capabilityCode.isBlank()) {

            throw new BusinessValidationException(
                    "Capability code is required"
            );
        }

        if (capabilityCode.trim().length() > 50) {
            throw new BusinessValidationException(
                    "Capability code cannot exceed 50 characters"
            );
        }
    }

    private String normalizeCapabilityCode(
            String capabilityCode
    ) {
        return capabilityCode
                .trim()
                .toUpperCase();
    }
}