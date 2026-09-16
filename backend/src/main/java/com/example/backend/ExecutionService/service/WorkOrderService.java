package com.example.backend.ExecutionService.service;

import java.math.BigDecimal;

import com.example.backend.ExecutionService.dto.workorder.CompleteWorkOrderRequest;
import com.example.backend.ExecutionService.dto.workorder.WorkOrderResponse;

public interface WorkOrderService {

    WorkOrderResponse startWorkOrder(
            Long workOrderId);

    WorkOrderResponse completeWorkOrder(
            Long workOrderId,
            CompleteWorkOrderRequest request,
            String idempotencyKey);
}