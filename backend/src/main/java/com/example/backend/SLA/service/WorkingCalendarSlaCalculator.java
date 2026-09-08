package com.example.backend.SLA.service;

import java.time.Instant;
import java.util.Collection;

import org.springframework.stereotype.Component;

import com.example.backend.SLA.calendar.PauseInterval;
import com.example.backend.SLA.calendar.WorkingCalendarArithmetic;
import com.example.backend.SLA.calendar.WorkshopCalendar;

/**
 * Measures service-level time against the workshop working calendar.
 *
 * <p>This is the basis US-4.1 demonstrates: a P2 raised at 17:00 on a Friday
 * with an 8-working-hour target is not breached at 01:00 on Saturday, because
 * the intervening closed hours consume none of the target.
 */
@Component
public class WorkingCalendarSlaCalculator implements SlaCalculator {

    public static final String BASIS = "WORKING_TIME";

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

        long open = WorkingCalendarArithmetic.workingMinutesBetween(from, to, calendar);
        long paused = WorkingCalendarArithmetic.pausedWorkingMinutes(pauses, from, to, calendar);
        return Math.max(0L, open - paused);
    }
}
