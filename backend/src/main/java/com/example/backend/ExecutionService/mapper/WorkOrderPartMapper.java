package com.example.backend.ExecutionService.mapper;

import org.springframework.stereotype.Component;

import com.example.backend.ExecutionService.dto.workorderpart.WorkOrderPartResponse;
import com.example.backend.ExecutionService.entity.WorkOrderPart;

@Component
public class WorkOrderPartMapper {

    public WorkOrderPartResponse toResponse(
            WorkOrderPart workOrderPart) {

        WorkOrderPartResponse response =
                new WorkOrderPartResponse();

        response.setId(
                workOrderPart.getId());

        response.setPartId(
                workOrderPart.getPart().getId());

        response.setPartCode(
                workOrderPart.getPart().getPartNumber());

        response.setQuantity(
                workOrderPart.getQuantity());

        response.setUnitCost(
                workOrderPart.getUnitCost());

        response.setLineCost(
                workOrderPart.getQuantity()
                        .multiply(
                                workOrderPart.getUnitCost()));

        response.setMovementId(
                workOrderPart.getMovement().getId());

        return response;
    }
}