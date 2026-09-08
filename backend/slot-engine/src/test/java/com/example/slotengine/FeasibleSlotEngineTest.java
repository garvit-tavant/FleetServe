package com.example.slotengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.slotengine.model.BayCandidate;
import com.example.slotengine.model.ExistingBooking;
import com.example.slotengine.model.FeasibleSlot;
import com.example.slotengine.model.SearchHorizon;
import com.example.slotengine.model.SkillCertification;
import com.example.slotengine.model.SlotSearchRequest;
import com.example.slotengine.model.TechnicianCandidate;
import com.example.slotengine.model.WorkingCalendar;
import com.example.slotengine.model.WorkingDayHours;

/**
 * Example-based suite for the feasible-slot engine, including every degenerate
 * case the specification calls out: no bays, no technicians, a fully booked
 * horizon, a duration longer than the working day, a holiday inside the
 * horizon, a zero-length horizon, and a booking that exactly abuts the
 * candidate slot.
 *
 * <p>Fixture: workshop open 08:00-17:00 Monday to Friday. 2026-01-05 is a
 * Monday, 2026-01-10 a Saturday.
 */
class FeasibleSlotEngineTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 1, 5);
    private static final String SKILL = "OIL_SERVICE";
    private static final String CAPABILITY = "GENERAL_SERVICE";

    private static WorkingCalendar weekdayCalendar(Set<LocalDate> holidays) {
        Map<DayOfWeek, WorkingDayHours> hours = new EnumMap<>(DayOfWeek.class);
        WorkingDayHours nineToFive =
                new WorkingDayHours(LocalTime.of(8, 0), LocalTime.of(17, 0));
        for (DayOfWeek day : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)) {
            hours.put(day, nineToFive);
        }
        return new WorkingCalendar(hours, holidays);
    }

    private static WorkingCalendar weekdayCalendar() {
        return weekdayCalendar(Set.of());
    }

    private static BayCandidate bay(long id) {
        return new BayCandidate(id, true, Set.of(CAPABILITY));
    }

    private static TechnicianCandidate technician(long id) {
        return new TechnicianCandidate(id, true, List.of(
                new SkillCertification(SKILL, LocalDate.of(2020, 1, 1), null)));
    }

    private static SlotSearchRequest request(
            int durationMinutes,
            List<BayCandidate> bays,
            List<TechnicianCandidate> technicians,
            WorkingCalendar calendar,
            List<ExistingBooking> bookings,
            SearchHorizon horizon,
            int maxResults) {
        return new SlotSearchRequest(
                durationMinutes, SKILL, CAPABILITY,
                bays, technicians, calendar, bookings, horizon, maxResults);
    }

    /** One bay, one technician, one open week, nothing booked. */
    private static SlotSearchRequest simple(int durationMinutes, int maxResults) {
        return request(durationMinutes, List.of(bay(1)), List.of(technician(10)),
                weekdayCalendar(), List.of(), new SearchHorizon(MONDAY, 5), maxResults);
    }

    @Nested
    @DisplayName("Basic search")
    class BasicSearch {

        @Test
        @DisplayName("The earliest slot starts at opening time")
        void earliestSlotIsAtOpening() {
            List<FeasibleSlot> slots = FeasibleSlotEngine.findSlots(simple(60, 1));

            assertEquals(1, slots.size());
            FeasibleSlot first = slots.get(0);
            assertEquals(MONDAY, first.date());
            assertEquals(LocalTime.of(8, 0), first.start());
            assertEquals(LocalTime.of(9, 0), first.end());
            assertEquals(1, first.bayId());
            assertEquals(10, first.technicianId());
        }

        @Test
        @DisplayName("Candidate starts advance in 15-minute steps")
        void startsAdvanceByGranularity() {
            List<FeasibleSlot> slots = FeasibleSlotEngine.findSlots(simple(60, 3));

            assertEquals(List.of(LocalTime.of(8, 0), LocalTime.of(8, 15), LocalTime.of(8, 30)),
                    slots.stream().map(FeasibleSlot::start).toList());
        }

        @Test
        @DisplayName("Never more results than requested")
        void resultCountIsCapped() {
            assertEquals(7, FeasibleSlotEngine.findSlots(simple(60, 7)).size());
        }

        @Test
        @DisplayName("Every slot ends on or before closing time")
        void slotsStayInsideWorkingHours() {
            List<FeasibleSlot> slots = FeasibleSlotEngine.findSlots(simple(60, 1000));

            assertFalse(slots.isEmpty());
            for (FeasibleSlot slot : slots) {
                assertTrue(!slot.start().isBefore(LocalTime.of(8, 0)));
                assertTrue(!slot.end().isAfter(LocalTime.of(17, 0)),
                        "slot " + slot.start() + "-" + slot.end() + " runs past closing");
            }
        }

        @Test
        @DisplayName("Results are ordered by day, then start, then bay, then technician")
        void deterministicOrdering() {
            SlotSearchRequest twoByTwo = request(60,
                    List.of(bay(2), bay(1)),
                    List.of(technician(20), technician(10)),
                    weekdayCalendar(), List.of(), new SearchHorizon(MONDAY, 5), 4);

            List<FeasibleSlot> slots = FeasibleSlotEngine.findSlots(twoByTwo);

            assertEquals(List.of(
                            "1/10", "1/20", "2/10", "2/20"),
                    slots.stream()
                            .map(s -> s.bayId() + "/" + s.technicianId())
                            .toList(),
                    "ties break by bay id then technician id");
            assertTrue(slots.stream().allMatch(s -> s.start().equals(LocalTime.of(8, 0))));
        }

        @Test
        @DisplayName("Identical inputs produce identical output")
        void deterministicAcrossRuns() {
            assertEquals(FeasibleSlotEngine.findSlots(simple(90, 50)),
                    FeasibleSlotEngine.findSlots(simple(90, 50)));
        }
    }

    @Nested
    @DisplayName("Existing bookings")
    class Bookings {

        @Test
        @DisplayName("A booking that exactly abuts the candidate slot does not block it")
        void abuttingBookingDoesNotOverlap() {
            // Half-open ranges: 08:00-09:00 and 09:00-10:00 can both stand.
            ExistingBooking earlier = new ExistingBooking(
                    MONDAY, LocalTime.of(8, 0), LocalTime.of(9, 0), 1L, 10L);

            List<FeasibleSlot> slots = FeasibleSlotEngine.findSlots(
                    request(60, List.of(bay(1)), List.of(technician(10)),
                            weekdayCalendar(), List.of(earlier),
                            new SearchHorizon(MONDAY, 1), 1));

            assertEquals(LocalTime.of(9, 0), slots.get(0).start(),
                    "the slot starting exactly when the booking ends is free");
        }

        @Test
        @DisplayName("A busy bay is skipped in favour of a free one at the same time")
        void busyBayIsSkipped() {
            ExistingBooking blocksBayOne = new ExistingBooking(
                    MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0), 1L, null);

            List<FeasibleSlot> slots = FeasibleSlotEngine.findSlots(
                    request(60, List.of(bay(1), bay(2)), List.of(technician(10)),
                            weekdayCalendar(), List.of(blocksBayOne),
                            new SearchHorizon(MONDAY, 1), 1));

            assertEquals(2, slots.get(0).bayId());
            assertEquals(LocalTime.of(8, 0), slots.get(0).start());
        }

        @Test
        @DisplayName("A booked technician blocks the slot even when a bay is free")
        void busyTechnicianBlocks() {
            ExistingBooking blocksTechnician = new ExistingBooking(
                    MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0), null, 10L);

            List<FeasibleSlot> slots = FeasibleSlotEngine.findSlots(
                    request(60, List.of(bay(1)), List.of(technician(10)),
                            weekdayCalendar(), List.of(blocksTechnician),
                            new SearchHorizon(MONDAY, 1), 1));

            assertEquals(LocalTime.of(12, 0), slots.get(0).start());
        }

        @Test
        @DisplayName("Availability is checked for the whole duration, not just the start")
        void wholeDurationIsChecked() {
            // 09:00-10:00 is busy. A two-hour job starting at 08:00 would be free
            // at its start instant but would run into the booking.
            ExistingBooking midMorning = new ExistingBooking(
                    MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), 1L, 10L);

            List<FeasibleSlot> slots = FeasibleSlotEngine.findSlots(
                    request(120, List.of(bay(1)), List.of(technician(10)),
                            weekdayCalendar(), List.of(midMorning),
                            new SearchHorizon(MONDAY, 1), 1));

            assertEquals(LocalTime.of(10, 0), slots.get(0).start(),
                    "08:00 is free at the start instant but collides at 09:00");
        }

        @Test
        @DisplayName("A fully booked horizon yields nothing")
        void fullyBookedHorizon() {
            List<ExistingBooking> allDay = List.of(
                    new ExistingBooking(MONDAY, LocalTime.of(8, 0), LocalTime.of(17, 0), 1L, 10L),
                    new ExistingBooking(MONDAY.plusDays(1), LocalTime.of(8, 0), LocalTime.of(17, 0), 1L, 10L));

            List<FeasibleSlot> slots = FeasibleSlotEngine.findSlots(
                    request(60, List.of(bay(1)), List.of(technician(10)),
                            weekdayCalendar(), allDay,
                            new SearchHorizon(MONDAY, 2), 10));

            assertTrue(slots.isEmpty());
        }

        @Test
        @DisplayName("Bookings outside the horizon are ignored")
        void bookingOutsideHorizonIgnored() {
            ExistingBooking nextWeek = new ExistingBooking(
                    MONDAY.plusDays(7), LocalTime.of(8, 0), LocalTime.of(17, 0), 1L, 10L);

            List<FeasibleSlot> slots = FeasibleSlotEngine.findSlots(
                    request(60, List.of(bay(1)), List.of(technician(10)),
                            weekdayCalendar(), List.of(nextWeek),
                            new SearchHorizon(MONDAY, 1), 1));

            assertEquals(LocalTime.of(8, 0), slots.get(0).start());
        }

        @Test
        @DisplayName("A booking overrunning closing time is clamped, not rejected")
        void bookingOutsideWorkingHoursIsClamped() {
            ExistingBooking overnight = new ExistingBooking(
                    MONDAY, LocalTime.of(6, 0), LocalTime.of(9, 0), 1L, 10L);

            List<FeasibleSlot> slots = FeasibleSlotEngine.findSlots(
                    request(60, List.of(bay(1)), List.of(technician(10)),
                            weekdayCalendar(), List.of(overnight),
                            new SearchHorizon(MONDAY, 1), 1));

            assertEquals(LocalTime.of(9, 0), slots.get(0).start());
        }
    }

    @Nested
    @DisplayName("Degenerate cases")
    class Degenerate {

        @Test
        @DisplayName("No bays at all")
        void noBays() {
            assertTrue(FeasibleSlotEngine.findSlots(
                    request(60, List.of(), List.of(technician(10)),
                            weekdayCalendar(), List.of(),
                            new SearchHorizon(MONDAY, 5), 10)).isEmpty());
        }

        @Test
        @DisplayName("No technicians at all")
        void noTechnicians() {
            assertTrue(FeasibleSlotEngine.findSlots(
                    request(60, List.of(bay(1)), List.of(),
                            weekdayCalendar(), List.of(),
                            new SearchHorizon(MONDAY, 5), 10)).isEmpty());
        }

        @Test
        @DisplayName("No bay carries the required capability")
        void noBayWithCapability() {
            BayCandidate wrongCapability = new BayCandidate(1, true, Set.of("BRAKE_REPAIR"));

            assertTrue(FeasibleSlotEngine.findSlots(
                    request(60, List.of(wrongCapability), List.of(technician(10)),
                            weekdayCalendar(), List.of(),
                            new SearchHorizon(MONDAY, 5), 10)).isEmpty());
        }

        @Test
        @DisplayName("Inactive resources are ignored")
        void inactiveResourcesIgnored() {
            BayCandidate inactiveBay = new BayCandidate(1, false, Set.of(CAPABILITY));
            TechnicianCandidate inactiveTechnician = new TechnicianCandidate(10, false,
                    List.of(new SkillCertification(SKILL, LocalDate.of(2020, 1, 1), null)));

            assertTrue(FeasibleSlotEngine.findSlots(
                    request(60, List.of(inactiveBay), List.of(technician(10)),
                            weekdayCalendar(), List.of(),
                            new SearchHorizon(MONDAY, 5), 10)).isEmpty());

            assertTrue(FeasibleSlotEngine.findSlots(
                    request(60, List.of(bay(1)), List.of(inactiveTechnician),
                            weekdayCalendar(), List.of(),
                            new SearchHorizon(MONDAY, 5), 10)).isEmpty());
        }

        @Test
        @DisplayName("A duration longer than the working day never fits")
        void durationLongerThanWorkingDay() {
            // The day is nine hours; the job is ten.
            assertTrue(FeasibleSlotEngine.findSlots(simple(600, 10)).isEmpty(),
                    "a job may not span two working days");
        }

        @Test
        @DisplayName("A duration exactly filling the working day fits once per day")
        void durationExactlyFillsDay() {
            List<FeasibleSlot> slots = FeasibleSlotEngine.findSlots(simple(540, 10));

            assertEquals(5, slots.size(), "one slot on each of five working days");
            assertTrue(slots.stream().allMatch(s -> s.start().equals(LocalTime.of(8, 0))));
            assertTrue(slots.stream().allMatch(s -> s.end().equals(LocalTime.of(17, 0))));
        }

        @Test
        @DisplayName("A zero-length horizon yields nothing")
        void zeroLengthHorizon() {
            assertTrue(FeasibleSlotEngine.findSlots(
                    request(60, List.of(bay(1)), List.of(technician(10)),
                            weekdayCalendar(), List.of(),
                            new SearchHorizon(MONDAY, 0), 10)).isEmpty());
        }

        @Test
        @DisplayName("A horizon of only non-working days yields nothing")
        void horizonOfWeekendOnly() {
            LocalDate saturday = LocalDate.of(2026, 1, 10);

            assertTrue(FeasibleSlotEngine.findSlots(
                    request(60, List.of(bay(1)), List.of(technician(10)),
                            weekdayCalendar(), List.of(),
                            new SearchHorizon(saturday, 2), 10)).isEmpty());
        }

        @Test
        @DisplayName("A holiday inside the horizon is skipped")
        void holidayInsideHorizon() {
            LocalDate tuesday = MONDAY.plusDays(1);
            WorkingCalendar withHoliday = weekdayCalendar(Set.of(tuesday));

            List<FeasibleSlot> slots = FeasibleSlotEngine.findSlots(
                    request(540, List.of(bay(1)), List.of(technician(10)),
                            withHoliday, List.of(),
                            new SearchHorizon(MONDAY, 5), 10));

            assertEquals(4, slots.size(), "five weekdays less one holiday");
            assertFalse(slots.stream().anyMatch(s -> s.date().equals(tuesday)),
                    "no slot may fall on a holiday");
        }

        @Test
        @DisplayName("maxResults of zero returns nothing without searching")
        void zeroMaxResults() {
            assertTrue(FeasibleSlotEngine.findSlots(simple(60, 0)).isEmpty());
        }

        @Test
        @DisplayName("Fewer feasible slots than requested returns what exists")
        void fewerSlotsThanRequested() {
            List<FeasibleSlot> slots = FeasibleSlotEngine.findSlots(
                    request(540, List.of(bay(1)), List.of(technician(10)),
                            weekdayCalendar(), List.of(),
                            new SearchHorizon(MONDAY, 1), 100));

            assertEquals(1, slots.size(), "returns what exists rather than searching indefinitely");
        }

        @Test
        @DisplayName("A non-positive duration is rejected")
        void invalidDurationRejected() {
            assertThrows(IllegalArgumentException.class, () -> simple(0, 1));
            assertThrows(IllegalArgumentException.class, () -> simple(-30, 1));
        }

        @Test
        @DisplayName("A negative horizon length is rejected")
        void negativeHorizonRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> new SearchHorizon(MONDAY, -1));
        }
    }

    @Nested
    @DisplayName("Certification validity")
    class Certification {

        @Test
        @DisplayName("A technician whose certification has expired is not offered")
        void expiredCertificationExcluded() {
            TechnicianCandidate expired = new TechnicianCandidate(10, true, List.of(
                    new SkillCertification(SKILL,
                            LocalDate.of(2020, 1, 1), LocalDate.of(2025, 12, 31))));

            assertTrue(FeasibleSlotEngine.findSlots(
                    request(60, List.of(bay(1)), List.of(expired),
                            weekdayCalendar(), List.of(),
                            new SearchHorizon(MONDAY, 5), 10)).isEmpty());
        }

        @Test
        @DisplayName("A certification starting mid-horizon only applies from its start date")
        void certificationStartingMidHorizon() {
            LocalDate wednesday = MONDAY.plusDays(2);
            TechnicianCandidate startsWednesday = new TechnicianCandidate(10, true, List.of(
                    new SkillCertification(SKILL, wednesday, null)));

            List<FeasibleSlot> slots = FeasibleSlotEngine.findSlots(
                    request(540, List.of(bay(1)), List.of(startsWednesday),
                            weekdayCalendar(), List.of(),
                            new SearchHorizon(MONDAY, 5), 10));

            assertEquals(3, slots.size(), "Wednesday, Thursday and Friday only");
            assertEquals(wednesday, slots.get(0).date());
        }

        @Test
        @DisplayName("Validity is inclusive on both boundary dates")
        void validityBoundariesAreInclusive() {
            TechnicianCandidate exactlyMonday = new TechnicianCandidate(10, true, List.of(
                    new SkillCertification(SKILL, MONDAY, MONDAY)));

            List<FeasibleSlot> slots = FeasibleSlotEngine.findSlots(
                    request(540, List.of(bay(1)), List.of(exactlyMonday),
                            weekdayCalendar(), List.of(),
                            new SearchHorizon(MONDAY, 5), 10));

            assertEquals(1, slots.size());
            assertEquals(MONDAY, slots.get(0).date());
        }

        @Test
        @DisplayName("A technician holding a different skill is not offered")
        void wrongSkillExcluded() {
            TechnicianCandidate otherSkill = new TechnicianCandidate(10, true, List.of(
                    new SkillCertification("BRAKE_SERVICE", LocalDate.of(2020, 1, 1), null)));

            assertTrue(FeasibleSlotEngine.findSlots(
                    request(60, List.of(bay(1)), List.of(otherSkill),
                            weekdayCalendar(), List.of(),
                            new SearchHorizon(MONDAY, 5), 10)).isEmpty());
        }
    }

    @Nested
    @DisplayName("Duration not a multiple of the granularity")
    class RaggedDuration {

        @Test
        @DisplayName("A 20-minute job reserves two units, so the next start is 08:30")
        void raggedDurationRoundsUp() {
            List<FeasibleSlot> slots = FeasibleSlotEngine.findSlots(
                    request(20, List.of(bay(1)), List.of(technician(10)),
                            weekdayCalendar(), List.of(),
                            new SearchHorizon(MONDAY, 1), 2));

            assertEquals(LocalTime.of(8, 0), slots.get(0).start());
            assertEquals(LocalTime.of(8, 20), slots.get(0).end());
            // Candidate starts still advance one unit at a time.
            assertEquals(LocalTime.of(8, 15), slots.get(1).start());
        }

        @Test
        @DisplayName("A ragged job cannot be placed where only part of it fits")
        void raggedDurationDoesNotOverrun() {
            // Busy from 08:30. A 20-minute job at 08:15 would end at 08:35 and
            // collide, so the engine must not offer it.
            ExistingBooking fromHalfPast = new ExistingBooking(
                    MONDAY, LocalTime.of(8, 30), LocalTime.of(17, 0), 1L, 10L);

            List<FeasibleSlot> slots = FeasibleSlotEngine.findSlots(
                    request(20, List.of(bay(1)), List.of(technician(10)),
                            weekdayCalendar(), List.of(fromHalfPast),
                            new SearchHorizon(MONDAY, 1), 10));

            assertEquals(1, slots.size());
            assertEquals(LocalTime.of(8, 0), slots.get(0).start());
        }
    }
}
