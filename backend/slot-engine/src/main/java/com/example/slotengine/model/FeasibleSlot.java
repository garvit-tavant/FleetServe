package com.example.slotengine.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * One bookable combination of day, time, bay and technician.
 *
 * <p>The interval is half-open: {@code [start, end)}.
 */
public record FeasibleSlot(
        LocalDate date,
        LocalTime start,
        LocalTime end,
        long bayId,
        long technicianId) {

    public FeasibleSlot {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
    }

    public boolean overlaps(LocalDate otherDate, LocalTime otherStart, LocalTime otherEnd) {
        if (!date.equals(otherDate)) {
            return false;
        }
        return start.isBefore(otherEnd) && otherStart.isBefore(end);
    }
}
