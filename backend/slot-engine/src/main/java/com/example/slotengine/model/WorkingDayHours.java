package com.example.slotengine.model;

import java.time.LocalTime;
import java.util.Objects;

/**
 * One day's opening period, half-open as {@code [open, close)}.
 */
public record WorkingDayHours(LocalTime open, LocalTime close) {

    public WorkingDayHours {
        Objects.requireNonNull(open, "open");
        Objects.requireNonNull(close, "close");
        if (!close.isAfter(open)) {
            throw new IllegalArgumentException(
                    "close " + close + " must be after open " + open);
        }
    }

    public int minutes() {
        return (int) java.time.Duration.between(open, close).toMinutes();
    }
}
