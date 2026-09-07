package com.example.backend.ExecutionService.service.impl;

import com.example.backend.ExecutionService.entity.WorkOrder;
import com.example.backend.ExecutionService.repository.WorkOrderRepository;
import com.example.backend.ExecutionService.service.WorkOrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class WorkOrderServiceImpl implements WorkOrderService {
    private final WorkOrderRepository repository;
    public WorkOrderServiceImpl(WorkOrderRepository repository) {
        this.repository = repository;
    }
    @Override
    @Transactional
    public WorkOrder save(WorkOrder workOrder) {
        return repository.save(workOrder);
    }
    @Override
    public Optional<WorkOrder> findById(Long id) {
        return repository.findById(id);
    }
    @Override
    @Transactional
    public void updateStatus(Long id, String status) {
        repository.findById(id).ifPresent(order -> {
            order.setStatus(status);
            repository.save(order);
        });
    }
}
