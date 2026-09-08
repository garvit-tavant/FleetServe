package com.example.backend.ExecutionService.dto.booking;

import java.time.OffsetDateTime;

/**
 * A slot that was successfully held.
 */
public record BookingResponse(
        Long bookingId,
        Long assetId,
        Long workshopId,
        Long bayId,
        Long technicianId,
        OffsetDateTime slotStart,
        OffsetDateTime slotEnd,
        String kind,
        String status,
        Long maintenancePlanId,
        Long breakdownRequestId) {
}
