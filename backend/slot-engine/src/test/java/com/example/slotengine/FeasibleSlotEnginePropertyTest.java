package com.example.slotengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
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
 * Property-based suite for the feasible-slot engine.
 *
 * <p>Each test generates several hundred randomised scenarios and asserts a
 * property after every one. The seed is fixed, so a failure is reproducible and
 * the suite never flakes.
 *
 * <p>The strongest property here is
 * {@link #matchesBruteForceOracle()}: the engine's answer is compared against an
 * independent brute-force implementation that uses plain interval overlap and no
 * prefix sums. That cross-checks the whole optimisation, including the
 * "no earlier feasible slot exists" requirement, rather than merely restating
 * the engine's own logic.
 *
 * <p>Generated bookings and durations are aligned to the slot granularity so the
 * oracle can use ordinary time-interval arithmetic and remain obviously correct.
 * Unaligned inputs are covered by targeted examples in
 * {@link FeasibleSlotEngineTest}.
 */
class FeasibleSlotEnginePropertyTest {

    private static final int SCENARIOS = 300;
    private static final long SEED = 20260907L;

    private static final String SKILL = "OIL_SERVICE";
    private static final String OTHER_SKILL = "BRAKE_SERVICE";
    private static final String CAPABILITY = "GENERAL_SERVICE";
    private static final String OTHER_CAPABILITY = "ENGINE_REPAIR";
    private static final LocalDate ORIGIN = LocalDate.of(2026, 1, 5);

    /** One randomly generated search, kept together for reporting on failure. */
    private record Scenario(SlotSearchRequest request, String description) {
    }

    private static Scenario randomScenario(Random random) {
        int bayCount = random.nextInt(4);
        int technicianCount = random.nextInt(4);

        List<BayCandidate> bays = new ArrayList<>();
        for (int i = 1; i <= bayCount; i++) {
            boolean active = random.nextInt(10) > 0;
            Set<String> capabilities = random.nextInt(5) == 0
                    ? Set.of(OTHER_CAPABILITY)
                    : Set.of(CAPABILITY, OTHER_CAPABILITY);
            bays.add(new BayCandidate(i, active, capabilities));
        }

        List<TechnicianCandidate> technicians = new ArrayList<>();
        for (int i = 1; i <= technicianCount; i++) {
            boolean active = random.nextInt(10) > 0;
            List<SkillCertification> certifications = new ArrayList<>();
            if (random.nextInt(5) == 0) {
                certifications.add(new SkillCertification(
                        OTHER_SKILL, ORIGIN.minusYears(1), null));
            } else {
                // Sometimes bounded, so certification expiry inside the horizon
                // is exercised as well as open-ended validity.
                LocalDate from = ORIGIN.plusDays(random.nextInt(5) - 2);
                LocalDate to = random.nextInt(3) == 0
                        ? from.plusDays(random.nextInt(6))
                        : null;
                certifications.add(new SkillCertification(SKILL, from, to));
            }
            technicians.add(new TechnicianCandidate(10L * i, active, certifications));
        }

        Map<DayOfWeek, WorkingDayHours> weekly = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            if (day == DayOfWeek.SUNDAY) {
                continue; // always shut, so closed weekdays are always exercised
            }
            if (random.nextInt(6) == 0) {
                continue; // occasionally shut on another weekday too
            }
            int openHour = 7 + random.nextInt(3);
            int lengthHours = 4 + random.nextInt(6);
            weekly.put(day, new WorkingDayHours(
                    LocalTime.of(openHour, 0),
                    LocalTime.of(Math.min(openHour + lengthHours, 23), 0)));
        }

        int horizonDays = random.nextInt(9);
        Set<LocalDate> holidays = new HashSet<>();
        for (int i = 0; i < horizonDays; i++) {
            if (random.nextInt(6) == 0) {
                holidays.add(ORIGIN.plusDays(i));
            }
        }
        WorkingCalendar calendar = new WorkingCalendar(weekly, holidays);
        SearchHorizon horizon = new SearchHorizon(ORIGIN, horizonDays);

        // Durations are multiples of the granularity so the oracle stays simple.
        int durationMinutes = FeasibleSlotEngine.SLOT_GRANULARITY_MINUTES
                * (1 + random.nextInt(8));

        List<ExistingBooking> bookings = new ArrayList<>();
        int bookingCount = random.nextInt(10);
        for (int i = 0; i < bookingCount && horizonDays > 0; i++) {
            LocalDate date = ORIGIN.plusDays(random.nextInt(horizonDays));
            int startUnit = random.nextInt(40);
            int lengthUnits = 1 + random.nextInt(8);
            LocalTime start = LocalTime.of(0, 0)
                    .plusMinutes((long) (startUnit + 24) * FeasibleSlotEngine.SLOT_GRANULARITY_MINUTES);
            LocalTime end = start.plusMinutes(
                    (long) lengthUnits * FeasibleSlotEngine.SLOT_GRANULARITY_MINUTES);
            if (!end.isAfter(start)) {
                continue;
            }

            Long bayId = bayCount == 0 || random.nextInt(4) == 0
                    ? null
                    : (long) (1 + random.nextInt(bayCount));
            Long technicianId = technicianCount == 0 || random.nextInt(4) == 0
                    ? null
                    : 10L * (1 + random.nextInt(technicianCount));
            if (bayId == null && technicianId == null) {
                continue;
            }
            bookings.add(new ExistingBooking(date, start, end, bayId, technicianId));
        }

        int maxResults = 1 + random.nextInt(25);

        SlotSearchRequest request = new SlotSearchRequest(
                durationMinutes, SKILL, CAPABILITY,
                bays, technicians, calendar, bookings, horizon, maxResults);

        String description = "bays=" + bayCount + " technicians=" + technicianCount
                + " horizonDays=" + horizonDays + " duration=" + durationMinutes
                + " bookings=" + bookings.size() + " maxResults=" + maxResults;

        return new Scenario(request, description);
    }

    /**
     * An independent, deliberately naive reference implementation.
     *
     * <p>No prefix sums and no unit arithmetic beyond generating candidate
     * starts: availability is decided by plain half-open interval overlap. It is
     * written to be obviously correct rather than fast.
     */
    private static List<FeasibleSlot> bruteForce(SlotSearchRequest request) {
        List<FeasibleSlot> found = new ArrayList<>();

        List<BayCandidate> bays = request.candidateBays().stream()
                .filter(BayCandidate::active)
                .filter(b -> b.capabilities().contains(request.requiredCapability()))
                .sorted((l, r) -> Long.compare(l.bayId(), r.bayId()))
                .toList();

        List<TechnicianCandidate> technicians = request.candidateTechnicians().stream()
                .filter(TechnicianCandidate::active)
                .sorted((l, r) -> Long.compare(l.technicianId(), r.technicianId()))
                .toList();

        for (int offset = 0; offset < request.searchHorizon().numberOfDays(); offset++) {
            LocalDate date = request.searchHorizon().startDate().plusDays(offset);

            var maybeHours = request.workingCalendar().hoursOn(date);
            if (maybeHours.isEmpty()) {
                continue;
            }
            WorkingDayHours hours = maybeHours.get();

            int units = hours.minutes() / FeasibleSlotEngine.SLOT_GRANULARITY_MINUTES;
            for (int startUnit = 0; startUnit < units; startUnit++) {
                LocalTime start = hours.open().plusMinutes(
                        (long) startUnit * FeasibleSlotEngine.SLOT_GRANULARITY_MINUTES);
                LocalTime end = start.plusMinutes(request.durationMinutes());

                if (end.isAfter(hours.close())) {
                    continue; // must fit inside this one working day
                }

                for (BayCandidate bay : bays) {
                    if (occupied(request, date, start, end, bay.bayId(), null)) {
                        continue;
                    }
                    for (TechnicianCandidate technician : technicians) {
                        boolean certified = technician.certifications().stream()
                                .anyMatch(c -> c.skillCode().equals(request.requiredSkill())
                                        && !date.isBefore(c.validFrom())
                                        && (c.validTo() == null || !date.isAfter(c.validTo())));
                        if (!certified) {
                            continue;
                        }
                        if (occupied(request, date, start, end, null, technician.technicianId())) {
                            continue;
                        }
                        found.add(new FeasibleSlot(
                                date, start, end, bay.bayId(), technician.technicianId()));
                    }
                }
            }
        }
        return found;
    }

    /** Plain half-open overlap against every booking for that resource. */
    private static boolean occupied(
            SlotSearchRequest request,
            LocalDate date,
            LocalTime start,
            LocalTime end,
            Long bayId,
            Long technicianId) {

        for (ExistingBooking booking : request.existingBookings()) {
            if (!booking.date().equals(date)) {
                continue;
            }
            boolean sameResource =
                    (bayId != null && bayId.equals(booking.bayId()))
                            || (technicianId != null && technicianId.equals(booking.technicianId()));
            if (!sameResource) {
                continue;
            }
            if (start.isBefore(booking.end()) && booking.start().isBefore(end)) {
                return true;
            }
        }
        return false;
    }

    @Test
    @DisplayName("The engine agrees exactly with an independent brute-force oracle")
    void matchesBruteForceOracle() {
        Random random = new Random(SEED);
        int scenariosWithResults = 0;
        int totalSlots = 0;

        for (int i = 0; i < SCENARIOS; i++) {
            Scenario scenario = randomScenario(random);

            List<FeasibleSlot> actual = FeasibleSlotEngine.findSlots(scenario.request());
            List<FeasibleSlot> expected = bruteForce(scenario.request()).stream()
                    .limit(scenario.request().maxResults())
                    .toList();

            assertEquals(expected, actual,
                    "scenario " + i + " (" + scenario.description() + ")");

            if (!actual.isEmpty()) {
                scenariosWithResults++;
                totalSlots += actual.size();
            }
        }

        // Guards against the generator drifting into producing only degenerate
        // scenarios, which would let every assertion above pass vacuously.
        assertTrue(scenariosWithResults > SCENARIOS / 4,
                "only " + scenariosWithResults + " of " + SCENARIOS
                        + " scenarios produced any slot; the generator is too degenerate to prove much");
        assertTrue(totalSlots > SCENARIOS,
                "only " + totalSlots + " slots generated across all scenarios");
    }

    @Test
    @DisplayName("No returned slot overlaps an existing booking for its bay or technician")
    void neverOverlapsAnExistingBooking() {
        Random random = new Random(SEED + 1);

        for (int i = 0; i < SCENARIOS; i++) {
            Scenario scenario = randomScenario(random);

            for (FeasibleSlot slot : FeasibleSlotEngine.findSlots(scenario.request())) {
                for (ExistingBooking booking : scenario.request().existingBookings()) {
                    if (!booking.date().equals(slot.date())) {
                        continue;
                    }
                    boolean sharesResource =
                            Long.valueOf(slot.bayId()).equals(booking.bayId())
                                    || Long.valueOf(slot.technicianId()).equals(booking.technicianId());
                    if (!sharesResource) {
                        continue;
                    }
                    boolean overlaps = slot.start().isBefore(booking.end())
                            && booking.start().isBefore(slot.end());
                    assertTrue(!overlaps,
                            "scenario " + i + ": slot " + slot + " collides with " + booking);
                }
            }
        }
    }

    @Test
    @DisplayName("Every slot lies inside its own working day and never on a holiday")
    void alwaysInsideWorkingHours() {
        Random random = new Random(SEED + 2);

        for (int i = 0; i < SCENARIOS; i++) {
            Scenario scenario = randomScenario(random);

            for (FeasibleSlot slot : FeasibleSlotEngine.findSlots(scenario.request())) {
                var hours = scenario.request().workingCalendar().hoursOn(slot.date());
                assertTrue(hours.isPresent(),
                        "scenario " + i + ": slot on a closed day or holiday: " + slot);
                assertTrue(!slot.start().isBefore(hours.get().open()),
                        "scenario " + i + ": slot starts before opening: " + slot);
                assertTrue(!slot.end().isAfter(hours.get().close()),
                        "scenario " + i + ": slot ends after closing: " + slot);
                assertTrue(scenario.request().searchHorizon().contains(slot.date()),
                        "scenario " + i + ": slot outside the horizon: " + slot);
            }
        }
    }

    @Test
    @DisplayName("Every returned resource is active, capable, and certified for that date")
    void resourcesAlwaysSatisfyRequirements() {
        Random random = new Random(SEED + 3);

        for (int i = 0; i < SCENARIOS; i++) {
            Scenario scenario = randomScenario(random);

            for (FeasibleSlot slot : FeasibleSlotEngine.findSlots(scenario.request())) {
                BayCandidate bay = scenario.request().candidateBays().stream()
                        .filter(b -> b.bayId() == slot.bayId())
                        .findFirst().orElseThrow();
                assertTrue(bay.active(), "scenario " + i + ": inactive bay returned");
                assertTrue(bay.capabilities().contains(CAPABILITY),
                        "scenario " + i + ": bay lacks the required capability");

                TechnicianCandidate technician = scenario.request().candidateTechnicians().stream()
                        .filter(t -> t.technicianId() == slot.technicianId())
                        .findFirst().orElseThrow();
                assertTrue(technician.active(), "scenario " + i + ": inactive technician returned");
                assertTrue(technician.isCertifiedFor(SKILL, slot.date()),
                        "scenario " + i + ": technician not certified on " + slot.date());
            }
        }
    }

    @Test
    @DisplayName("Results are ordered by day, start, bay, then technician")
    void resultsAlwaysOrdered() {
        Random random = new Random(SEED + 4);

        for (int i = 0; i < SCENARIOS; i++) {
            Scenario scenario = randomScenario(random);
            List<FeasibleSlot> slots = FeasibleSlotEngine.findSlots(scenario.request());

            for (int s = 1; s < slots.size(); s++) {
                FeasibleSlot previous = slots.get(s - 1);
                FeasibleSlot current = slots.get(s);

                int comparison = previous.date().compareTo(current.date());
                if (comparison == 0) {
                    comparison = previous.start().compareTo(current.start());
                }
                if (comparison == 0) {
                    comparison = Long.compare(previous.bayId(), current.bayId());
                }
                if (comparison == 0) {
                    comparison = Long.compare(previous.technicianId(), current.technicianId());
                }
                assertTrue(comparison < 0,
                        "scenario " + i + ": out of order at " + s
                                + ": " + previous + " then " + current);
            }
        }
    }

    @Test
    @DisplayName("The result count never exceeds maxResults")
    void neverExceedsMaxResults() {
        Random random = new Random(SEED + 5);

        for (int i = 0; i < SCENARIOS; i++) {
            Scenario scenario = randomScenario(random);
            List<FeasibleSlot> slots = FeasibleSlotEngine.findSlots(scenario.request());

            assertTrue(slots.size() <= scenario.request().maxResults(),
                    "scenario " + i + ": returned " + slots.size()
                            + " for maxResults " + scenario.request().maxResults());
        }
    }

    @Test
    @DisplayName("No feasible slot exists earlier than the first returned slot")
    void noEarlierFeasibleSlotExists() {
        Random random = new Random(SEED + 6);

        for (int i = 0; i < SCENARIOS; i++) {
            Scenario scenario = randomScenario(random);

            List<FeasibleSlot> slots = FeasibleSlotEngine.findSlots(scenario.request());
            List<FeasibleSlot> all = bruteForce(scenario.request());

            if (slots.isEmpty()) {
                assertTrue(all.isEmpty(),
                        "scenario " + i + " (" + scenario.description()
                                + "): engine found nothing but " + all.size() + " slots exist");
                continue;
            }

            FeasibleSlot first = slots.get(0);
            for (FeasibleSlot candidate : all) {
                boolean earlier = candidate.date().isBefore(first.date())
                        || (candidate.date().equals(first.date())
                        && candidate.start().isBefore(first.start()));
                assertTrue(!earlier,
                        "scenario " + i + ": " + candidate + " is earlier than " + first);
            }
        }
    }

    @Test
    @DisplayName("Identical inputs always produce identical output")
    void alwaysDeterministic() {
        Random random = new Random(SEED + 7);

        for (int i = 0; i < SCENARIOS; i++) {
            Scenario scenario = randomScenario(random);

            assertEquals(
                    FeasibleSlotEngine.findSlots(scenario.request()),
                    FeasibleSlotEngine.findSlots(scenario.request()),
                    "scenario " + i + ": engine is not deterministic");
        }
    }

    @Test
    @DisplayName("Raising maxResults only extends the earlier answer, never reorders it")
    void largerMaxResultsExtendsThePrefix() {
        Random random = new Random(SEED + 8);

        for (int i = 0; i < SCENARIOS; i++) {
            Scenario scenario = randomScenario(random);
            SlotSearchRequest base = scenario.request();

            SlotSearchRequest wider = new SlotSearchRequest(
                    base.durationMinutes(), base.requiredSkill(), base.requiredCapability(),
                    base.candidateBays(), base.candidateTechnicians(), base.workingCalendar(),
                    base.existingBookings(), base.searchHorizon(), base.maxResults() + 10);

            List<FeasibleSlot> narrow = FeasibleSlotEngine.findSlots(base);
            List<FeasibleSlot> broad = FeasibleSlotEngine.findSlots(wider);

            assertTrue(broad.size() >= narrow.size(),
                    "scenario " + i + ": a wider search returned fewer slots");
            assertEquals(narrow, broad.subList(0, narrow.size()),
                    "scenario " + i + ": the wider search reordered the earlier slots");
        }
    }
}
