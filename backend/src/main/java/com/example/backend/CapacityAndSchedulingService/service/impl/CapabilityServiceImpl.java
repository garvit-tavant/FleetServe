package com.example.backend.CapacityAndSchedulingService.service.impl;

import com.example.backend.AssetManagamentService.exception.BusinessValidationException;
import com.example.backend.AssetManagamentService.exception.DuplicateResourceException;
import com.example.backend.AssetManagamentService.exception.ResourceNotFoundException;
import com.example.backend.CapacityAndSchedulingService.dto.capability.CapabilityResponse;
import com.example.backend.CapacityAndSchedulingService.dto.capability.CreateCapabilityRequest;
import com.example.backend.CapacityAndSchedulingService.entity.Capability;
import com.example.backend.CapacityAndSchedulingService.mapper.CapacitySchedulingMapper;
import com.example.backend.CapacityAndSchedulingService.repository.CapabilityRepository;
import com.example.backend.CapacityAndSchedulingService.service.CapabilityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CapabilityServiceImpl
        implements CapabilityService {

    private final CapabilityRepository capabilityRepository;

    private final CapacitySchedulingMapper mapper;

    public CapabilityServiceImpl(
            CapabilityRepository capabilityRepository,
            CapacitySchedulingMapper mapper
    ) {
        this.capabilityRepository = capabilityRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public CapabilityResponse createCapability(
            CreateCapabilityRequest request
    ) {

        validateCreateCapabilityRequest(request);

        String normalizedCapabilityCode =
                normalizeCapabilityCode(
                        request.getCapabilityCode()
                );

        String normalizedName =
                normalizeName(
                        request.getName()
                );

        if (capabilityRepository.existsById(
                normalizedCapabilityCode
        )) {

            throw new DuplicateResourceException(
                    "Capability already exists with code "
                            + normalizedCapabilityCode
            );
        }

        if (capabilityRepository.existsByName(
                normalizedName
        )) {

            throw new DuplicateResourceException(
                    "Capability already exists with name "
                            + normalizedName
            );
        }

        Capability capability =
                new Capability();

        capability.setCapabilityCode(
                normalizedCapabilityCode
        );

        capability.setName(
                normalizedName
        );

        capability.setDescription(
                request.getDescription()
        );

        Capability savedCapability =
                capabilityRepository.save(
                        capability
                );

        return mapper.toCapabilityResponse(
                savedCapability
        );
    }

    @Override
    public CapabilityResponse getCapability(
            String capabilityCode
    ) {

        Capability capability =
                getCapabilityEntity(
                        capabilityCode
                );

        return mapper.toCapabilityResponse(
                capability
        );
    }

    @Override
    public List<CapabilityResponse>
    getAllCapabilities() {

        return capabilityRepository
                .findAll()
                .stream()
                .map(
                        mapper::toCapabilityResponse
                )
                .toList();
    }

    private Capability getCapabilityEntity(
            String capabilityCode
    ) {

        validateCapabilityCode(
                capabilityCode
        );

        return capabilityRepository
                .findById(
                        capabilityCode
                )
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Capability not found with code "
                                                + capabilityCode
                                )
                );
    }

    private void validateCreateCapabilityRequest(
            CreateCapabilityRequest request
    ) {

        if (request == null) {

            throw new BusinessValidationException(
                    "Create capability request is required"
            );
        }

        if (request.getCapabilityCode() == null
                || request.getCapabilityCode().isBlank()) {

            throw new BusinessValidationException(
                    "Capability code is required"
            );
        }

        if (request.getName() == null
                || request.getName().isBlank()) {

            throw new BusinessValidationException(
                    "Capability name is required"
            );
        }

        if (request.getCapabilityCode()
                .trim()
                .length() > 50) {

            throw new BusinessValidationException(
                    "Capability code cannot exceed 50 characters"
            );
        }

        if (request.getName()
                .trim()
                .length() > 100) {

            throw new BusinessValidationException(
                    "Capability name cannot exceed 100 characters"
            );
        }

        if (request.getDescription() != null
                && request.getDescription().length() > 500) {

            throw new BusinessValidationException(
                    "Capability description cannot exceed 500 characters"
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
    }

    private String normalizeCapabilityCode(
            String capabilityCode
    ) {

        return capabilityCode
                .trim()
                .toUpperCase();
    }

    private String normalizeName(
            String name
    ) {

        return name
                .trim()
                .toUpperCase();
    }
}