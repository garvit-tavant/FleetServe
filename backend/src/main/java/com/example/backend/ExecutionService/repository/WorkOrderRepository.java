package com.example.backend.ExecutionService.repository;

import com.example.backend.ExecutionService.entity.WorkOrder;

import java.time.OffsetDateTime;
import java.util.List;

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

   // mean time to repair calcualtion 
   // 

}
