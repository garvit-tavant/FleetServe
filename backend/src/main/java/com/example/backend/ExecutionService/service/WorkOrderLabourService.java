package com.example.backend.ExecutionService.service;

import com.example.backend.ExecutionService.entity.WorkOrderLabour;
import java.util.List;

public interface WorkOrderLabourService {
    WorkOrderLabour save(WorkOrderLabour workOrderLabour);
    List<WorkOrderLabour> findByWorkOrderId(Long workOrderId);
}
