package com.example.backend.SLA.service;

import java.time.Instant;
import java.util.Collection;

import com.example.backend.SLA.calendar.PauseInterval;
import com.example.backend.SLA.calendar.WorkshopCalendar;

/**
 * The service-level calculation extension point (US-4.1).
 *
 * <p>This is the seam exercised by the blind extension test at the final demo:
 * a new measurement basis must be addable as one new class annotated with
 * {@code @Component}, plus reference data, with no edit to any existing class.
 * {@link SlaCalculatorRegistry} discovers implementations automatically and
 * selects one using {@code sla_policy.calendar_basis}.
 *
 * <p>Implementations must honour INV-7: never count hours outside the working
 * calendar, and never count a paused interval.
 */
public interface SlaCalculator {

    /**
     * The {@code sla_policy.calendar_basis} value this implementation serves.
     * Must be unique across implementations.
     */
    String basis();

    /**
     * Service-level minutes consumed between two instants, with pauses removed.
     *
     * @param from     start of the clock, normally when the breakdown was reported
     * @param to       evaluation point: the recorded timestamp, or now for a running clock
     * @param calendar the workshop working calendar, including its time zone
     * @param pauses   suspension intervals overlapping the window; may be empty
     * @return consumed minutes, never negative
     */
    long elapsedMinutes(
            Instant from,
            Instant to,
            WorkshopCalendar calendar,
            Collection<PauseInterval> pauses);
}
