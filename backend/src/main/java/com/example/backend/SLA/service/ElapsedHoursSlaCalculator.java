package com.example.backend.SLA.service;

import java.time.Instant;
import java.util.Collection;

import org.springframework.stereotype.Component;

import com.example.backend.SLA.calendar.PauseInterval;
import com.example.backend.SLA.calendar.WorkingCalendarArithmetic;
import com.example.backend.SLA.calendar.WorkshopCalendar;

/**
 * Measures service-level time as plain elapsed time, ignoring opening hours.
 *
 * <p>The second implementation required at delivery. Pauses are still removed,
 * because INV-7 excludes paused intervals on every basis, not only the
 * working-calendar one.
 *
 * <p>Also the safe fallback while a breakdown has not been booked yet and
 * therefore has no workshop calendar: counting continuously can only report a
 * clock as more consumed than a calendar basis would, so it never hides a
 * breach.
 */
@Component
public class ElapsedHoursSlaCalculator implements SlaCalculator {

    public static final String BASIS = "CALENDAR_TIME";

    @Override
    public String basis() {
        return BASIS;
    }

    @Override
    public long elapsedMinutes(
            Instant from,
            Instant to,
            WorkshopCalendar calendar,
            Collection<PauseInterval> pauses) {

        long elapsed = WorkingCalendarArithmetic.rawMinutesBetween(from, to);
        long paused = WorkingCalendarArithmetic.pausedRawMinutes(pauses, from, to);
        return Math.max(0L, elapsed - paused);
    }
}
