package com.example.backend.ExecutionService.repository;

import com.example.backend.ExecutionService.entity.WorkOrderPartRequirement;
import com.example.backend.ExecutionService.status.WorkOrderPartRequirementStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkOrderPartRequirementRepository
        extends JpaRepository<
        WorkOrderPartRequirement,
        Long> {

    List<WorkOrderPartRequirement>
    findByWorkOrder_Id(
            Long workOrderId
    );

    List<WorkOrderPartRequirement>
    findByWorkOrder_IdAndStatus(
            Long workOrderId,
            WorkOrderPartRequirementStatus status
    );

    long countByWorkOrder_IdAndStatus(
            Long workOrderId,
            WorkOrderPartRequirementStatus status
    );

    boolean existsByWorkOrder_IdAndStatus(
            Long workOrderId,
            WorkOrderPartRequirementStatus status
    );

    boolean existsByWorkOrder_IdAndPart_IdAndStatus(
            Long workOrderId,
            Long partId,
            WorkOrderPartRequirementStatus status
    );

    Optional<WorkOrderPartRequirement>
    findByWorkOrder_IdAndPart_IdAndStatus(Long workOrderId, Long partId, WorkOrderPartRequirementStatus status);

}