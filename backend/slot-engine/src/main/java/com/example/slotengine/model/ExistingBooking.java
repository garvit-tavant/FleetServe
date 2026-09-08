package com.example.slotengine.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * A booking already holding a bay and a technician, as {@code [start, end)}.
 *
 * <p>The half-open convention is what makes an abutting booking not an overlap:
 * 09:00-11:00 and 11:00-13:00 can both stand.
 *
 * <p>{@code bayId} or {@code technicianId} may be null when a booking occupies
 * only one of the two resources.
 */
public record ExistingBooking(
        LocalDate date,
        LocalTime start,
        LocalTime end,
        Long bayId,
        Long technicianId) {

    public ExistingBooking {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException(
                    "booking end " + end + " must be after start " + start);
        }
    }
}
