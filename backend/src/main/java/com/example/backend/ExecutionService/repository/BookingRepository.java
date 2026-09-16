package com.example.backend.ExecutionService.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.example.backend.ExecutionService.status.BookingKind;
import com.example.backend.ExecutionService.status.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.backend.ExecutionService.entity.Booking;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

   // Workshop is on the booking itself, so this resolves for preventive
   // (maintenance-plan) bookings too - unlike the old breakdown-request route,
   // which only existed for corrective jobs.
   @Query("SELECT b.workshop.id FROM Booking b WHERE b.id = :id")
   Long workshopIdByBookingId(@Param("id") Long id);

   // Null for preventive bookings: they have no breakdown request and therefore
   // no SlaPolicy targets.
   @Query("SELECT b.breakdownRequest.id FROM Booking b WHERE b.id = :id")
   Long breakdownRequestIdByBookingId(@Param("id") Long id);

   @Query(
           value = """
            SELECT b.*
            FROM booking b
            WHERE b.workshop_id = :workshopId
              AND b.status = 'CONFIRMED'
              AND b.slot && tstzrange(
                    :horizonStart,
                    :horizonEnd,
                    '[)'
              )
            ORDER BY lower(b.slot), b.id
            """,
           nativeQuery = true
   )
   List<Booking> findBlockingBookings(
           @Param("workshopId") Long workshopId,
           @Param("horizonStart") OffsetDateTime horizonStart,
           @Param("horizonEnd") OffsetDateTime horizonEnd
   );

   boolean existsByAsset_IdAndMaintenancePlan_IdAndStatusIn(Long id, Long id1, List<BookingStatus> held);

   boolean existsByBreakdownRequest_Id(Long breakdownRequestId);

   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("""
        SELECT b
        FROM Booking b
        LEFT JOIN FETCH b.workOrder wo
        LEFT JOIN FETCH b.breakdownRequest br
        WHERE b.id = :bookingId
        """)
   Optional<Booking> findByIdForCancellation(
           @Param("bookingId") Long bookingId);

   @Query("select b from Booking b")
   public List<Booking> findALl();


}
