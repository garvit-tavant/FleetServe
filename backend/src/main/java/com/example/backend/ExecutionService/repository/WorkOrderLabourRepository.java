package com.example.backend.ExecutionService.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.backend.ExecutionService.entity.WorkOrderLabour;

@Repository
public interface WorkOrderLabourRepository
        extends JpaRepository<WorkOrderLabour, Long> {

    List<WorkOrderLabour> findByWorkOrder_Id(
            Long workOrderId
    );

    boolean existsByWorkOrder_IdAndTechnician_Id(
            Long workOrderId,
            Long technicianId
    );
        Optional<WorkOrderLabour>
        findByWorkOrder_IdAndTechnician_Id(
                Long workOrderId,
                Long technicianId);

        @Query("""
            SELECT COALESCE(
                SUM(l.hours * l.rateApplied),
                0
            )
            FROM WorkOrderLabour l
            WHERE l.workOrder.id = :workOrderId
            """)
        BigDecimal calculateTotalLabourCost(
                @Param("workOrderId")
                Long workOrderId);

}