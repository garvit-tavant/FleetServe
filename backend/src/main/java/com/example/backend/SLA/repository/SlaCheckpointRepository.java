package com.example.backend.SLA.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.example.backend.SLA.dto.SlaComplianceReport;
import com.example.backend.SLA.dto.BookingAwaitingMinutesRow;

import com.example.backend.SLA.entity.SlaCheckpoint;

import jakarta.transaction.Transactional;

/**
 * SlaCheckpoint is keyed on booking_id: every booking - corrective (driven by
 * a breakdown request) or preventive (driven by a maintenance plan) - gets
 * exactly one checkpoint, so clock tracking and awaiting-parts pauses work
 * uniformly for both. The breakdown request, where one exists, is reached via
 * sc.booking.breakdownRequest.
 */
@Repository
public interface SlaCheckpointRepository extends JpaRepository<SlaCheckpoint, Long> {

    /**
     * returns the booking id and accumulated awaiting minutes for the completed work orders
     */
 


    @Query("select c.responseBreach from SlaCheckpoint c where c.booking.id = :bookingId")
    Boolean responseBreachByBookingId(@Param("bookingId") Long bookingId);

    @Query("select c.resolutionBreach from SlaCheckpoint c where c.booking.id = :bookingId")
    Boolean resolutionBreachByBookingId(@Param("bookingId") Long bookingId);

    @Query("select c.respondedAt from SlaCheckpoint c where c.booking.id = :bookingId")
    OffsetDateTime respondedAtByBookingId(@Param("bookingId") Long bookingId);

    @Query("select c.resolvedAt from SlaCheckpoint c where c.booking.id = :bookingId")
    OffsetDateTime resolvedAtByBookingId(@Param("bookingId") Long bookingId);

    @Query("select c.accumulatedAwaitingMinutes from SlaCheckpoint c where c.booking.id = :bookingId")
    Long accumulatedAwaitingMinutesByBookingId(@Param("bookingId") Long bookingId);

    @Query("select c.lastAwaitingRaisedAt from SlaCheckpoint c where c.booking.id = :bookingId")
    OffsetDateTime lastAwaitingRaisedAtByBookingId(@Param("bookingId") Long bookingId);

    @Query("select c from SlaCheckpoint c where c.booking.id = :bookingId")
    Optional<SlaCheckpoint> findByBookingId(@Param("bookingId") Long bookingId);

    @Modifying
    @Transactional
    @Query("update SlaCheckpoint c set c.responseBreach = true where c.booking.id = :bookingId")
    void updateResponseBreach(@Param("bookingId") Long bookingId);

    @Modifying
    @Transactional
    @Query("update SlaCheckpoint c set c.resolutionBreach = true where c.booking.id = :bookingId")
    void updateResolutionBreach(@Param("bookingId") Long bookingId);

    /**
     * Opens an awaiting-parts/approval pause: records the start time, only if
     * no pause is currently open (guards against double-raising). Returns 0
     * rows affected if a pause is already open (or no checkpoint exists), which
     * SlaCalculator.raiseAwaiting turns into an explicit error rather than a
     * silent no-op.
     */
    @Modifying
    @Transactional
    @Query("update SlaCheckpoint c set c.lastAwaitingRaisedAt = :raisedAt where c.booking.id = :bookingId and c.lastAwaitingRaisedAt is null")
    int raiseAwaiting(@Param("bookingId") Long bookingId, @Param("raisedAt") OffsetDateTime raisedAt);

    /**
     * Closes the currently open awaiting pause: adds the given elapsed minutes
     * to the running total and clears lastAwaitingRaisedAt. Callers compute the
     * elapsed minutes themselves (via the working-calendar strategy) before
     * calling this, since only application code knows which strategy applies.
     */
    @Modifying
    @Transactional
    @Query("update SlaCheckpoint c set c.accumulatedAwaitingMinutes = c.accumulatedAwaitingMinutes + :minutes, c.lastAwaitingRaisedAt = null where c.booking.id = :bookingId")
    void resolveAwaiting(@Param("bookingId") Long bookingId, @Param("minutes") long minutes);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE SlaCheckpoint sc
        SET sc.accumulatedAwaitingMinutes = sc.accumulatedAwaitingMinutes + :minutes,
            sc.lastAwaitingRaisedAt = NULL
        WHERE sc.booking.id = :bookingId
          AND sc.lastAwaitingRaisedAt = :expectedOpenSince
        """)
int resolveAwaitingIfOpen(@Param("bookingId") Long bookingId,
                          @Param("expectedOpenSince") OffsetDateTime expectedOpenSince,
                          @Param("minutes") long minutes);


    /**
     * Priority-wise SLA compliance. Restricted to corrective jobs
     * (breakdownRequest IS NOT NULL): preventive/maintenance-plan jobs have no
     * SlaPolicy and therefore no response target to comply with, so including
     * them would mix two different populations into one percentage.
     */
    @Query("""
        SELECT sc.booking.breakdownRequest.slaPolicy.priority AS priority,
        COUNT(sc.booking.breakdownRequest.id) AS evaluatedCases,
        (COUNT(sc.booking.breakdownRequest.id) - SUM(CASE WHEN sc.responseBreach = true THEN 1 ELSE 0 END)) AS compliantCases,
        ((COUNT(sc.booking.breakdownRequest.id) - SUM(CASE WHEN sc.responseBreach = true THEN 1 ELSE 0 END)) * 100.0 / COUNT(sc.booking.breakdownRequest.id)) AS compliancePercent
        FROM SlaCheckpoint sc
        WHERE sc.booking.breakdownRequest IS NOT NULL
          AND sc.booking.breakdownRequest.status = 'COMPLETED'
        GROUP BY sc.booking.breakdownRequest.slaPolicy.priority
        ORDER BY sc.booking.breakdownRequest.slaPolicy.priority
        """)
    List<SlaComplianceReport> findSlaComplianceMetrics();


    /**
     * Bulk fetch of accumulated awaiting-parts minutes, keyed by booking id,
     * for the given set of bookings. Used by SlaService.MeanTimeToRepair() to
     * avoid one query per booking (N+1) when computing MTTR across many
     * completed work orders at once.
     */
    @Query("""
            select new com.example.backend.SLA.dto.BookingAwaitingMinutesRow(
                sc.booking.id, sc.accumulatedAwaitingMinutes)
            from SlaCheckpoint sc
            where sc.booking.id in :bookingIds
            """)
    List<BookingAwaitingMinutesRow> findAccumulatedAwaitingMinutesForBookingIds(@Param("bookingIds") List<Long> bookingIds);

}
