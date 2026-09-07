package com.example.backend.ExecutionService.service.impl;

import com.example.backend.ExecutionService.entity.WorkOrderPart;
import com.example.backend.ExecutionService.repository.WorkOrderPartRepository;
import com.example.backend.ExecutionService.service.WorkOrderPartService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class WorkOrderPartServiceImpl implements WorkOrderPartService {
    private final WorkOrderPartRepository repository;
    public WorkOrderPartServiceImpl(WorkOrderPartRepository repository) {
        this.repository = repository;
    }
    @Override
    @Transactional
    public WorkOrderPart save(WorkOrderPart workOrderPart) {
        return repository.save(workOrderPart);
    }
    @Override
    public List<WorkOrderPart> findByWorkOrderId(Long workOrderId) {
        return repository.findAll(); // TODO: implement filter by workOrderId
    }
}
