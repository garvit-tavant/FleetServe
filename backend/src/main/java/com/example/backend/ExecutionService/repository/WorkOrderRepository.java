package com.example.backend.ExecutionService.repository;

import com.example.backend.ExecutionService.dto.CompletedWork;
import com.example.backend.ExecutionService.entity.WorkOrder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.example.backend.ExecutionService.entity.WorkOrderLabour;
import com.example.backend.ExecutionService.status.WorkOrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {

   @Query ("select w.startedAt from WorkOrder w where w.booking.id = :id")
   OffsetDateTime findStartedAtByBookingId(@Param("id") Long breakdownId);

   // Mirrors the uk_work_order_booking DB constraint (one work order per booking);
   // checked before insert so we can return a clean 409/validation error instead
   // of letting the DB throw a constraint-violation exception.
   boolean existsByBooking_Id(Long bookingId);

   @Query("""
        select w.booking.id
        from WorkOrder w
        where w.status in (
            com.example.backend.ExecutionService.status.WorkOrderStatus.SCHEDULED,
            com.example.backend.ExecutionService.status.WorkOrderStatus.IN_PROGRESS,
            com.example.backend.ExecutionService.status.WorkOrderStatus.AWAITING_PARTS
        )
        """)
   List<Long> findActiveBookingIds();

   
   @Query("""
            select new com.example.backend.ExecutionService.dto.CompletedWork(
                w.booking.id, w.booking.workshop.id, w.booking.asset.assetClass.id, w.booking.asset.assetClass.code,
                w.startedAt, w.completedAt)
            from WorkOrder w
            where w.status = com.example.backend.ExecutionService.status.WorkOrderStatus.COMPLETED
              and w.startedAt is not null
              and w.completedAt is not null
           """)
   List<CompletedWork> findCompletedWork();


   @Query("""
        SELECT wo
        FROM WorkOrder wo
        JOIN FETCH wo.booking b
        WHERE b.asset.id = :assetId
          AND b.maintenancePlan.id = :maintenancePlanId
          AND b.kind = com.example.backend.ExecutionService.status.BookingKind.PREVENTIVE
          AND wo.status = com.example.backend.ExecutionService.status.WorkOrderStatus.COMPLETED
          AND wo.completedAt IS NOT NULL
          AND wo.odometerAtService IS NOT NULL
        ORDER BY wo.completedAt DESC, wo.id DESC
        """)
   List<WorkOrder> findCompletedPreventiveServices(
           @Param("assetId") Long assetId,
           @Param("maintenancePlanId") Long maintenancePlanId,
           Pageable pageable
   );

   Optional<WorkOrder> findByBooking_Id(
           Long bookingId
   );

   List<WorkOrder> findByStatusIn(
           List<WorkOrderStatus> statuses
   );

   long countByStatusIn(
           List<WorkOrderStatus> statuses
   );

   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("""
        SELECT wo
        FROM WorkOrder wo
        JOIN FETCH wo.booking b
        JOIN FETCH b.technician t
        JOIN FETCH t.appUser au
        LEFT JOIN FETCH b.breakdownRequest br
        WHERE wo.id = :workOrderId
        """)
   Optional<WorkOrder> findByIdForTransition(
           @Param("workOrderId")
           Long workOrderId);

      Optional<WorkOrder> findByIdempotencyKey(
              String idempotencyKey);

   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("""
        SELECT wo
        FROM WorkOrder wo
        JOIN FETCH wo.booking b
        JOIN FETCH b.technician t
        JOIN FETCH t.appUser au
        LEFT JOIN FETCH b.breakdownRequest br
        WHERE wo.id = :workOrderId
        """)
   Optional<WorkOrder> findByIdForStartWork(
           @Param("workOrderId") Long workOrderId);

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
