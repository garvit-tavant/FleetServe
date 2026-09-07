package com.example.backend.ExecutionService.service;

import com.example.backend.ExecutionService.entity.WorkOrder;
import java.util.Optional;

public interface WorkOrderService {
    WorkOrder save(WorkOrder workOrder);
    Optional<WorkOrder> findById(Long id);
    void updateStatus(Long id, String status);
}
