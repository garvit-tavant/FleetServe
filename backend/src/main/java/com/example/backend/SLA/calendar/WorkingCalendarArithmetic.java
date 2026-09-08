package com.example.backend.SLA.calendar;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * The single calendar-arithmetic component.
 *
 * <p>Deliberately pure: every input is passed in, and there are no repository
 * calls, no clock reads and no I/O. Both the service-level clocks (US-4.1) and
 * mean time to repair (US-4.2) must measure through this class rather than
 * keeping a second copy of the logic, otherwise the two dashboard figures
 * drift apart.
 *
 * <p>Supports INV-7: elapsed service-level time never counts hours outside the
 * workshop working calendar, and never counts a paused interval.
 */
public final class WorkingCalendarArithmetic {

    private WorkingCalendarArithmetic() {
    }

    /** A half-open instant range. */
    public record Interval(Instant start, Instant end) {
    }

    /**
     * Minutes between two instants that fall inside the workshop's open hours.
     * Closed days, hours outside the shift, and holidays contribute nothing.
     */
    public static long workingMinutesBetween(Instant start, Instant end, WorkshopCalendar calendar) {
        if (start == null || end == null || !end.isAfter(start)) {
            return 0L;
        }

        ZonedDateTime from = start.atZone(calendar.zone());
        ZonedDateTime to = end.atZone(calendar.zone());

        long total = 0L;
        LocalDate date = from.toLocalDate();
        LocalDate lastDate = to.toLocalDate();

        while (!date.isAfter(lastDate)) {
            final LocalDate currentDate = date;
            total += calendar.shiftOn(currentDate)
                    .map(shift -> overlapMinutes(from, to, currentDate, shift, calendar))
                    .orElse(0L);
            date = date.plusDays(1);
        }
        return total;
    }

    private static long overlapMinutes(
            ZonedDateTime from,
            ZonedDateTime to,
            LocalDate date,
            ShiftWindow shift,
            WorkshopCalendar calendar) {

        ZonedDateTime shiftOpen = ZonedDateTime.of(date, shift.open(), calendar.zone());
        ZonedDateTime shiftClose = ZonedDateTime.of(date, shift.close(), calendar.zone());

        ZonedDateTime windowStart = from.isAfter(shiftOpen) ? from : shiftOpen;
        ZonedDateTime windowEnd = to.isBefore(shiftClose) ? to : shiftClose;

        if (!windowEnd.isAfter(windowStart)) {
            return 0L;
        }
        return Duration.between(windowStart, windowEnd).toMinutes();
    }

    /** Plain elapsed minutes, ignoring any calendar. */
    public static long rawMinutesBetween(Instant start, Instant end) {
        if (start == null || end == null || !end.isAfter(start)) {
            return 0L;
        }
        return Duration.between(start, end).toMinutes();
    }

    /**
     * Clips pauses to the evaluation window and merges any that overlap.
     *
     * <p>Merging matters: a breakdown can be simultaneously AWAITING_PARTS and
     * DEPOT_UNREACHABLE, and subtracting both rows independently would remove
     * the same wall-clock interval twice and manufacture a false pass.
     *
     * <p>A pause with a null end is still open and is clipped to
     * {@code windowEnd}.
     */
    public static List<Interval> clipAndMergePauses(
            Collection<PauseInterval> pauses,
            Instant windowStart,
            Instant windowEnd) {

        if (pauses == null || pauses.isEmpty() || windowStart == null || windowEnd == null) {
            return List.of();
        }

        List<Interval> clipped = new ArrayList<>();
        for (PauseInterval pause : pauses) {
            if (pause == null || pause.start() == null) {
                continue;
            }
            Instant start = pause.start().isBefore(windowStart) ? windowStart : pause.start();
            Instant rawEnd = pause.end() == null ? windowEnd : pause.end();
            Instant end = rawEnd.isAfter(windowEnd) ? windowEnd : rawEnd;

            if (end.isAfter(start)) {
                clipped.add(new Interval(start, end));
            }
        }

        if (clipped.isEmpty()) {
            return List.of();
        }

        clipped.sort(Comparator.comparing(Interval::start));

        List<Interval> merged = new ArrayList<>();
        Instant currentStart = clipped.get(0).start();
        Instant currentEnd = clipped.get(0).end();

        for (int i = 1; i < clipped.size(); i++) {
            Interval next = clipped.get(i);
            if (!next.start().isAfter(currentEnd)) {
                if (next.end().isAfter(currentEnd)) {
                    currentEnd = next.end();
                }
            } else {
                merged.add(new Interval(currentStart, currentEnd));
                currentStart = next.start();
                currentEnd = next.end();
            }
        }
        merged.add(new Interval(currentStart, currentEnd));
        return merged;
    }

    /** Working minutes lost to pauses inside the evaluation window. */
    public static long pausedWorkingMinutes(
            Collection<PauseInterval> pauses,
            Instant windowStart,
            Instant windowEnd,
            WorkshopCalendar calendar) {

        long total = 0L;
        for (Interval interval : clipAndMergePauses(pauses, windowStart, windowEnd)) {
            total += workingMinutesBetween(interval.start(), interval.end(), calendar);
        }
        return total;
    }

    /** Plain elapsed minutes lost to pauses inside the evaluation window. */
    public static long pausedRawMinutes(
            Collection<PauseInterval> pauses,
            Instant windowStart,
            Instant windowEnd) {

        long total = 0L;
        for (Interval interval : clipAndMergePauses(pauses, windowStart, windowEnd)) {
            total += rawMinutesBetween(interval.start(), interval.end());
        }
        return total;
    }
}
