package com.example.backend.InventoryService.service.impl;

import com.example.backend.AssetManagamentService.exception.BusinessValidationException;
import com.example.backend.AssetManagamentService.exception.DuplicateResourceException;
import com.example.backend.AssetManagamentService.exception.ResourceNotFoundException;
import com.example.backend.InventoryService.dtos.part.CreatePartRequest;
import com.example.backend.InventoryService.dtos.part.PartResponse;
import com.example.backend.InventoryService.dtos.part.UpdatePartRequest;
import com.example.backend.InventoryService.entity.Part;
import com.example.backend.InventoryService.mapper.PartMapper;
import com.example.backend.InventoryService.repository.PartRepository;
import com.example.backend.InventoryService.service.PartService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class PartServiceImpl implements PartService {

    private final PartRepository partRepository;

    public PartServiceImpl(
            PartRepository partRepository
    ) {
        this.partRepository = partRepository;
    }

    @Override
    @Transactional
    public PartResponse createPart(
            CreatePartRequest request
    ) {
        validateCreateRequest(request);

        String partNumber =
                normalizePartNumber(request.getPartNumber());

        if (partRepository.existsByPartNumber(partNumber)) {
            throw new DuplicateResourceException(
                    "Part already exists with part number "
                            + partNumber
            );
        }

        Part part = new Part();

        part.setPartNumber(partNumber);
        part.setDescription(
                request.getDescription().trim()
        );
        part.setUnitOfMeasure(
                normalizeUnitOfMeasure(
                        request.getUnitOfMeasure()
                )
        );
        part.setStandardCost(
                request.getStandardCost()
        );
        part.setActive(true);

        Part savedPart =
                partRepository.save(part);

        return PartMapper.toResponse(savedPart);
    }

    @Override
    @Transactional
    public PartResponse updatePart(
            Long partId,
            UpdatePartRequest request
    ) {
        validatePartId(partId);
        validateUpdateRequest(request);

        Part part = getPartEntity(partId);

        validateVersion(
                part,
                request.getVersion()
        );

        part.setDescription(
                request.getDescription().trim()
        );
        part.setUnitOfMeasure(
                normalizeUnitOfMeasure(
                        request.getUnitOfMeasure()
                )
        );
        part.setStandardCost(
                request.getStandardCost()
        );

        if (request.getActive() != null) {
            part.setActive(request.getActive());
        }

        Part updatedPart =
                partRepository.save(part);

        return PartMapper.toResponse(updatedPart);
    }

    @Override
    public PartResponse getPartById(
            Long partId
    ) {
        validatePartId(partId);

        return PartMapper.toResponse(
                getPartEntity(partId)
        );
    }

    @Override
    public Page<PartResponse> getAllParts(
            Pageable pageable
    ) {
        if (pageable == null) {
            throw new BusinessValidationException(
                    "Pageable information is required"
            );
        }

        return partRepository
                .findAll(pageable)
                .map(PartMapper::toResponse);
    }

    @Override
    @Transactional
    public void deactivatePart(
            Long partId,
            Long version
    ) {
        validatePartId(partId);

        Part part = getPartEntity(partId);

        validateVersion(part, version);

        if (Boolean.FALSE.equals(part.getActive())) {
            throw new BusinessValidationException(
                    "Part is already inactive"
            );
        }

        part.setActive(false);
        partRepository.save(part);
    }

    private Part getPartEntity(
            Long partId
    ) {
        return partRepository
                .findById(partId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Part not found with id "
                                        + partId
                        )
                );
    }

    private void validateCreateRequest(
            CreatePartRequest request
    ) {
        if (request == null) {
            throw new BusinessValidationException(
                    "Create part request is required"
            );
        }

        validatePartNumber(request.getPartNumber());
        validateDescription(request.getDescription());
        validateUnitOfMeasure(request.getUnitOfMeasure());
        validateStandardCost(request.getStandardCost());
    }

    private void validateUpdateRequest(
            UpdatePartRequest request
    ) {
        if (request == null) {
            throw new BusinessValidationException(
                    "Update part request is required"
            );
        }

        validateDescription(request.getDescription());
        validateUnitOfMeasure(request.getUnitOfMeasure());
        validateStandardCost(request.getStandardCost());

        if (request.getVersion() == null
                || request.getVersion() < 0) {

            throw new BusinessValidationException(
                    "Valid part version is required"
            );
        }
    }

    private void validatePartNumber(
            String partNumber
    ) {
        if (partNumber == null
                || partNumber.isBlank()) {

            throw new BusinessValidationException(
                    "Part number is required"
            );
        }

        if (partNumber.trim().length() > 100) {
            throw new BusinessValidationException(
                    "Part number cannot exceed 100 characters"
            );
        }
    }

    private void validateDescription(
            String description
    ) {
        if (description == null
                || description.isBlank()) {

            throw new BusinessValidationException(
                    "Part description is required"
            );
        }

        if (description.trim().length() > 500) {
            throw new BusinessValidationException(
                    "Part description cannot exceed 500 characters"
            );
        }
    }

    private void validateUnitOfMeasure(
            String unitOfMeasure
    ) {
        if (unitOfMeasure == null
                || unitOfMeasure.isBlank()) {

            throw new BusinessValidationException(
                    "Unit of measure is required"
            );
        }

        if (unitOfMeasure.trim().length() > 30) {
            throw new BusinessValidationException(
                    "Unit of measure cannot exceed 30 characters"
            );
        }
    }

    private void validateStandardCost(
            BigDecimal standardCost
    ) {
        if (standardCost == null) {
            throw new BusinessValidationException(
                    "Standard cost is required"
            );
        }

        if (standardCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessValidationException(
                    "Standard cost cannot be negative"
            );
        }
    }

    private void validatePartId(
            Long partId
    ) {
        if (partId == null || partId <= 0) {
            throw new BusinessValidationException(
                    "Part ID must be a positive number"
            );
        }
    }

    private void validateVersion(
            Part part,
            Long requestedVersion
    ) {
        if (requestedVersion == null) {
            throw new BusinessValidationException(
                    "Part version is required"
            );
        }

        if (!requestedVersion.equals(part.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(
                    Part.class,
                    part.getId()
            );
        }
    }

    private String normalizePartNumber(
            String partNumber
    ) {
        return partNumber
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private String normalizeUnitOfMeasure(
            String unitOfMeasure
    ) {
        return unitOfMeasure
                .trim()
                .toUpperCase(Locale.ROOT);
    }
}