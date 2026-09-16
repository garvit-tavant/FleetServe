package com.example.backend.ExecutionService.service;

import com.example.backend.ExecutionService.dto.workorder.WorkOrderResponse;

public interface WorkOrderResumeService {

    WorkOrderResponse resumeWork(
            Long workOrderId
    );

}
