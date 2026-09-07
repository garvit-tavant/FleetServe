package com.example.backend.InventoryService.service.impl;

import com.example.backend.AssetManagamentService.exception.BusinessValidationException;
import com.example.backend.AssetManagamentService.exception.DuplicateResourceException;
import com.example.backend.AssetManagamentService.exception.ResourceNotFoundException;
import com.example.backend.CapacityAndSchedulingService.entity.Workshop;
import com.example.backend.CapacityAndSchedulingService.repository.WorkshopRepository;
import com.example.backend.InventoryService.dtos.partreorderlevel.CreateOrUpdatePartReorderLevelRequest;
import com.example.backend.InventoryService.dtos.partreorderlevel.PartReorderLevelResponse;
import com.example.backend.InventoryService.entity.Part;
import com.example.backend.InventoryService.entity.PartReorderLevel;
import com.example.backend.InventoryService.mapper.PartReorderLevelMapper;
import com.example.backend.InventoryService.repository.PartReorderLevelRepository;
import com.example.backend.InventoryService.repository.PartRepository;
import com.example.backend.InventoryService.service.PartReorderLevelService;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PartReorderLevelServiceImpl
        implements PartReorderLevelService {

    private final PartReorderLevelRepository reorderLevelRepository;
    private final PartRepository partRepository;
    private final WorkshopRepository workshopRepository;

    public PartReorderLevelServiceImpl(
            PartReorderLevelRepository reorderLevelRepository,
            PartRepository partRepository,
            WorkshopRepository workshopRepository
    ) {
        this.reorderLevelRepository =
                reorderLevelRepository;
        this.partRepository = partRepository;
        this.workshopRepository = workshopRepository;
    }

    @Override
    @Transactional
    public PartReorderLevelResponse createReorderLevel(
            CreateOrUpdatePartReorderLevelRequest request
    ) {
        validateRequest(request);

        Long partId = request.getPartId();
        Long workshopId = request.getWorkshopId();

        if (reorderLevelRepository
                .existsByPart_IdAndWorkshop_Id(
                        partId,
                        workshopId
                )) {

            throw new DuplicateResourceException(
                    "Reorder level already exists for part "
                            + partId
                            + " and workshop "
                            + workshopId
            );
        }

        Part part = getPartEntity(partId);
        Workshop workshop =
                getWorkshopEntity(workshopId);

        if (!Boolean.TRUE.equals(part.getActive())) {
            throw new BusinessValidationException(
                    "Cannot configure a reorder level for inactive part "
                            + partId
            );
        }

        if (!Boolean.TRUE.equals(workshop.getActive())) {
            throw new BusinessValidationException(
                    "Cannot configure a reorder level for inactive workshop "
                            + workshopId
            );
        }

        PartReorderLevel entity =
                new PartReorderLevel();

        entity.setPart(part);
        entity.setWorkshop(workshop);
        entity.setReorderLevel(
                request.getReorderLevel()
        );

        PartReorderLevel saved =
                reorderLevelRepository.save(entity);

        return PartReorderLevelMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PartReorderLevelResponse updateReorderLevel(
            Long id,
            CreateOrUpdatePartReorderLevelRequest request,
            Long version
    ) {
        validateId(id, "Reorder level ID");
        validateRequest(request);
        validateVersionValue(version);

        PartReorderLevel entity =
                getReorderLevelEntity(id);

        validateVersion(entity, version);

        if (!entity.getPart()
                .getId()
                .equals(request.getPartId())
                || !entity.getWorkshop()
                .getId()
                .equals(request.getWorkshopId())) {

            throw new BusinessValidationException(
                    "Part and workshop cannot be changed "
                            + "when updating a reorder level"
            );
        }

        entity.setReorderLevel(
                request.getReorderLevel()
        );

        PartReorderLevel updated =
                reorderLevelRepository.save(entity);

        return PartReorderLevelMapper.toResponse(updated);
    }

    @Override
    public PartReorderLevelResponse getById(
            Long id
    ) {
        validateId(id, "Reorder level ID");

        return PartReorderLevelMapper.toResponse(
                getReorderLevelEntity(id)
        );
    }

    @Override
    public List<PartReorderLevelResponse> getByPart(
            Long partId
    ) {
        validateId(partId, "Part ID");
        getPartEntity(partId);

        return reorderLevelRepository
                .findByPart_Id(partId)
                .stream()
                .map(PartReorderLevelMapper::toResponse)
                .toList();
    }

    @Override
    public List<PartReorderLevelResponse> getByWorkshop(
            Long workshopId
    ) {
        validateId(workshopId, "Workshop ID");
        getWorkshopEntity(workshopId);

        return reorderLevelRepository
                .findByWorkshop_Id(workshopId)
                .stream()
                .map(PartReorderLevelMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deleteReorderLevel(
            Long id,
            Long version
    ) {
        validateId(id, "Reorder level ID");
        validateVersionValue(version);

        PartReorderLevel entity =
                getReorderLevelEntity(id);

        validateVersion(entity, version);

        reorderLevelRepository.delete(entity);
    }

    private PartReorderLevel getReorderLevelEntity(
            Long id
    ) {
        return reorderLevelRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Part reorder level not found with id "
                                        + id
                        )
                );
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

    private Workshop getWorkshopEntity(
            Long workshopId
    ) {
        return workshopRepository
                .findById(workshopId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Workshop not found with id "
                                        + workshopId
                        )
                );
    }

    private void validateRequest(
            CreateOrUpdatePartReorderLevelRequest request
    ) {
        if (request == null) {
            throw new BusinessValidationException(
                    "Reorder-level request is required"
            );
        }

        validateId(request.getPartId(), "Part ID");
        validateId(
                request.getWorkshopId(),
                "Workshop ID"
        );

        BigDecimal reorderLevel =
                request.getReorderLevel();

        if (reorderLevel == null) {
            throw new BusinessValidationException(
                    "Reorder level is required"
            );
        }

        if (reorderLevel.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessValidationException(
                    "Reorder level cannot be negative"
            );
        }
    }

    private void validateVersion(
            PartReorderLevel entity,
            Long version
    ) {
        if (!version.equals(entity.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(
                    PartReorderLevel.class,
                    entity.getId()
            );
        }
    }

    private void validateVersionValue(
            Long version
    ) {
        if (version == null || version < 0) {
            throw new BusinessValidationException(
                    "Valid reorder-level version is required"
            );
        }
    }

    private void validateId(
            Long id,
            String fieldName
    ) {
        if (id == null || id <= 0) {
            throw new BusinessValidationException(
                    fieldName + " must be a positive number"
            );
        }
    }
}