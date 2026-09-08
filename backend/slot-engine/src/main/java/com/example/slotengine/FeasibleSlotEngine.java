package com.example.slotengine;

import com.example.slotengine.model.BayCandidate;
import com.example.slotengine.model.ExistingBooking;
import com.example.slotengine.model.FeasibleSlot;
import com.example.slotengine.model.SearchHorizon;
import com.example.slotengine.model.SlotSearchRequest;
import com.example.slotengine.model.TechnicianCandidate;
import com.example.slotengine.model.WorkingCalendar;
import com.example.slotengine.model.WorkingDayHours;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The feasible-slot engine: the algorithmic core described in
 * docs/algorithmic-core.md.
 *
 * <p>A pure, deterministic function. Every input is supplied by the caller, and
 * there is no repository, no clock and no I/O, so identical inputs always give
 * identical outputs. The module deliberately has no runtime dependencies, so
 * this purity is enforced by the build rather than by convention.
 *
 * <p>Complexity, with B eligible bays, T eligible technicians, E existing
 * bookings, D working days in the horizon and U units per working day:
 * time {@code O(D * (B + T) * U + E)}, space {@code O(D * (B + T) * U)}.
 */
public final class FeasibleSlotEngine {

    /**
     * Candidate start times land on 15-minute boundaries: fine enough for real
     * appointment times, coarse enough to keep a working day at a few dozen
     * units.
     */
    public static final int SLOT_GRANULARITY_MINUTES = 15;

    private FeasibleSlotEngine() {
    }

    public static List<FeasibleSlot> findSlots(SlotSearchRequest request) {

        java.util.Objects.requireNonNull(
            request,
            "request"
        );
        
        List<FeasibleSlot> results = new ArrayList<>();
        if (request.maxResults() == 0) {
            return results;
        }

        // ---- Step 1: eligible resources, sorted so ordering is deterministic ----

        List<BayCandidate> bays = request.candidateBays().stream()
                .filter(BayCandidate::active)
                .filter(bay -> bay.hasCapability(request.requiredCapability()))
                .sorted(Comparator.comparingLong(BayCandidate::bayId))
                .toList();
        if (bays.isEmpty()) {
            return results;
        }

        // Technicians are filtered on holding the skill at all here; whether the
        // certification actually covers the day being searched is checked later,
        // because a certification can expire partway through the horizon.
        List<TechnicianCandidate> technicians = request.candidateTechnicians().stream()
                .filter(TechnicianCandidate::active)
                .filter(tech -> tech.holdsSkill(request.requiredSkill()))
                .sorted(Comparator.comparingLong(TechnicianCandidate::technicianId))
                .toList();
        if (technicians.isEmpty()) {
            return results;
        }

        // ---- Step 2: working days, holidays and closed weekdays removed ----

        List<LocalDate> workingDays = workingDaysIn(
                request.searchHorizon(), request.workingCalendar());
        if (workingDays.isEmpty()) {
            return results;
        }

        int dayCount = workingDays.size();
        int bayCount = bays.size();
        int technicianCount = technicians.size();

        Map<LocalDate, Integer> dayIndex = new HashMap<>();
        for (int d = 0; d < dayCount; d++) {
            dayIndex.put(workingDays.get(d), d);
        }
        Map<Long, Integer> bayIndex = new HashMap<>();
        for (int b = 0; b < bayCount; b++) {
            bayIndex.put(bays.get(b).bayId(), b);
        }
        Map<Long, Integer> technicianIndex = new HashMap<>();
        for (int t = 0; t < technicianCount; t++) {
            technicianIndex.put(technicians.get(t).technicianId(), t);
        }

        // ---- Step 3: per-day availability arrays, 0 free and 1 busy ----

        int[] unitsPerDay = new int[dayCount];
        LocalTime[] openPerDay = new LocalTime[dayCount];
        int[][][] bayBusy = new int[dayCount][bayCount][];
        int[][][] technicianBusy = new int[dayCount][technicianCount][];

        for (int d = 0; d < dayCount; d++) {
            WorkingDayHours hours = request.workingCalendar()
                    .hoursOn(workingDays.get(d))
                    .orElseThrow();
            // Floor: a partial trailing unit cannot hold a whole slot anyway, and
            // discarding it guarantees every slot ends on or before closing time.
            int units = hours.minutes() / SLOT_GRANULARITY_MINUTES;
            unitsPerDay[d] = units;
            openPerDay[d] = hours.open();

            for (int b = 0; b < bayCount; b++) {
                bayBusy[d][b] = new int[units];
            }
            for (int t = 0; t < technicianCount; t++) {
                technicianBusy[d][t] = new int[units];
            }
        }

        // ---- Step 4: mark existing bookings busy ----

        for (ExistingBooking booking : request.existingBookings()) {
            Integer d = dayIndex.get(booking.date());
            if (d == null) {
                continue;
            }

            int units = unitsPerDay[d];
            LocalTime open = openPerDay[d];

            int startUnit = floorUnit(open, booking.start());
            int endUnit = ceilUnit(open, booking.end());

            // Clamp into the day. A booking may legitimately start before opening
            // or run past closing; only the overlapping part concerns us.
            startUnit = Math.max(0, startUnit);
            endUnit = Math.min(units, endUnit);
            if (endUnit <= startUnit) {
                continue;
            }

            if (booking.bayId() != null) {
                Integer b = bayIndex.get(booking.bayId());
                if (b != null) {
                    java.util.Arrays.fill(bayBusy[d][b], startUnit, endUnit, 1);
                }
            }
            if (booking.technicianId() != null) {
                Integer t = technicianIndex.get(booking.technicianId());
                if (t != null) {
                    java.util.Arrays.fill(technicianBusy[d][t], startUnit, endUnit, 1);
                }
            }
        }

        // ---- Step 5: prefix sums, so a whole-range free check is O(1) ----

        int[][][] bayPrefix = new int[dayCount][bayCount][];
        int[][][] technicianPrefix = new int[dayCount][technicianCount][];
        for (int d = 0; d < dayCount; d++) {
            for (int b = 0; b < bayCount; b++) {
                bayPrefix[d][b] = prefixSum(bayBusy[d][b]);
            }
            for (int t = 0; t < technicianCount; t++) {
                technicianPrefix[d][t] = prefixSum(technicianBusy[d][t]);
            }
        }

        // Certification validity depends only on the date, because a job may not
        // span two working days, so it is resolved once per day per technician.
        boolean[][] certified = new boolean[dayCount][technicianCount];
        for (int d = 0; d < dayCount; d++) {
            LocalDate date = workingDays.get(d);
            for (int t = 0; t < technicianCount; t++) {
                certified[d][t] = technicians.get(t)
                        .isCertifiedFor(request.requiredSkill(), date);
            }
        }

        // ---- Step 6: walk days, then start units, earliest first ----

        // Round up: a 20-minute job must hold two 15-minute units. Rounding down
        // would let a job overrun the next booking.
        int requiredUnits = ceilDiv(request.durationMinutes(), SLOT_GRANULARITY_MINUTES);

        for (int d = 0; d < dayCount; d++) {
            int units = unitsPerDay[d];
            int lastStartUnit = units - requiredUnits;
            if (lastStartUnit < 0) {
                continue; // the job cannot fit in this day at all
            }

            LocalDate date = workingDays.get(d);
            LocalTime open = openPerDay[d];

            for (int startUnit = 0; startUnit <= lastStartUnit; startUnit++) {
                int endUnit = startUnit + requiredUnits;

                // ---- Step 7: bays free for the whole duration ----
                List<BayCandidate> freeBays = null;
                for (int b = 0; b < bayCount; b++) {
                    int[] prefix = bayPrefix[d][b];
                    if (prefix[endUnit] - prefix[startUnit] == 0) {
                        if (freeBays == null) {
                            freeBays = new ArrayList<>();
                        }
                        freeBays.add(bays.get(b));
                    }
                }
                if (freeBays == null) {
                    continue;
                }

                // ---- Step 8: technicians certified and free for the whole duration ----
                List<TechnicianCandidate> freeTechnicians = null;
                for (int t = 0; t < technicianCount; t++) {
                    if (!certified[d][t]) {
                        continue;
                    }
                    int[] prefix = technicianPrefix[d][t];
                    if (prefix[endUnit] - prefix[startUnit] == 0) {
                        if (freeTechnicians == null) {
                            freeTechnicians = new ArrayList<>();
                        }
                        freeTechnicians.add(technicians.get(t));
                    }
                }
                if (freeTechnicians == null) {
                    continue;
                }

                // ---- Step 9: combine, bay ascending then technician ascending ----
                LocalTime slotStart = open.plusMinutes(
                        (long) startUnit * SLOT_GRANULARITY_MINUTES);
                LocalTime slotEnd = slotStart.plusMinutes(request.durationMinutes());

                for (BayCandidate bay : freeBays) {
                    for (TechnicianCandidate technician : freeTechnicians) {
                        results.add(new FeasibleSlot(
                                date, slotStart, slotEnd,
                                bay.bayId(), technician.technicianId()));

                        if (results.size() == request.maxResults()) {
                            return results;
                        }
                    }
                }
            }
        }

        return results;
    }

    private static List<LocalDate> workingDaysIn(SearchHorizon horizon, WorkingCalendar calendar) {
        List<LocalDate> days = new ArrayList<>();
        for (int offset = 0; offset < horizon.numberOfDays(); offset++) {
            LocalDate date = horizon.startDate().plusDays(offset);
            if (calendar.isWorkingDay(date)) {
                days.add(date);
            }
        }
        return days;
    }

    /** prefix[i] is the number of busy units before index i. */
    private static int[] prefixSum(int[] busy) {
        int[] prefix = new int[busy.length + 1];
        for (int i = 0; i < busy.length; i++) {
            prefix[i + 1] = prefix[i] + busy[i];
        }
        return prefix;
    }

    /** Any unit a booking touches at all counts as busy, so the start rounds down. */
    private static int floorUnit(LocalTime open, LocalTime time) {
        return Math.floorDiv(minutesBetween(open, time), SLOT_GRANULARITY_MINUTES);
    }

    /** ...and the end rounds up, for the same reason. */
    private static int ceilUnit(LocalTime open, LocalTime time) {
        return ceilDiv(minutesBetween(open, time), SLOT_GRANULARITY_MINUTES);
    }

    private static int minutesBetween(LocalTime from, LocalTime to) {
        return to.toSecondOfDay() / 60 - from.toSecondOfDay() / 60;
    }

    private static int ceilDiv(int value, int divisor) {
        return Math.floorDiv(value + divisor - 1, divisor);
    }
}
