package com.example.backend.ExecutionService.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.backend.ExecutionService.entity.WorkOrderPart;

@Repository
public interface WorkOrderPartRepository
        extends JpaRepository<WorkOrderPart, Long> {

    List<WorkOrderPart> findByWorkOrder_Id(
            Long workOrderId);

    @Query("""
            SELECT COALESCE(
                SUM(p.quantity * p.unitCost),
                0
            )
            FROM WorkOrderPart p
            WHERE p.workOrder.id = :workOrderId
            """)
    BigDecimal calculateTotalPartCost(
            @Param("workOrderId")
            Long workOrderId);
}