package com.example.backend.SLA;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.backend.SLA.calendar.ShiftWindow;
import com.example.backend.SLA.calendar.WorkshopCalendar;
import com.example.backend.SLA.calendar.WorkshopCalendarProvider;
import com.example.backend.SLA.dto.SlaClockSnapshot;
import com.example.backend.SLA.dto.SlaClockStatus;
import com.example.backend.SLA.dto.SlaEvaluationRow;
import com.example.backend.SLA.entity.AwaitingRaised;
import com.example.backend.SLA.repository.AwaitingRaisedRepository;
import com.example.backend.SLA.repository.BreakdownRequestRepository;
import com.example.backend.SLA.repository.SlaCheckpointRepository;
import com.example.backend.SLA.service.ElapsedHoursSlaCalculator;
import com.example.backend.SLA.service.SlaCalculatorRegistry;
import com.example.backend.SLA.service.SlaClockService;
import com.example.backend.SLA.service.WorkingCalendarSlaCalculator;

/**
 * Breach evaluation against an injected, fixed clock.
 *
 * <p>Workshop opens 08:00-17:00 Monday to Friday, Asia/Kolkata. P2 carries a
 * 240 minute response target and a 480 minute resolution target.
 */
class SlaClockServiceTest {

    private static final long BREAKDOWN_ID = 100L;
    private static final long WORKSHOP_ID = 7L;
    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private BreakdownRequestRepository breakdownRequestRepository;
    private AwaitingRaisedRepository awaitingRaisedRepository;
    private SlaCheckpointRepository slaCheckpointRepository;
    private WorkshopCalendarProvider calendarProvider;
    private SlaCalculatorRegistry registry;

    @BeforeEach
    void setUp() {
        breakdownRequestRepository = mock(BreakdownRequestRepository.class);
        awaitingRaisedRepository = mock(AwaitingRaisedRepository.class);
        slaCheckpointRepository = mock(SlaCheckpointRepository.class);
        calendarProvider = mock(WorkshopCalendarProvider.class);
        registry = new SlaCalculatorRegistry(
                List.of(new WorkingCalendarSlaCalculator(), new ElapsedHoursSlaCalculator()));

        when(calendarProvider.load(anyLong(), any(), any())).thenReturn(workshopCalendar());
        when(calendarProvider.continuousCalendar())
                .thenReturn(new WorkshopCalendar(ZoneId.of("UTC"), Map.of(), Set.of()));
        when(awaitingRaisedRepository.findByBreakdownRequestId(anyLong())).thenReturn(List.of());
    }

    private static WorkshopCalendar workshopCalendar() {
        Map<DayOfWeek, ShiftWindow> shifts = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : List.of(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)) {
            shifts.put(day, new ShiftWindow(LocalTime.of(8, 0), LocalTime.of(17, 0)));
        }
        return new WorkshopCalendar(ZONE, shifts, Set.of());
    }

    private static OffsetDateTime at(String isoLocal) {
        return OffsetDateTime.parse(isoLocal + "+05:30");
    }

    private SlaClockService serviceAt(String nowIsoLocal) {
        Clock fixed = Clock.fixed(at(nowIsoLocal).toInstant(), ZONE);
        return new SlaClockService(
                fixed, registry, calendarProvider,
                breakdownRequestRepository, awaitingRaisedRepository, slaCheckpointRepository,
                0.2);
    }

    private void givenBreakdown(
            OffsetDateTime reportedAt,
            Long workshopId,
            OffsetDateTime respondedAt,
            OffsetDateTime resolvedAt) {

        when(breakdownRequestRepository.findForEvaluation(BREAKDOWN_ID))
                .thenReturn(Optional.of(new SlaEvaluationRow(
                        BREAKDOWN_ID, com.example.backend.SLA.status.BreakdownPriority.P2, reportedAt, workshopId,
                        240, 480, WorkingCalendarSlaCalculator.BASIS,
                        respondedAt, resolvedAt, false, false)));
    }

    private static AwaitingRaised pause(String reason, OffsetDateTime from, OffsetDateTime to) {
        AwaitingRaised row = new AwaitingRaised();
        row.setPauseReason(reason);
        row.setRaisedAt(from);
        row.setResolvedAt(to);
        return row;
    }

    @Test
    @DisplayName("US-4.1: a P2 raised 17:00 Friday is not breached at 01:00 Saturday")
    void notBreachedOverTheWeekend() {
        givenBreakdown(at("2026-01-02T17:00:00"), WORKSHOP_ID, null, null);

        SlaClockSnapshot snapshot = serviceAt("2026-01-03T01:00:00").evaluate(BREAKDOWN_ID);

        assertEquals(0, snapshot.responseElapsedMinutes(), "the workshop was shut all night");
        assertEquals(SlaClockStatus.OK, snapshot.responseStatus());
        assertEquals(SlaClockStatus.OK, snapshot.resolutionStatus());
    }

    @Test
    @DisplayName("A response recorded after the target is flagged as breached")
    void lateRecordedResponseIsBreach() {
        // Reported Monday 09:00, response target 240 minutes, so due by 13:00.
        // The response was only recorded at 16:00, which is 420 working minutes.
        givenBreakdown(
                at("2026-01-05T09:00:00"), WORKSHOP_ID,
                at("2026-01-05T16:00:00"), null);

        SlaClockSnapshot snapshot = serviceAt("2026-01-06T09:00:00").evaluate(BREAKDOWN_ID);

        assertEquals(420, snapshot.responseElapsedMinutes());
        assertEquals(SlaClockStatus.BREACHED, snapshot.responseStatus());
        assertTrue(snapshot.responseClockStopped(), "the clock stops at the recorded timestamp");
    }

    @Test
    @DisplayName("A response inside the target is compliant and the clock stops there")
    void responseWithinTargetIsCompliant() {
        givenBreakdown(
                at("2026-01-05T09:00:00"), WORKSHOP_ID,
                at("2026-01-05T11:00:00"), null);

        SlaClockSnapshot snapshot = serviceAt("2026-01-09T09:00:00").evaluate(BREAKDOWN_ID);

        assertEquals(120, snapshot.responseElapsedMinutes());
        assertEquals(SlaClockStatus.OK, snapshot.responseStatus());
    }

    @Test
    @DisplayName("A running clock past its target is breached even with nothing recorded")
    void runningClockBreaches() {
        givenBreakdown(at("2026-01-05T09:00:00"), WORKSHOP_ID, null, null);

        SlaClockSnapshot snapshot = serviceAt("2026-01-05T15:00:00").evaluate(BREAKDOWN_ID);

        assertEquals(360, snapshot.responseElapsedMinutes());
        assertEquals(SlaClockStatus.BREACHED, snapshot.responseStatus());
    }

    @Test
    @DisplayName("A depot-unreachable pause holds the clock and prevents a breach")
    void depotUnreachablePausePreventsBreach() {
        givenBreakdown(at("2026-01-05T09:00:00"), WORKSHOP_ID, null, null);
        when(awaitingRaisedRepository.findByBreakdownRequestId(BREAKDOWN_ID)).thenReturn(List.of(
                pause(com.example.backend.SLA.calendar.PauseInterval.DEPOT_UNREACHABLE,
                        at("2026-01-05T10:00:00"), at("2026-01-05T14:00:00"))));

        // 09:00 to 15:00 is 360 working minutes, less a 240 minute pause, so 120
        // against a 240 minute target: still compliant.
        SlaClockSnapshot snapshot = serviceAt("2026-01-05T15:00:00").evaluate(BREAKDOWN_ID);

        assertEquals(120, snapshot.responseElapsedMinutes());
        assertEquals(SlaClockStatus.OK, snapshot.responseStatus());
        assertEquals(240, snapshot.pausedMinutes());
    }

    @Test
    @DisplayName("Approaching the target reports AT_RISK before it is missed")
    void atRiskBeforeBreach() {
        givenBreakdown(at("2026-01-05T09:00:00"), WORKSHOP_ID, null, null);

        // 200 of a 240 minute target consumed, so 40 remain, inside the 20% window.
        SlaClockSnapshot snapshot = serviceAt("2026-01-05T12:20:00").evaluate(BREAKDOWN_ID);

        assertEquals(200, snapshot.responseElapsedMinutes());
        assertEquals(SlaClockStatus.AT_RISK, snapshot.responseStatus());
    }

    @Test
    @DisplayName("An unbooked breakdown still runs its clock, on continuous time")
    void unbookedBreakdownStillRunsAClock() {
        givenBreakdown(at("2026-01-02T17:00:00"), null, null, null);

        SlaClockSnapshot snapshot = serviceAt("2026-01-03T01:00:00").evaluate(BREAKDOWN_ID);

        assertEquals(ElapsedHoursSlaCalculator.BASIS, snapshot.calendarBasis());
        assertEquals(480, snapshot.responseElapsedMinutes(), "eight hours of continuous time");
        assertEquals(SlaClockStatus.BREACHED, snapshot.responseStatus());
    }

    @Test
    @DisplayName("Both clocks run from the report time, so resolution includes response")
    void bothClocksShareAnOrigin() {
        givenBreakdown(
                at("2026-01-05T09:00:00"), WORKSHOP_ID,
                at("2026-01-05T11:00:00"), at("2026-01-05T15:00:00"));

        SlaClockSnapshot snapshot = serviceAt("2026-01-09T09:00:00").evaluate(BREAKDOWN_ID);

        assertEquals(120, snapshot.responseElapsedMinutes());
        assertEquals(360, snapshot.resolutionElapsedMinutes());
        assertEquals(SlaClockStatus.OK, snapshot.resolutionStatus());
    }
}
