package com.example.backend.SLA.service;

import java.time.LocalTime;
import java.time.OffsetDateTime;

import com.example.backend.SLA.dto.SlaBasis;

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

    /**
     * Batch-friendly overload: caller pre-fetches shift hours once per workshop and
     * the holiday COUNT for this specific start/end range, then passes them in here.
     * We only ever need a count (not which dates) because clamping only cares how
     * many middle days to subtract — start/end dates are assumed to never be
     * holidays (see WorkingHoursSlaTimeCalculationStrategy). Used by batch
     * aggregations (e.g. SlaService.MeanTimeToRepair) to avoid one DB round-trip
     * per row when computing this over many bookings at once.
     *
     * Default delegates to the single-call overload (which fetches shift
     * hours/holiday count itself), so existing single-booking callers (SlaCalculator)
     * don't need to change. Strategies that don't consult shift hours/holidays
     * at all (e.g. CalendarHoursSlaTimeCalculationStrategy) can rely on this
     * default without overriding it.
     *
     * @param shiftStart   workshop's opening time, ignored by CALENDAR_TIME strategies
     * @param shiftEnd     workshop's closing time, ignored by CALENDAR_TIME strategies
     * @param holidayCount number of full-day holidays strictly between start and end
     *                     dates (exclusive of the start/end dates themselves)
     */
    default long calculateElapsedTime(OffsetDateTime start, OffsetDateTime end, long workshopID,
                                       LocalTime shiftStart, LocalTime shiftEnd,
                                       long holidayCount) {
        return calculateElapsedTime(start, end, workshopID);
    }

    SlaBasis supportedBasis();
}

