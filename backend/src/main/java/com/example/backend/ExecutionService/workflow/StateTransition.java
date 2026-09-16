package com.example.backend.ExecutionService.workflow;

import com.example.backend.ExecutionService.status.WorkOrderStatus;

public record StateTransition(
        WorkOrderStatus currentState,
        WorkOrderEvent event
) {
}