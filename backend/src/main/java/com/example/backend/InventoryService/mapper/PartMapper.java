package com.example.backend.InventoryService.mapper;

import com.example.backend.InventoryService.dtos.part.CreatePartRequest;
import com.example.backend.InventoryService.dtos.part.UpdatePartRequest;
import com.example.backend.InventoryService.dtos.part.PartResponse;
import com.example.backend.InventoryService.entity.Part;

public final class PartMapper {

    private PartMapper() {
    }

    public static Part toEntity(
            CreatePartRequest request
    ) {
        Part part = new Part();

        part.setPartNumber(
                request.getPartNumber()
        );

        part.setDescription(
                request.getDescription()
        );

        part.setUnitOfMeasure(
                request.getUnitOfMeasure()
        );

        part.setStandardCost(
                request.getStandardCost()
        );

        part.setActive(true);

        return part;
    }

    public static void updateEntity(
            Part part,
            UpdatePartRequest request
    ) {
        part.setDescription(
                request.getDescription()
        );

        part.setUnitOfMeasure(
                request.getUnitOfMeasure()
        );

        part.setStandardCost(
                request.getStandardCost()
        );

        part.setActive(
                request.getActive()
        );
    }

    public static PartResponse toResponse(
            Part part
    ) {
        PartResponse response =
                new PartResponse();

        response.setId(
                part.getId()
        );

        response.setPartNumber(
                part.getPartNumber()
        );

        response.setDescription(
                part.getDescription()
        );

        response.setUnitOfMeasure(
                part.getUnitOfMeasure()
        );

        response.setStandardCost(
                part.getStandardCost()
        );

        response.setActive(
                part.getActive()
        );

        response.setVersion(
                part.getVersion()
        );

        return response;
    }
}