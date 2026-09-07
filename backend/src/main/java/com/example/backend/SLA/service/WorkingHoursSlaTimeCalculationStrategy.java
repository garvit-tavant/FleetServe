package com.example.backend.SLA.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import com.example.backend.CapacityAndSchedulingService.repository.HolidayRepository;
import com.example.backend.CapacityAndSchedulingService.repository.WorkingCalendarRepository;

/**
 * Working-hours SLA time calculation strategy.
 * Computes elapsed time respecting workshop working calendars, shift hours, and holidays.
 *
 * Example: If a workshop is open 08:00-17:00 Monday-Friday:
 * - Breakdown raised at 17:00 Friday, responded at 10:00 Monday → 9 hours (skip weekend)
 * - Breakdown raised at 16:00 Friday, responded at 09:00 next Monday → 2 hours Friday + 1 hour Monday = 3 hours
 *
 * This is the recommended basis per INV-7 specification and calendar_basis=WORKING_TIME policy.
 */
public class WorkingHoursSlaTimeCalculationStrategy implements SlaTimeCalculationStrategy {

    private final WorkingCalendarRepository workingCalendarRepository;
    private final HolidayRepository holidayRepository;

    public WorkingHoursSlaTimeCalculationStrategy(
            WorkingCalendarRepository workingCalendarRepository,
            HolidayRepository holidayRepository) {
        this.workingCalendarRepository = workingCalendarRepository;
        this.holidayRepository = holidayRepository;
    }

    @Override
    public long calculateElapsedTime(OffsetDateTime start, OffsetDateTime end, long workshopID) {
        LocalDate dateStart = start.toLocalDate();
        LocalDate dateEnd = end.toLocalDate();

        LocalTime timeStart = start.toLocalTime();
        LocalTime timeEnd = end.toLocalTime();

        LocalTime shiftStart = workingCalendarRepository.findopentimebywokrshopID(workshopID);
        LocalTime shiftEnd = workingCalendarRepository.findbyclosetimebyworkshopID(workshopID);
        long shiftMinutes = countMinutes(shiftStart, shiftEnd);

        // Same-day case: clamp both times to shift window and return direct difference
        if (dateStart.equals(dateEnd)) {
            LocalTime clampedStart = clampToShift(timeStart, shiftStart, shiftEnd);
            LocalTime clampedEnd = clampToShift(timeEnd, shiftStart, shiftEnd);
            return Math.max(0, countMinutes(clampedStart, clampedEnd));
        }

        // Multi-day case:
        // Day 1 contribution: minutes from startTime to end-of-shift
        // Day N contribution: minutes from start-of-shift to endTime
        // Middle days: full shift minutes per day, minus holidays

        long countHolidays = holidayRepository.countHoldidayinbetween(workshopID, dateStart, dateEnd);
        long dayCount = countDays(dateStart, dateEnd);

        long day1Minutes = clampMinutes(countMinutes(timeStart, shiftEnd), shiftMinutes);
        long dayNMinutes = clampMinutes(countMinutes(shiftStart, timeEnd), shiftMinutes);
        long middleDays = Math.max(0, dayCount - 1 - countHolidays);

        return day1Minutes + dayNMinutes + middleDays * shiftMinutes;
    }

    private LocalTime clampToShift(LocalTime time, LocalTime shiftStart, LocalTime shiftEnd) {
        if (time.isBefore(shiftStart)) return shiftStart;
        if (time.isAfter(shiftEnd)) return shiftEnd;
        return time;
    }

    private long clampMinutes(long minutes, long max) {
        return Math.max(0, Math.min(minutes, max));
    }

    private long countDays(LocalDate start, LocalDate end) {
        return ChronoUnit.DAYS.between(start, end);
    }

    private long countMinutes(LocalTime start, LocalTime end) {
        return ChronoUnit.MINUTES.between(start, end);
    }

}
