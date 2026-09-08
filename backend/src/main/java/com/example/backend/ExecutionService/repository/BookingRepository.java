package com.example.backend.ExecutionService.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.backend.SLA.entity.Booking;

@Repository
public interface BookingRepository
        extends JpaRepository<Booking, Long>, BookingRepositoryCustom {

    /** Live bookings already held for an asset, used to refuse duplicates. */
    @Query("""
            select b
            from Booking b
            where b.assetId = :assetId
              and b.status in ('HELD', 'CONFIRMED')
            """)
    List<Booking> findLiveBookingsForAsset(@Param("assetId") long assetId);

    /**
     * The live booking for a breakdown, if one exists.
     *
     * <p>uk_booking_active_breakdown already stops a second live booking being
     * created; this lets the service fail with a clear message first.
     */
    @Query("""
            select b
            from Booking b
            where b.breakdownRequest.id = :breakdownRequestId
              and b.status in ('HELD', 'CONFIRMED', 'COMPLETED')
            """)
    Optional<Booking> findLiveBookingForBreakdown(
            @Param("breakdownRequestId") long breakdownRequestId);

    /** A live preventive booking for this asset under this plan, if any. */
    @Query("""
            select b
            from Booking b
            where b.assetId = :assetId
              and b.maintenancePlanId = :maintenancePlanId
              and b.status in ('HELD', 'CONFIRMED')
            """)
    Optional<Booking> findLivePreventiveBooking(
            @Param("assetId") long assetId,
            @Param("maintenancePlanId") long maintenancePlanId);
}
