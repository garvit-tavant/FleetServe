package com.example.backend.SLA.service;

import java.time.OffsetDateTime;

/**
 * Strategy interface for computing elapsed SLA time between two timestamps.
 * Supports multiple basis calculations (working-hours, calendar-hours, etc.).
 *
 * This interface enables the Open/Closed principle: new calculation strategies
 * can be added without modifying SlaCalculator or SlaService. At runtime,
 * the appropriate strategy is selected based on SlaPolicy.calendarBasis.
 *
 * Implementations must:
 * - Respect workshop working calendars, holidays, and shift hours if applicable
 * - Handle same-day vs multi-day timestamps
 * - Return minutes elapsed between start and end
 * - Return 0 or negative only if end <= start (accounting for calendar/shift logic)
 */
public interface SlaTimeCalculationStrategy {

    /**
     * Compute elapsed SLA time between start and end according to this strategy's basis.
     *
     * @param start      Start timestamp (OffsetDateTime)
     * @param end        End timestamp (OffsetDateTime), typically later than start
     * @param workshopID Workshop ID to fetch calendar, shift hours, and holidays
     * @return Elapsed time in minutes according to this strategy
     *         (0 if end <= start when considering the strategy's basis)
     */
    long calculateElapsedTime(OffsetDateTime start, OffsetDateTime end, long workshopID);

}
