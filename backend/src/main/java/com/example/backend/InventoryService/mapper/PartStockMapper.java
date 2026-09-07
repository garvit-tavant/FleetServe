package com.example.backend.InventoryService.mapper;

import com.example.backend.InventoryService.dtos.partstock.PartStockResponse;
import com.example.backend.InventoryService.entity.Part;
import com.example.backend.InventoryService.entity.PartStockView;

import java.math.BigDecimal;

public final class PartStockMapper {

    private PartStockMapper() {
    }

    public static PartStockResponse toResponse(
            PartStockView stockView,
            Part part,
            String workshopCode,
            BigDecimal reorderLevel
    ) {
        BigDecimal onHand =
                stockView.getOnHand() == null
                        ? BigDecimal.ZERO
                        : stockView.getOnHand();

        BigDecimal effectiveReorderLevel =
                reorderLevel == null
                        ? BigDecimal.ZERO
                        : reorderLevel;

        PartStockResponse response =
                new PartStockResponse();

        response.setPartId(
                part.getId()
        );

        response.setPartNumber(
                part.getPartNumber()
        );

        response.setPartDescription(
                part.getDescription()
        );

        response.setWorkshopId(
                stockView.getId()
                        .getWorkshopId()
        );

        response.setWorkshopCode(
                workshopCode
        );

        response.setOnHand(
                onHand
        );

        response.setReorderLevel(
                effectiveReorderLevel
        );

        response.setReorderRequired(
                onHand.compareTo(
                        effectiveReorderLevel
                ) <= 0
        );

        return response;
    }
}