package com.example.backend.InventoryService.service.impl;

import com.example.backend.AssetManagamentService.exception.BusinessValidationException;
import com.example.backend.AssetManagamentService.exception.ResourceNotFoundException;
import com.example.backend.CapacityAndSchedulingService.entity.Workshop;
import com.example.backend.CapacityAndSchedulingService.repository.WorkshopRepository;
import com.example.backend.InventoryService.dtos.inventorymovement.AdjustStockRequest;
import com.example.backend.InventoryService.dtos.inventorymovement.InventoryMovementResponse;
import com.example.backend.InventoryService.dtos.inventorymovement.IssuePartRequest;
import com.example.backend.InventoryService.dtos.inventorymovement.ReceiveStockRequest;
import com.example.backend.InventoryService.dtos.inventorymovement.ReturnPartRequest;
import com.example.backend.InventoryService.dtos.inventorymovement.TransferStockRequest;
import com.example.backend.InventoryService.entity.InventoryMovement;
import com.example.backend.InventoryService.entity.InventoryMovementType;
import com.example.backend.InventoryService.entity.Part;
import com.example.backend.InventoryService.entity.PartStockView;
import com.example.backend.InventoryService.mapper.InventoryMovementMapper;
import com.example.backend.InventoryService.repository.InventoryMovementRepository;
import com.example.backend.InventoryService.repository.PartRepository;
import com.example.backend.InventoryService.repository.PartStockViewRepository;
import com.example.backend.InventoryService.service.InventoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class InventoryServiceImpl
        implements InventoryService {

    private final InventoryMovementRepository movementRepository;
    private final PartRepository partRepository;
    private final PartStockViewRepository stockViewRepository;
    private final WorkshopRepository workshopRepository;

    public InventoryServiceImpl(
            InventoryMovementRepository movementRepository,
            PartRepository partRepository,
            PartStockViewRepository stockViewRepository,
            WorkshopRepository workshopRepository
    ) {
        this.movementRepository = movementRepository;
        this.partRepository = partRepository;
        this.stockViewRepository = stockViewRepository;
        this.workshopRepository = workshopRepository;
    }

    @Override
    @Transactional
    public InventoryMovementResponse receiveStock(
            ReceiveStockRequest request
    ) {
        validateReceiveRequest(request);

        Part part =
                lockAndGetActivePart(request.getPartId());

        Workshop workshop =
                getActiveWorkshop(
                        request.getWorkshopId()
                );

        InventoryMovement movement =
                createMovement(
                        part,
                        workshop,
                        InventoryMovementType.RECEIPT,
                        request.getQuantity(),
                        request.getUnitCost(),
                        null,
                        normalizeOptionalText(
                                request.getReason()
                        )
                );

        return InventoryMovementMapper.toResponse(
                movementRepository.save(movement)
        );
    }

    @Override
    @Transactional
    public InventoryMovementResponse issuePart(
            IssuePartRequest request
    ) {
        validateIssueRequest(request);

        Part part =
                lockAndGetActivePart(request.getPartId());

        Workshop workshop =
                getActiveWorkshop(
                        request.getWorkshopId()
                );

        BigDecimal available =
                getOnHand(
                        part.getId(),
                        workshop.getId()
                );

        validateAvailableStock(
                available,
                request.getQuantity()
        );

        InventoryMovement movement =
                createMovement(
                        part,
                        workshop,
                        InventoryMovementType.ISSUE,
                        request.getQuantity().negate(),
                        part.getStandardCost(),
                        null,
                        null
                );

        return InventoryMovementMapper.toResponse(
                movementRepository.save(movement)
        );
    }

    @Override
    @Transactional
    public InventoryMovementResponse returnPart(
            ReturnPartRequest request
    ) {
        validateReturnRequest(request);

        Part part =
                lockAndGetActivePart(request.getPartId());

        Workshop workshop =
                getActiveWorkshop(
                        request.getWorkshopId()
                );

        InventoryMovement movement =
                createMovement(
                        part,
                        workshop,
                        InventoryMovementType.RETURN,
                        request.getQuantity(),
                        part.getStandardCost(),
                        null,
                        normalizeOptionalText(
                                request.getReason()
                        )
                );

        return InventoryMovementMapper.toResponse(
                movementRepository.save(movement)
        );
    }

    @Override
    @Transactional
    public void transferStock(
            TransferStockRequest request
    ) {
        validateTransferRequest(request);

        Part part =
                lockAndGetActivePart(request.getPartId());

        Workshop sourceWorkshop =
                getActiveWorkshop(
                        request.getFromWorkshopId()
                );

        Workshop destinationWorkshop =
                getActiveWorkshop(
                        request.getToWorkshopId()
                );

        BigDecimal available =
                getOnHand(
                        part.getId(),
                        sourceWorkshop.getId()
                );

        validateAvailableStock(
                available,
                request.getQuantity()
        );

        String transferReference =
                generateTransferReference();

        InventoryMovement transferOut =
                createMovement(
                        part,
                        sourceWorkshop,
                        InventoryMovementType.TRANSFER_OUT,
                        request.getQuantity().negate(),
                        part.getStandardCost(),
                        transferReference,
                        normalizeOptionalText(
                                request.getReason()
                        )
                );

        InventoryMovement transferIn =
                createMovement(
                        part,
                        destinationWorkshop,
                        InventoryMovementType.TRANSFER_IN,
                        request.getQuantity(),
                        part.getStandardCost(),
                        transferReference,
                        normalizeOptionalText(
                                request.getReason()
                        )
                );

        /*
         * Both inserts occur in one transaction.
         * If either insert fails, both are rolled back.
         */
        movementRepository.save(transferOut);
        movementRepository.save(transferIn);
    }

    @Override
    @Transactional
    public InventoryMovementResponse adjustStock(
            AdjustStockRequest request
    ) {
        validateAdjustmentRequest(request);

        Part part =
                lockAndGetActivePart(request.getPartId());

        Workshop workshop =
                getActiveWorkshop(
                        request.getWorkshopId()
                );

        BigDecimal currentStock =
                getOnHand(
                        part.getId(),
                        workshop.getId()
                );

        BigDecimal resultingStock =
                currentStock.add(
                        request.getAdjustmentQuantity()
                );

        if (resultingStock.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessValidationException(
                    "Inventory adjustment would make stock negative. "
                            + "Current stock: "
                            + currentStock
                            + ", adjustment: "
                            + request.getAdjustmentQuantity()
            );
        }

        InventoryMovement movement =
                createMovement(
                        part,
                        workshop,
                        InventoryMovementType.ADJUSTMENT,
                        request.getAdjustmentQuantity(),
                        part.getStandardCost(),
                        null,
                        request.getReason().trim()
                );

        return InventoryMovementMapper.toResponse(
                movementRepository.save(movement)
        );
    }

    @Override
    public Page<InventoryMovementResponse>
    getMovementHistoryForPart(
            Long partId,
            Pageable pageable
    ) {
        validateId(partId, "Part ID");
        validatePageable(pageable);

        getPartEntity(partId);

        return movementRepository
                .findByPart_Id(
                        partId,
                        pageable
                )
                .map(InventoryMovementMapper::toResponse);
    }

    @Override
    public Page<InventoryMovementResponse>
    getMovementHistoryForWorkshop(
            Long workshopId,
            Pageable pageable
    ) {
        validateId(workshopId, "Workshop ID");
        validatePageable(pageable);

        getWorkshopEntity(workshopId);

        return movementRepository
                .findByWorkshop_Id(
                        workshopId,
                        pageable
                )
                .map(InventoryMovementMapper::toResponse);
    }

    @Override
    public Page<InventoryMovementResponse>
    getMovementHistoryForPartAndWorkshop(
            Long partId,
            Long workshopId,
            Pageable pageable
    ) {
        validateId(partId, "Part ID");
        validateId(workshopId, "Workshop ID");
        validatePageable(pageable);

        getPartEntity(partId);
        getWorkshopEntity(workshopId);

        return movementRepository
                .findByPart_IdAndWorkshop_Id(
                        partId,
                        workshopId,
                        pageable
                )
                .map(InventoryMovementMapper::toResponse);
    }

    private InventoryMovement createMovement(
            Part part,
            Workshop workshop,
            InventoryMovementType movementType,
            BigDecimal signedQuantity,
            BigDecimal unitCost,
            String transferReference,
            String reason
    ) {
        InventoryMovement movement =
                new InventoryMovement();

        movement.setPart(part);
        movement.setWorkshop(workshop);
        movement.setMovementType(movementType);
        movement.setSignedQuantity(signedQuantity);
        movement.setUnitCost(unitCost);
        movement.setTransferReference(
                transferReference
        );
        movement.setReason(reason);
        movement.setRecordedBy(
                getAuthenticatedUsername()
        );

        /*
         * If occurred_at has insertable = false and the database
         * supplies CURRENT_TIMESTAMP, remove this setter.
         */
        movement.setOccurredAt(
                OffsetDateTime.now(ZoneOffset.UTC)
        );

        return movement;
    }

    private Part lockAndGetActivePart(
            Long partId
    ) {
        validateId(partId, "Part ID");

        Part part =
                partRepository
                        .findByIdForInventoryUpdate(partId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Part not found with id "
                                                + partId
                                )
                        );

        if (!Boolean.TRUE.equals(part.getActive())) {
            throw new BusinessValidationException(
                    "Inventory movement cannot be recorded "
                            + "for inactive part "
                            + partId
            );
        }

        return part;
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

    private Workshop getActiveWorkshop(
            Long workshopId
    ) {
        validateId(workshopId, "Workshop ID");

        Workshop workshop =
                getWorkshopEntity(workshopId);

        if (!Boolean.TRUE.equals(workshop.getActive())) {
            throw new BusinessValidationException(
                    "Inventory movement cannot be recorded "
                            + "for inactive workshop "
                            + workshopId
            );
        }

        return workshop;
    }

    private BigDecimal getOnHand(
            Long partId,
            Long workshopId
    ) {
        return stockViewRepository
                .findByIdPartIdAndIdWorkshopId(
                        partId,
                        workshopId
                )
                .map(PartStockView::getOnHand)
                .orElse(BigDecimal.ZERO);
    }

    private void validateAvailableStock(
            BigDecimal available,
            BigDecimal requested
    ) {
        if (available.compareTo(requested) < 0) {
            throw new BusinessValidationException(
                    "Insufficient stock. Available: "
                            + available
                            + ", requested: "
                            + requested
            );
        }
    }

    private String getAuthenticatedUsername() {
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(
                authentication.getPrincipal()
        )) {

            throw new BusinessValidationException(
                    "Authenticated user is required "
                            + "to record an inventory movement"
            );
        }

        return authentication.getName();
    }

    private String generateTransferReference() {
        return "TRF-"
                + UUID.randomUUID()
                .toString()
                .toUpperCase();
    }

    private void validateReceiveRequest(
            ReceiveStockRequest request
    ) {
        if (request == null) {
            throw new BusinessValidationException(
                    "Receive-stock request is required"
            );
        }

        validateCommonMovementFields(
                request.getPartId(),
                request.getWorkshopId(),
                request.getQuantity()
        );

        if (request.getUnitCost() == null) {
            throw new BusinessValidationException(
                    "Unit cost is required"
            );
        }

        if (request.getUnitCost()
                .compareTo(BigDecimal.ZERO) < 0) {

            throw new BusinessValidationException(
                    "Unit cost cannot be negative"
            );
        }
    }

    private void validateIssueRequest(
            IssuePartRequest request
    ) {
        if (request == null) {
            throw new BusinessValidationException(
                    "Issue-part request is required"
            );
        }

        validateCommonMovementFields(
                request.getPartId(),
                request.getWorkshopId(),
                request.getQuantity()
        );
    }

    private void validateReturnRequest(
            ReturnPartRequest request
    ) {
        if (request == null) {
            throw new BusinessValidationException(
                    "Return-part request is required"
            );
        }

        validateCommonMovementFields(
                request.getPartId(),
                request.getWorkshopId(),
                request.getQuantity()
        );
    }

    private void validateTransferRequest(
            TransferStockRequest request
    ) {
        if (request == null) {
            throw new BusinessValidationException(
                    "Transfer-stock request is required"
            );
        }

        validateId(request.getPartId(), "Part ID");

        validateId(
                request.getFromWorkshopId(),
                "Source workshop ID"
        );

        validateId(
                request.getToWorkshopId(),
                "Destination workshop ID"
        );

        validatePositiveQuantity(
                request.getQuantity()
        );

        if (request.getFromWorkshopId()
                .equals(request.getToWorkshopId())) {

            throw new BusinessValidationException(
                    "Source and destination workshops "
                            + "cannot be the same"
            );
        }
    }

    private void validateAdjustmentRequest(
            AdjustStockRequest request
    ) {
        if (request == null) {
            throw new BusinessValidationException(
                    "Adjust-stock request is required"
            );
        }

        validateId(request.getPartId(), "Part ID");

        validateId(
                request.getWorkshopId(),
                "Workshop ID"
        );

        if (request.getAdjustmentQuantity() == null
                || request.getAdjustmentQuantity()
                .compareTo(BigDecimal.ZERO) == 0) {

            throw new BusinessValidationException(
                    "Adjustment quantity must be non-zero"
            );
        }

        if (request.getReason() == null
                || request.getReason().isBlank()) {

            throw new BusinessValidationException(
                    "Adjustment reason is required"
            );
        }

        if (request.getReason().trim().length() > 1000) {
            throw new BusinessValidationException(
                    "Adjustment reason cannot exceed 1000 characters"
            );
        }
    }

    private void validateCommonMovementFields(
            Long partId,
            Long workshopId,
            BigDecimal quantity
    ) {
        validateId(partId, "Part ID");
        validateId(workshopId, "Workshop ID");
        validatePositiveQuantity(quantity);
    }

    private void validatePositiveQuantity(
            BigDecimal quantity
    ) {
        if (quantity == null
                || quantity.compareTo(BigDecimal.ZERO) <= 0) {

            throw new BusinessValidationException(
                    "Quantity must be greater than zero"
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

    private void validatePageable(
            Pageable pageable
    ) {
        if (pageable == null) {
            throw new BusinessValidationException(
                    "Pageable information is required"
            );
        }
    }

    private String normalizeOptionalText(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}