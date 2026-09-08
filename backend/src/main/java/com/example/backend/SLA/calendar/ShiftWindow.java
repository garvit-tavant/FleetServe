package com.example.backend.SLA.calendar;

import java.time.LocalTime;
import java.util.Objects;

/**
 * A single open period within one day, in the workshop's local time.
 */
public record ShiftWindow(LocalTime open, LocalTime close) {

    public ShiftWindow {
        Objects.requireNonNull(open, "open");
        Objects.requireNonNull(close, "close");
        if (!close.isAfter(open)) {
            throw new IllegalArgumentException(
                    "close time " + close + " must be after open time " + open);
        }
    }

    public long minutes() {
        return java.time.Duration.between(open, close).toMinutes();
    }
}
