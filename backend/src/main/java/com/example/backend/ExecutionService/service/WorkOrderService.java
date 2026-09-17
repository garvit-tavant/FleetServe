package com.example.backend.ExecutionService.service;

import java.util.List;

import com.example.backend.ExecutionService.dto.workorder.CompleteWorkOrderRequest;
import com.example.backend.ExecutionService.dto.workorder.WorkOrderDetailsResponse;
import com.example.backend.ExecutionService.dto.workorder.WorkOrderResponse;
import com.example.backend.ExecutionService.dto.workorderlabour.WorkOrderLabourCreateRequest;

public interface WorkOrderService {

    WorkOrderResponse startWorkOrder(
            Long workOrderId);

    WorkOrderResponse completeWorkOrder(
            Long workOrderId,
            CompleteWorkOrderRequest request,
            String idempotencyKey);

    List<WorkOrderResponse> getAllWorkOrders();

    WorkOrderDetailsResponse getWorkOrder(
            Long workOrderId);

    WorkOrderDetailsResponse addLabour(
            Long workOrderId,
            WorkOrderLabourCreateRequest request);
}