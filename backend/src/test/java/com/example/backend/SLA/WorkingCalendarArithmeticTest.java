package com.example.backend.SLA;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.backend.SLA.calendar.PauseInterval;
import com.example.backend.SLA.calendar.ShiftWindow;
import com.example.backend.SLA.calendar.WorkingCalendarArithmetic;
import com.example.backend.SLA.calendar.WorkshopCalendar;

/**
 * INV-7: elapsed service-level time never counts hours outside the workshop
 * working calendar, and never counts a paused interval.
 *
 * <p>Workshop opens 08:00-17:00 Monday to Friday in Asia/Kolkata, so a full open
 * day is 540 minutes. 2026-01-02 is a Friday and 2026-01-05 the next Monday.
 */
class WorkingCalendarArithmeticTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private static WorkshopCalendar calendar(Set<LocalDate> holidays) {
        Map<DayOfWeek, ShiftWindow> shifts = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : List.of(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)) {
            shifts.put(day, new ShiftWindow(LocalTime.of(8, 0), LocalTime.of(17, 0)));
        }
        return new WorkshopCalendar(ZONE, shifts, holidays);
    }

    private static WorkshopCalendar calendar() {
        return calendar(Set.of());
    }

    private static Instant at(String isoLocal) {
        return OffsetDateTime.parse(isoLocal + "+05:30").toInstant();
    }

    private static long working(String from, String to, WorkshopCalendar calendar) {
        return WorkingCalendarArithmetic.workingMinutesBetween(at(from), at(to), calendar);
    }

    @Nested
    @DisplayName("Working calendar")
    class WorkingCalendarCases {

        @Test
        @DisplayName("Inside a single shift, elapsed time is the plain difference")
        void insideOneShift() {
            assertEquals(180, working("2026-01-05T09:00:00", "2026-01-05T12:00:00", calendar()));
        }

        @Test
        @DisplayName("Time before opening and after closing is not counted")
        void outsideShiftIsNotCounted() {
            assertEquals(540, working("2026-01-05T06:00:00", "2026-01-05T21:00:00", calendar()));
        }

        @Test
        @DisplayName("A weekend contributes nothing: Fri 16:00 to Mon 09:00 is 120 minutes")
        void weekendIsExcluded() {
            assertEquals(120, working("2026-01-02T16:00:00", "2026-01-05T09:00:00", calendar()));
        }

        @Test
        @DisplayName("The overnight closure is not counted")
        void overnightIsExcluded() {
            assertEquals(120, working("2026-01-05T16:00:00", "2026-01-06T09:00:00", calendar()));
        }

        @Test
        @DisplayName("A P2 raised 17:00 Friday with an 8 working hour target is not breached at 01:00 Saturday")
        void us41WorkedExample() {
            long consumed = working("2026-01-02T17:00:00", "2026-01-03T01:00:00", calendar());
            assertEquals(0, consumed, "the workshop was closed for the whole interval");
            assertEquals(480 - consumed, 480, "none of the 8 working hour target is used up");
        }
    }

    @Nested
    @DisplayName("Holidays")
    class HolidayCases {

        @Test
        @DisplayName("A holiday on a weekday contributes nothing")
        void holidayMidRange() {
            WorkshopCalendar withHoliday = calendar(Set.of(LocalDate.of(2026, 1, 6)));
            // Mon 16:00 -> Wed 09:00. Tuesday the 6th is a holiday.
            assertEquals(120, working("2026-01-05T16:00:00", "2026-01-07T09:00:00", withHoliday));
        }

        @Test
        @DisplayName("A holiday on the closing date of the range is also excluded")
        void holidayOnBoundary() {
            WorkshopCalendar withHoliday = calendar(Set.of(LocalDate.of(2026, 1, 5)));
            // Fri 16:00 -> Mon 09:00, but the Monday is a holiday, leaving only Friday.
            assertEquals(60, working("2026-01-02T16:00:00", "2026-01-05T09:00:00", withHoliday));
        }
    }

    @Nested
    @DisplayName("Pause intervals")
    class PauseCases {

        private long netMinutes(String from, String to, List<PauseInterval> pauses) {
            WorkshopCalendar cal = calendar();
            long open = working(from, to, cal);
            long paused = WorkingCalendarArithmetic.pausedWorkingMinutes(pauses, at(from), at(to), cal);
            return open - paused;
        }

        @Test
        @DisplayName("An AWAITING_PARTS pause is subtracted")
        void awaitingPartsIsSubtracted() {
            List<PauseInterval> pauses = List.of(new PauseInterval(
                    PauseInterval.AWAITING_PARTS,
                    at("2026-01-05T10:00:00"),
                    at("2026-01-05T12:00:00")));

            assertEquals(240, netMinutes("2026-01-05T09:00:00", "2026-01-05T15:00:00", pauses));
        }

        @Test
        @DisplayName("A DEPOT_UNREACHABLE pause is subtracted the same way")
        void depotUnreachableIsSubtracted() {
            List<PauseInterval> pauses = List.of(new PauseInterval(
                    PauseInterval.DEPOT_UNREACHABLE,
                    at("2026-01-05T10:00:00"),
                    at("2026-01-05T12:00:00")));

            assertEquals(240, netMinutes("2026-01-05T09:00:00", "2026-01-05T15:00:00", pauses));
        }

        @Test
        @DisplayName("Concurrent pauses of different reasons are not subtracted twice")
        void overlappingPausesAreMerged() {
            // 10:00-12:00 waiting for parts, 11:00-13:00 depot unreachable.
            // The union is 10:00-13:00, so 180 minutes, not 240.
            List<PauseInterval> pauses = List.of(
                    new PauseInterval(PauseInterval.AWAITING_PARTS,
                            at("2026-01-05T10:00:00"), at("2026-01-05T12:00:00")),
                    new PauseInterval(PauseInterval.DEPOT_UNREACHABLE,
                            at("2026-01-05T11:00:00"), at("2026-01-05T13:00:00")));

            assertEquals(180, netMinutes("2026-01-05T09:00:00", "2026-01-05T15:00:00", pauses));
        }

        @Test
        @DisplayName("A pause outside opening hours removes nothing, because none was counted")
        void pauseOutsideShiftRemovesNothing() {
            List<PauseInterval> pauses = List.of(new PauseInterval(
                    PauseInterval.DEPOT_UNREACHABLE,
                    at("2026-01-05T18:00:00"),
                    at("2026-01-05T20:00:00")));

            assertEquals(360, netMinutes("2026-01-05T09:00:00", "2026-01-05T15:00:00", pauses));
        }

        @Test
        @DisplayName("A still-open pause is clipped to the evaluation instant")
        void openPauseIsClipped() {
            List<PauseInterval> pauses = List.of(new PauseInterval(
                    PauseInterval.AWAITING_PARTS,
                    at("2026-01-05T13:00:00"),
                    null));

            assertEquals(240, netMinutes("2026-01-05T09:00:00", "2026-01-05T15:00:00", pauses));
        }

        @Test
        @DisplayName("A pause spanning a weekend only removes working minutes")
        void pauseAcrossWeekend() {
            List<PauseInterval> pauses = List.of(new PauseInterval(
                    PauseInterval.AWAITING_PARTS,
                    at("2026-01-02T16:30:00"),
                    at("2026-01-05T08:30:00")));

            // Open minutes Fri 16:00 -> Mon 09:00 are 120; the pause covers the
            // last 30 on Friday and the first 30 on Monday.
            assertEquals(60, netMinutes("2026-01-02T16:00:00", "2026-01-05T09:00:00", pauses));
        }
    }

    @Nested
    @DisplayName("Degenerate input")
    class DegenerateCases {

        @Test
        @DisplayName("An end before the start yields zero rather than a negative clock")
        void reversedRange() {
            assertEquals(0, working("2026-01-05T12:00:00", "2026-01-05T09:00:00", calendar()));
        }

        @Test
        @DisplayName("Null bounds yield zero")
        void nullBounds() {
            assertEquals(0, WorkingCalendarArithmetic.workingMinutesBetween(null, null, calendar()));
        }
    }
}
