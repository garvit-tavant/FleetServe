package com.example.slotengine.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * One bookable combination of day, time,
 * bay and technician.
 *
 * Interval is half-open:
 *
 * [start, end)
 */
public record FeasibleSlot(
        LocalDate date,
        LocalTime start,
        LocalTime end,
        long bayId,
        long technicianId
) {

    public FeasibleSlot {

        Objects.requireNonNull(
                date,
                "date"
        );

        Objects.requireNonNull(
                start,
                "start"
        );

        Objects.requireNonNull(
                end,
                "end"
        );

        if (!end.isAfter(start)) {
            throw new IllegalArgumentException(
                    "end must be after start"
            );
        }

        if (bayId <= 0) {
            throw new IllegalArgumentException(
                    "bayId must be positive"
            );
        }

        if (technicianId <= 0) {
            throw new IllegalArgumentException(
                    "technicianId must be positive"
            );
        }
    }

    public boolean overlaps(
            LocalDate otherDate,
            LocalTime otherStart,
            LocalTime otherEnd
    ) {
        if (!date.equals(otherDate)) {
            return false;
        }

        return start.isBefore(otherEnd)
                && otherStart.isBefore(end);
    }
}