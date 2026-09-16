package com.example.backend.ExecutionService.service;

import com.example.backend.ExecutionService.dto.workorder.CreatePartRequirementRequest;
import com.example.backend.ExecutionService.dto.workorder.WorkOrderResponse;

public interface WorkOrderAwaitPartsService {

    WorkOrderResponse awaitParts(
            Long workOrderId,
            CreatePartRequirementRequest request
    );
}
