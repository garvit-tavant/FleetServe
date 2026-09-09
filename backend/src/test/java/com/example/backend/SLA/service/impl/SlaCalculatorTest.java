package com.example.backend.SLA.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.backend.ExecutionService.repository.BookingRepository;
import com.example.backend.ExecutionService.repository.WorkOrderRepository;
import com.example.backend.SLA.dto.BreakdownPriority;
import com.example.backend.SLA.dto.SlaBasis;
import com.example.backend.SLA.entity.SlaCheckpoint;
import com.example.backend.SLA.entity.SlaPolicy;
import com.example.backend.SLA.repository.BreakdownRequestRepository;
import com.example.backend.SLA.repository.SlaCheckpointRepository;
import com.example.backend.SLA.service.SlaCalculatorResolver;
import com.example.backend.SLA.service.SlaPolicyService;
import com.example.backend.SLA.service.SlaTimeCalculationStrategy;
import com.example.backend.common.exception.GlobalExceptionHandler.ConflictException;

/**
 * Unit tests for {@link SlaCalculator}. All collaborators are mocked; the
 * {@link Clock} is fixed so "now()" reads are deterministic.
 */
@ExtendWith(MockitoExtension.class)
class SlaCalculatorTest {

    private static final Long BOOKING_ID = 1L;
    private static final Long BREAKDOWN_ID = 10L;
    private static final Long WORKSHOP_ID = 100L;

    private static final OffsetDateTime NOW =
            OffsetDateTime.of(2026, 9, 9, 12, 0, 0, 0, ZoneOffset.UTC);

    @Mock private SlaCalculatorResolver strategyResolver;
    @Mock private SlaPolicyService slaPolicyService;
    @Mock private SlaCheckpointRepository slaCheckpointRepository;
    @Mock private BreakdownRequestRepository breakdownRequestRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private WorkOrderRepository workOrderRepository;
    @Mock private SlaTimeCalculationStrategy calendarStrategy;
    @Mock private SlaTimeCalculationStrategy workingStrategy;

    private Clock clock;
    private SlaCalculator slaCalculator;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(NOW.toInstant(), ZoneOffset.UTC);
        slaCalculator = new SlaCalculator(
                strategyResolver,
                slaPolicyService,
                clock,
                slaCheckpointRepository,
                breakdownRequestRepository,
                bookingRepository,
                workOrderRepository);
    }

    private SlaPolicy buildPolicy(long responseTarget, long resolutionTarget, SlaBasis basis) {
        SlaPolicy policy = new SlaPolicy();
        policy.setId(1L);
        policy.setPriority(BreakdownPriority.HIGH);
        policy.setResponseTargetMinutes(responseTarget);
        policy.setResolutionTargetMinutes(resolutionTarget);
        policy.setCalendarBasis(basis);
        policy.setEffectiveFrom(NOW.toLocalDate().minusDays(30));
        return policy;
    }

    // ------------------------------------------------------------ isResponseBreach

    @Test
    void isResponseBreach_returnsTrue_whenAlreadyFlaggedSticky() {
        when(slaCheckpointRepository.responseBreachByBookingId(BOOKING_ID)).thenReturn(true);

        boolean result = slaCalculator.isResponseBreach(BOOKING_ID);

        assertThat(result).isTrue();
        // sticky path should short-circuit before touching anything else
        verify(bookingRepository, never()).breakdownRequestIdByBookingId(any());
    }

    @Test
    void isResponseBreach_returnsFalse_whenNoBreakdownLinked() {
        when(slaCheckpointRepository.responseBreachByBookingId(BOOKING_ID)).thenReturn(false);
        when(bookingRepository.breakdownRequestIdByBookingId(BOOKING_ID)).thenReturn(null);

        boolean result = slaCalculator.isResponseBreach(BOOKING_ID);

        assertThat(result).isFalse();
    }

    @Test
    void isResponseBreach_returnsTrueAndPersists_whenElapsedExceedsTarget() {
        SlaPolicy policy = buildPolicy(60, 240, SlaBasis.WORKING_TIME);
        OffsetDateTime reportedAt = NOW.minusHours(3);

        when(slaCheckpointRepository.responseBreachByBookingId(BOOKING_ID)).thenReturn(false);
        when(bookingRepository.breakdownRequestIdByBookingId(BOOKING_ID)).thenReturn(BREAKDOWN_ID);
        when(bookingRepository.workshopIdByBookingId(BOOKING_ID)).thenReturn(WORKSHOP_ID);
        when(breakdownRequestRepository.requestraisedtimebyid(BREAKDOWN_ID)).thenReturn(reportedAt);
        when(workOrderRepository.findStartedAtByBookingId(BOOKING_ID)).thenReturn(null);
        when(breakdownRequestRepository.pinnedSlaPolicyById(BREAKDOWN_ID)).thenReturn(policy);
        when(strategyResolver.resolve(SlaBasis.CALENDAR_TIME)).thenReturn(calendarStrategy);
        when(calendarStrategy.calculateElapsedTime(eq(reportedAt), eq(NOW), eq(WORKSHOP_ID)))
                .thenReturn(180L); // 3 hours > 60 min target

        boolean result = slaCalculator.isResponseBreach(BOOKING_ID);

        assertThat(result).isTrue();
        verify(slaCheckpointRepository, times(1)).updateResponseBreach(BOOKING_ID);
    }

    @Test
    void isResponseBreach_returnsFalse_whenWithinTarget() {
        SlaPolicy policy = buildPolicy(60, 240, SlaBasis.WORKING_TIME);
        OffsetDateTime reportedAt = NOW.minusMinutes(30);

        when(slaCheckpointRepository.responseBreachByBookingId(BOOKING_ID)).thenReturn(false);
        when(bookingRepository.breakdownRequestIdByBookingId(BOOKING_ID)).thenReturn(BREAKDOWN_ID);
        when(bookingRepository.workshopIdByBookingId(BOOKING_ID)).thenReturn(WORKSHOP_ID);
        when(breakdownRequestRepository.requestraisedtimebyid(BREAKDOWN_ID)).thenReturn(reportedAt);
        when(workOrderRepository.findStartedAtByBookingId(BOOKING_ID)).thenReturn(null);
        when(breakdownRequestRepository.pinnedSlaPolicyById(BREAKDOWN_ID)).thenReturn(policy);
        when(strategyResolver.resolve(SlaBasis.CALENDAR_TIME)).thenReturn(calendarStrategy);
        when(calendarStrategy.calculateElapsedTime(eq(reportedAt), eq(NOW), eq(WORKSHOP_ID)))
                .thenReturn(30L); // within 60 min target

        boolean result = slaCalculator.isResponseBreach(BOOKING_ID);

        assertThat(result).isFalse();
        verify(slaCheckpointRepository, never()).updateResponseBreach(any());
    }

    // ---------------------------------------------------------- isResolutionBreach

    @Test
    void isResolutionBreach_returnsFalse_whenNoWorkshopAssigned() {
        when(slaCheckpointRepository.resolutionBreachByBookingId(BOOKING_ID)).thenReturn(false);
        when(bookingRepository.breakdownRequestIdByBookingId(BOOKING_ID)).thenReturn(BREAKDOWN_ID);
        when(bookingRepository.workshopIdByBookingId(BOOKING_ID)).thenReturn(null);

        boolean result = slaCalculator.isResolutionBreach(BOOKING_ID);

        assertThat(result).isFalse();
    }

    @Test
    void isResolutionBreach_returnsTrueAndPersists_whenElapsedMinusAwaitingExceedsTarget() {
        SlaPolicy policy = buildPolicy(60, 120, SlaBasis.WORKING_TIME);
        OffsetDateTime reportedAt = NOW.minusHours(5);

        when(slaCheckpointRepository.resolutionBreachByBookingId(BOOKING_ID)).thenReturn(false);
        when(bookingRepository.breakdownRequestIdByBookingId(BOOKING_ID)).thenReturn(BREAKDOWN_ID);
        when(bookingRepository.workshopIdByBookingId(BOOKING_ID)).thenReturn(WORKSHOP_ID);
        when(breakdownRequestRepository.requestraisedtimebyid(BREAKDOWN_ID)).thenReturn(reportedAt);
        when(slaCheckpointRepository.resolvedAtByBookingId(BOOKING_ID)).thenReturn(null);
        when(breakdownRequestRepository.pinnedSlaPolicyById(BREAKDOWN_ID)).thenReturn(policy);
        when(strategyResolver.resolve(SlaBasis.WORKING_TIME)).thenReturn(workingStrategy);
        when(workingStrategy.calculateElapsedTime(eq(reportedAt), eq(NOW), eq(WORKSHOP_ID)))
                .thenReturn(300L); // 5 hours elapsed

        // calculateAwaitingTime internals: checkpoint present, no accumulated/open pause
        when(slaCheckpointRepository.findByBookingId(BOOKING_ID))
                .thenReturn(Optional.of(new SlaCheckpoint()));
        when(slaCheckpointRepository.accumulatedAwaitingMinutesByBookingId(BOOKING_ID)).thenReturn(0L);
        when(slaCheckpointRepository.lastAwaitingRaisedAtByBookingId(BOOKING_ID)).thenReturn(null);

        boolean result = slaCalculator.isResolutionBreach(BOOKING_ID);

        // 300 - 0 = 300 > 120 target
        assertThat(result).isTrue();
        verify(slaCheckpointRepository, times(1)).updateResolutionBreach(BOOKING_ID);
    }

    @Test
    void isResolutionBreach_returnsFalse_whenAwaitingPauseBringsElapsedWithinTarget() {
        SlaPolicy policy = buildPolicy(60, 120, SlaBasis.WORKING_TIME);
        OffsetDateTime reportedAt = NOW.minusHours(5);

        when(slaCheckpointRepository.resolutionBreachByBookingId(BOOKING_ID)).thenReturn(false);
        when(bookingRepository.breakdownRequestIdByBookingId(BOOKING_ID)).thenReturn(BREAKDOWN_ID);
        when(bookingRepository.workshopIdByBookingId(BOOKING_ID)).thenReturn(WORKSHOP_ID);
        when(breakdownRequestRepository.requestraisedtimebyid(BREAKDOWN_ID)).thenReturn(reportedAt);
        when(slaCheckpointRepository.resolvedAtByBookingId(BOOKING_ID)).thenReturn(null);
        when(breakdownRequestRepository.pinnedSlaPolicyById(BREAKDOWN_ID)).thenReturn(policy);
        when(strategyResolver.resolve(SlaBasis.WORKING_TIME)).thenReturn(workingStrategy);
        when(workingStrategy.calculateElapsedTime(eq(reportedAt), eq(NOW), eq(WORKSHOP_ID)))
                .thenReturn(300L); // 5 hours elapsed

        // 200 accumulated awaiting minutes already stored, no pause currently open
        when(slaCheckpointRepository.findByBookingId(BOOKING_ID))
                .thenReturn(Optional.of(new SlaCheckpoint()));
        when(slaCheckpointRepository.accumulatedAwaitingMinutesByBookingId(BOOKING_ID)).thenReturn(200L);
        when(slaCheckpointRepository.lastAwaitingRaisedAtByBookingId(BOOKING_ID)).thenReturn(null);

        boolean result = slaCalculator.isResolutionBreach(BOOKING_ID);

        // 300 - 200 = 100 <= 120 target
        assertThat(result).isFalse();
        verify(slaCheckpointRepository, never()).updateResolutionBreach(any());
    }

    // ------------------------------------------------------- calculateAwaitingTime

    @Test
    void calculateAwaitingTime_throwsConflictException_whenNoCheckpointExists() {
        when(slaCheckpointRepository.findByBookingId(BOOKING_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> slaCalculator.calculateAwaitingTime(BOOKING_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("No SLA checkpoint for booking " + BOOKING_ID);
    }

    @Test
    void calculateAwaitingTime_includesOpenPause_measuredUpToClockNow() {
        OffsetDateTime openSince = NOW.minusMinutes(45);

        when(slaCheckpointRepository.findByBookingId(BOOKING_ID))
                .thenReturn(Optional.of(new SlaCheckpoint()));
        when(slaCheckpointRepository.accumulatedAwaitingMinutesByBookingId(BOOKING_ID)).thenReturn(10L);
        when(slaCheckpointRepository.lastAwaitingRaisedAtByBookingId(BOOKING_ID)).thenReturn(openSince);
        when(bookingRepository.workshopIdByBookingId(BOOKING_ID)).thenReturn(WORKSHOP_ID);
        when(bookingRepository.breakdownRequestIdByBookingId(BOOKING_ID)).thenReturn(BREAKDOWN_ID);
        when(breakdownRequestRepository.pinnedSlaPolicyById(BREAKDOWN_ID)).thenReturn(null);
        when(breakdownRequestRepository.priorityById(BREAKDOWN_ID)).thenReturn(BreakdownPriority.HIGH);
        when(breakdownRequestRepository.requestraisedtimebyid(BREAKDOWN_ID)).thenReturn(NOW.minusDays(1));
        when(slaPolicyService.getEffectivePolicy(eq(BreakdownPriority.HIGH), any())).thenReturn(null);
        when(strategyResolver.resolve(SlaBasis.WORKING_TIME)).thenReturn(workingStrategy);
        when(workingStrategy.calculateElapsedTime(eq(openSince), eq(NOW), eq(WORKSHOP_ID)))
                .thenReturn(45L);

        long result = slaCalculator.calculateAwaitingTime(BOOKING_ID);

        assertThat(result).isEqualTo(55L); // 10 accumulated + 45 open
    }

    // ------------------------------------------------------------- raiseAwaiting

    @Test
    void raiseAwaiting_throwsIllegalArgumentException_whenTimestampNull() {
        assertThatThrownBy(() -> slaCalculator.raiseAwaiting(BOOKING_ID, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void raiseAwaiting_throwsConflictException_whenRepositoryReportsNoUpdate() {
        OffsetDateTime raisedAt = NOW;
        when(slaCheckpointRepository.raiseAwaiting(BOOKING_ID, raisedAt)).thenReturn(0);

        assertThatThrownBy(() -> slaCalculator.raiseAwaiting(BOOKING_ID, raisedAt))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot raise awaiting-parts");
    }

    @Test
    void raiseAwaiting_succeeds_whenRepositoryUpdatesOneRow() {
        OffsetDateTime raisedAt = NOW;
        when(slaCheckpointRepository.raiseAwaiting(BOOKING_ID, raisedAt)).thenReturn(1);

        slaCalculator.raiseAwaiting(BOOKING_ID, raisedAt);

        verify(slaCheckpointRepository, times(1)).raiseAwaiting(BOOKING_ID, raisedAt);
    }

    // ------------------------------------------------------------ resolveAwaiting

    @Test
    void resolveAwaiting_throwsIllegalArgumentException_whenTimestampNull() {
        assertThatThrownBy(() -> slaCalculator.resolveAwaiting(BOOKING_ID, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resolveAwaiting_throwsConflictException_whenNoPauseOpen() {
        when(slaCheckpointRepository.lastAwaitingRaisedAtByBookingId(BOOKING_ID)).thenReturn(null);

        assertThatThrownBy(() -> slaCalculator.resolveAwaiting(BOOKING_ID, NOW))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("no pause is open");
    }

    @Test
    void resolveAwaiting_throwsIllegalArgumentException_whenResolvedBeforeRaised() {
        OffsetDateTime openSince = NOW;
        OffsetDateTime resolvedAt = NOW.minusMinutes(5);
        when(slaCheckpointRepository.lastAwaitingRaisedAtByBookingId(BOOKING_ID)).thenReturn(openSince);

        assertThatThrownBy(() -> slaCalculator.resolveAwaiting(BOOKING_ID, resolvedAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("before the pause was raised");
    }

    @Test
    void resolveAwaiting_throwsConflictException_whenConcurrentlyResolved() {
        OffsetDateTime openSince = NOW.minusMinutes(30);
        when(slaCheckpointRepository.lastAwaitingRaisedAtByBookingId(BOOKING_ID)).thenReturn(openSince);
        when(bookingRepository.workshopIdByBookingId(BOOKING_ID)).thenReturn(null); // no workshop -> 0 minutes
        when(bookingRepository.breakdownRequestIdByBookingId(BOOKING_ID)).thenReturn(null);
        when(slaCheckpointRepository.resolveAwaitingIfOpen(BOOKING_ID, openSince, 0L)).thenReturn(0);

        assertThatThrownBy(() -> slaCalculator.resolveAwaiting(BOOKING_ID, NOW))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already resolved concurrently");
    }

    @Test
    void resolveAwaiting_succeeds_andPersistsMeasuredMinutes() {
        OffsetDateTime openSince = NOW.minusMinutes(30);
        SlaPolicy policy = buildPolicy(60, 120, SlaBasis.WORKING_TIME);

        when(slaCheckpointRepository.lastAwaitingRaisedAtByBookingId(BOOKING_ID)).thenReturn(openSince);
        when(bookingRepository.workshopIdByBookingId(BOOKING_ID)).thenReturn(WORKSHOP_ID);
        when(bookingRepository.breakdownRequestIdByBookingId(BOOKING_ID)).thenReturn(BREAKDOWN_ID);
        when(breakdownRequestRepository.pinnedSlaPolicyById(BREAKDOWN_ID)).thenReturn(policy);
        when(strategyResolver.resolve(SlaBasis.WORKING_TIME)).thenReturn(workingStrategy);
        when(workingStrategy.calculateElapsedTime(openSince, NOW, WORKSHOP_ID)).thenReturn(30L);
        when(slaCheckpointRepository.resolveAwaitingIfOpen(BOOKING_ID, openSince, 30L)).thenReturn(1);

        slaCalculator.resolveAwaiting(BOOKING_ID, NOW);

        verify(slaCheckpointRepository, times(1))
                .resolveAwaitingIfOpen(BOOKING_ID, openSince, 30L);
    }

    // ------------------------------------------------------ remaining-minutes clocks

    @Test
    void getResponseRemainingMinutes_returnsNull_whenNoBreakdownLinked() {
        when(bookingRepository.breakdownRequestIdByBookingId(BOOKING_ID)).thenReturn(null);

        Long result = slaCalculator.getResponseRemainingMinutes(BOOKING_ID);

        assertThat(result).isNull();
    }

    @Test
    void getResponseRemainingMinutes_returnsPositiveValue_whenWithinTarget() {
        SlaPolicy policy = buildPolicy(60, 240, SlaBasis.WORKING_TIME);
        OffsetDateTime reportedAt = NOW.minusMinutes(20);

        when(bookingRepository.breakdownRequestIdByBookingId(BOOKING_ID)).thenReturn(BREAKDOWN_ID);
        when(breakdownRequestRepository.requestraisedtimebyid(BREAKDOWN_ID)).thenReturn(reportedAt);
        when(breakdownRequestRepository.pinnedSlaPolicyById(BREAKDOWN_ID)).thenReturn(policy);
        when(bookingRepository.workshopIdByBookingId(BOOKING_ID)).thenReturn(WORKSHOP_ID);
        when(workOrderRepository.findStartedAtByBookingId(BOOKING_ID)).thenReturn(null);
        when(strategyResolver.resolve(SlaBasis.CALENDAR_TIME)).thenReturn(calendarStrategy);
        when(calendarStrategy.calculateElapsedTime(reportedAt, NOW, WORKSHOP_ID)).thenReturn(20L);

        Long result = slaCalculator.getResponseRemainingMinutes(BOOKING_ID);

        assertThat(result).isEqualTo(40L); // 60 target - 20 elapsed
    }

    @Test
    void getResolutionRemainingMinutes_returnsNull_whenNoWorkshopAssigned() {
        when(bookingRepository.breakdownRequestIdByBookingId(BOOKING_ID)).thenReturn(BREAKDOWN_ID);
        when(bookingRepository.workshopIdByBookingId(BOOKING_ID)).thenReturn(null);

        Long result = slaCalculator.getResolutionRemainingMinutes(BOOKING_ID);

        assertThat(result).isNull();
    }

    @Test
    void getResolutionRemainingMinutes_subtractsAwaitingTime_fromElapsed() {
        SlaPolicy policy = buildPolicy(60, 120, SlaBasis.WORKING_TIME);
        OffsetDateTime reportedAt = NOW.minusHours(2);

        when(bookingRepository.breakdownRequestIdByBookingId(BOOKING_ID)).thenReturn(BREAKDOWN_ID);
        when(bookingRepository.workshopIdByBookingId(BOOKING_ID)).thenReturn(WORKSHOP_ID);
        when(breakdownRequestRepository.requestraisedtimebyid(BREAKDOWN_ID)).thenReturn(reportedAt);
        when(breakdownRequestRepository.pinnedSlaPolicyById(BREAKDOWN_ID)).thenReturn(policy);
        when(slaCheckpointRepository.resolvedAtByBookingId(BOOKING_ID)).thenReturn(null);
        when(strategyResolver.resolve(SlaBasis.WORKING_TIME)).thenReturn(workingStrategy);
        when(workingStrategy.calculateElapsedTime(reportedAt, NOW, WORKSHOP_ID)).thenReturn(100L);

        when(slaCheckpointRepository.findByBookingId(BOOKING_ID))
                .thenReturn(Optional.of(new SlaCheckpoint()));
        when(slaCheckpointRepository.accumulatedAwaitingMinutesByBookingId(BOOKING_ID)).thenReturn(20L);
        when(slaCheckpointRepository.lastAwaitingRaisedAtByBookingId(BOOKING_ID)).thenReturn(null);

        Long result = slaCalculator.getResolutionRemainingMinutes(BOOKING_ID);

        // target 120 - (elapsed 100 - awaiting 20) = 120 - 80 = 40
        assertThat(result).isEqualTo(40L);
    }
}
