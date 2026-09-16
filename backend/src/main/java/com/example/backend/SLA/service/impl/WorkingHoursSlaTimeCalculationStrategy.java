package com.example.backend.SLA.service.impl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;

import com.example.backend.CapacityAndSchedulingService.repository.HolidayRepository;
import com.example.backend.CapacityAndSchedulingService.repository.WorkingCalendarRepository;
import com.example.backend.SLA.dto.SlaBasis;
import com.example.backend.SLA.service.SlaTimeCalculationStrategy;

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
@Service 
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
    public SlaBasis supportedBasis() {
        return SlaBasis.WORKING_TIME;
    }

    @Override
    public long calculateElapsedTime(OffsetDateTime start, OffsetDateTime end, long workshopID) {
        LocalTime shiftStart = workingCalendarRepository.findopentimebywokrshopID(workshopID);
        LocalTime shiftEnd = workingCalendarRepository.findbyclosetimebyworkshopID(workshopID);

        // Only need a count of holidays strictly between start/end dates for
        // clamping — start/end dates are assumed to never be holidays.
        long holidayCount = holidayRepository
                .countHoldidayinbetween(workshopID, start.toLocalDate(), end.toLocalDate());

        return calculateElapsedTimeInternal(start, end, shiftStart, shiftEnd, holidayCount);
    }

    /**
     * Batch-friendly overload: caller pre-fetches shiftStart/shiftEnd/holidayCount
     * once (e.g. per distinct workshop, across many rows) instead of this method
     * hitting workingCalendarRepository/holidayRepository per call. Used by
     * SlaService.MeanTimeToRepair() to avoid an N+1 query pattern when computing
     * elapsed time for many completed work orders at once.
     */
    @Override
    public long calculateElapsedTime(OffsetDateTime start, OffsetDateTime end, long workshopID,
                                      LocalTime shiftStart, LocalTime shiftEnd, long holidayCount) {
        return calculateElapsedTimeInternal(start, end, shiftStart, shiftEnd, holidayCount);
    }

    /**
     * we are assuming start and end dates are never the holidays — simplifies
     * clamping since we never need to special-case "what if the FIRST or LAST
     * day of the repair window is itself a holiday". We only ever need the
     * COUNT of holidays strictly between the start/end dates, not which
     * specific dates they are.
     */
    private long calculateElapsedTimeInternal(OffsetDateTime start, OffsetDateTime end,
                                               LocalTime shiftStart, LocalTime shiftEnd,
                                               long holidayCount) {
        LocalDate dateStart = start.toLocalDate();
        LocalDate dateEnd = end.toLocalDate();

        LocalTime timeStart = start.toLocalTime();
        LocalTime timeEnd = end.toLocalTime();

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
        // Middle days: full shift minutes per day, minus holidays (start/end
        // dates themselves are assumed never to be holidays, so holidayCount
        // only reflects dates strictly between dateStart and dateEnd)
        long dayCount = countDays(dateStart, dateEnd);

        long day1Minutes = clampMinutes(countMinutes(timeStart, shiftEnd), shiftMinutes);
        long dayNMinutes = clampMinutes(countMinutes(shiftStart, timeEnd), shiftMinutes);
        long middleDays = Math.max(0, dayCount - 1 - holidayCount);

        return day1Minutes + dayNMinutes + middleDays * shiftMinutes;
    }

    private LocalTime clampToShift(LocalTime time, LocalTime shiftStart, LocalTime shiftEnd) {
        if (time.isBefore(shiftStart)) return shiftStart;
        if (time.isAfter(shiftEnd)) return shiftEnd;
        return time;
    }

    /*
        what if the end time is after shift ends so we need to clamp that
        or similarly what if the start time is before shift starts
     */
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
