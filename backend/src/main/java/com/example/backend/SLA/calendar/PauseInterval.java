package com.example.backend.SLA.calendar;

import java.time.Instant;

/**
 * An interval during which a service-level clock is suspended.
 *
 * <p>Two reasons exist, and both suspend the clock identically:
 * <ul>
 *   <li>{@code AWAITING_PARTS} - mandated by US-4.1.</li>
 *   <li>{@code DEPOT_UNREACHABLE} - left unstated by the specification and
 *       resolved in docs/open-questions.md entry 12.</li>
 * </ul>
 *
 * <p>An {@code end} of {@code null} means the pause is still open, in which case
 * the caller clips it to the evaluation instant.
 */
public record PauseInterval(String reason, Instant start, Instant end) {

    public static final String AWAITING_PARTS = "AWAITING_PARTS";
    public static final String DEPOT_UNREACHABLE = "DEPOT_UNREACHABLE";

    public boolean isOpen() {
        return end == null;
    }
}
