package com.example.backend.ExecutionService.repository.projection;

import java.time.OffsetDateTime;

/**
 * A booking's occupancy of a bay and a technician over a half-open interval.
 */
public record BookedSlotProjection(
        Long bookingId,
        Long bayId,
        Long technicianId,
        OffsetDateTime slotStart,
        OffsetDateTime slotEnd) {
}
