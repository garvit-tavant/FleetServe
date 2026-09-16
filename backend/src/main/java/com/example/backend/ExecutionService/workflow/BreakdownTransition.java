package com.example.backend.ExecutionService.workflow;

import com.example.backend.SLA.dto.BreakdownStatus;

public record BreakdownTransition(
    BreakdownStatus currentState,
    BreakdownEvent event
) {
}
