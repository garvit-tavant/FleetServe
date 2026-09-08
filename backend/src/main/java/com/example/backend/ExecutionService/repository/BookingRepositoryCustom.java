package com.example.backend.ExecutionService.repository;

import java.time.OffsetDateTime;
import java.util.List;

import com.example.backend.ExecutionService.repository.projection.BookedSlotProjection;

/**
 * Booking writes that JPA cannot express, because the {@code slot} column is a
 * PostgreSQL {@code tstzrange}.
 */
public interface BookingRepositoryCustom {

    /**
     * Inserts a booking and returns its generated id.
     *
     * <p>The range is built in SQL as {@code tstzrange(start, end, '[)')}, so the
     * exclusion constraints on (bay, slot) and (technician, slot) decide overlap.
     * A clash raises a constraint violation, which the global handler turns into
     * 409; the check is never made in Java, where a race could slip through.
     *
     * @param breakdownRequestId null for preventive work
     * @param maintenancePlanId  null for corrective work
     */
    long insertBooking(
            long assetId,
            long workshopId,
            long bayId,
            long technicianId,
            OffsetDateTime slotStart,
            OffsetDateTime slotEnd,
            String kind,
            Long maintenancePlanId,
            Long breakdownRequestId,
            String status);

    /**
     * Bookings that occupy a bay or technician anywhere in the window.
     *
     * <p>Scoped to HELD and CONFIRMED so it matches the partial predicate on the
     * exclusion constraints exactly. If the engine considered a different set
     * from the database, it would either propose slots the database then rejects
     * or hide slots that were genuinely free.
     */
    List<BookedSlotProjection> findOccupyingSlots(
            long workshopId,
            OffsetDateTime from,
            OffsetDateTime to);
}
