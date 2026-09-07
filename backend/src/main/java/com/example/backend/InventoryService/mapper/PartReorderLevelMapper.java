package com.example.backend.InventoryService.mapper;

import com.example.backend.InventoryService.dtos.partreorderlevel.PartReorderLevelResponse;
import com.example.backend.InventoryService.entity.PartReorderLevel;

public final class PartReorderLevelMapper {

    private PartReorderLevelMapper() {
    }

    public static PartReorderLevelResponse toResponse(
            PartReorderLevel entity
    ) {
        PartReorderLevelResponse response =
                new PartReorderLevelResponse();

        response.setId(
                entity.getId()
        );

        response.setPartId(
                entity.getPart().getId()
        );

        response.setPartNumber(
                entity.getPart().getPartNumber()
        );

        response.setWorkshopId(
                entity.getWorkshop().getId()
        );

        response.setWorkshopCode(
                entity.getWorkshop().getCode()
        );

        response.setReorderLevel(
                entity.getReorderLevel()
        );

        response.setVersion(
                entity.getVersion()
        );

        return response;
    }
}