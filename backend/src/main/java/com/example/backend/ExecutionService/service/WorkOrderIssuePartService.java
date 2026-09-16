package com.example.backend.ExecutionService.service;

import com.example.backend.ExecutionService.dto.workorder.IssueWorkOrderPartRequest;
import com.example.backend.ExecutionService.dto.workorder.WorkOrderResponse;

public interface WorkOrderIssuePartService {

    WorkOrderResponse issuePart(
            Long workOrderId,
            IssueWorkOrderPartRequest request);
}