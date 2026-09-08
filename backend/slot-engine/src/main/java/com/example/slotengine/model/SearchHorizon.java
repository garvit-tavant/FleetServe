package com.example.slotengine.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * The window the engine may search.
 *
 * <p>The caller supplies both the start date and the length, so the engine never
 * reads the system clock and a search is reproducible.
 *
 * <p>{@code numberOfDays} of zero is legal and yields no slots.
 */
public record SearchHorizon(LocalDate startDate, int numberOfDays) {

    public SearchHorizon {
        Objects.requireNonNull(startDate, "startDate");
        if (numberOfDays < 0) {
            throw new IllegalArgumentException(
                    "numberOfDays must not be negative: " + numberOfDays);
        }
    }

    /** Exclusive end of the horizon. */
    public LocalDate endDateExclusive() {
        return startDate.plusDays(numberOfDays);
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && date.isBefore(endDateExclusive());
    }
}
