package com.example.backend.InventoryService.mapper;

import com.example.backend.InventoryService.dtos.inventorymovement.InventoryMovementResponse;
import com.example.backend.InventoryService.entity.InventoryMovement;

public final class InventoryMovementMapper {

    private InventoryMovementMapper() {
    }

    public static InventoryMovementResponse toResponse(
            InventoryMovement movement
    ) {
        InventoryMovementResponse response =
                new InventoryMovementResponse();

        response.setId(
                movement.getId()
        );

        response.setPartId(
                movement.getPart().getId()
        );

        response.setPartNumber(
                movement.getPart().getPartNumber()
        );

        response.setWorkshopId(
                movement.getWorkshop().getId()
        );

        response.setWorkshopCode(
                movement.getWorkshop().getCode()
        );

        response.setMovementType(
                movement.getMovementType().name()
        );

        response.setSignedQuantity(
                movement.getSignedQuantity()
        );

        response.setUnitCost(
                movement.getUnitCost()
        );

        response.setTransferReference(
                movement.getTransferReference()
        );

        response.setReason(
                movement.getReason()
        );

        response.setRecordedBy(
                movement.getRecordedBy()
        );

        response.setOccurredAt(
                movement.getOccurredAt()
        );

        return response;
    }
}