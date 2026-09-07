package com.example.backend.ExecutionService.service;

import com.example.backend.ExecutionService.entity.WorkOrderPart;
import java.util.List;

public interface WorkOrderPartService {
    WorkOrderPart save(WorkOrderPart workOrderPart);
    List<WorkOrderPart> findByWorkOrderId(Long workOrderId);
}
