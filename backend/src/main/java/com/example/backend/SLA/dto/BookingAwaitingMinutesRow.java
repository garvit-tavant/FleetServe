package com.example.backend.SLA.dto;

/**
 * Row: booking_id -> accumulated awaiting-parts minutes, fetched in bulk (see
 * SlaCheckpointRepository.findAccumulatedAwaitingMinutesForBookingIds) to
 * avoid an N+1 query pattern when aggregating over many bookings at once
 * (e.g. SlaService.MeanTimeToRepair()).
 */
public class BookingAwaitingMinutesRow {
    private final Long bookingId;
    private final Long minutes;

    public BookingAwaitingMinutesRow(Long bookingId, Long minutes) {
        this.bookingId = bookingId;
        this.minutes = minutes;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public Long getMinutes() {
        return minutes;
    }
}
