package com.example.backend.SLA.service.impl;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;

import com.example.backend.SLA.dto.SlaBasis;
import com.example.backend.SLA.service.SlaTimeCalculationStrategy;

/**
 * Calendar-hours SLA time calculation strategy.
 * Computes pure elapsed time between two timestamps, ignoring working hours, holidays, and shift times.
 *
 * Example: Breakdown raised 17:00 Friday, responded 01:00 Saturday → 8 hours (pure elapsed)
 * regardless of workshop opening hours or holidays.
 *
 * Used when SlaPolicy.calendarBasis = CALENDAR_TIME.
 * Note: This strategy ignores the workshopID parameter since calendar time is 24/7.
 */
@Service 
public class CalendarHoursSlaTimeCalculationStrategy implements SlaTimeCalculationStrategy {

    @Override
    public long calculateElapsedTime(OffsetDateTime start, OffsetDateTime end, long workshopID) {
        // workshopID is ignored; calendar time is 24/7 regardless of workshop schedule
        return ChronoUnit.MINUTES.between(start, end);
    }

     @Override
    public SlaBasis supportedBasis() {
        return SlaBasis.CALENDAR_TIME;
    }
}
