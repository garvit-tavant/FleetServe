package com.example.backend.ExecutionService.service.impl;

import com.example.backend.ExecutionService.entity.WorkOrderLabour;
import com.example.backend.ExecutionService.repository.WorkOrderLabourRepository;
import com.example.backend.ExecutionService.service.WorkOrderLabourService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class WorkOrderLabourServiceImpl implements WorkOrderLabourService {
    private final WorkOrderLabourRepository repository;
    public WorkOrderLabourServiceImpl(WorkOrderLabourRepository repository) {
        this.repository = repository;
    }
    @Override
    @Transactional
    public WorkOrderLabour save(WorkOrderLabour workOrderLabour) {
        return repository.save(workOrderLabour);
    }
    @Override
    public List<WorkOrderLabour> findByWorkOrderId(Long workOrderId) {
        return repository.findAll(); // TODO: implement filter by workOrderId
    }
}
