package com.example.backend.SLA.service;

import java.time.OffsetDateTime;
import java.util.List;

import com.example.backend.SLA.dto.SlaClockStatus;
import com.example.backend.SLA.entity.AwaitingRaised;
import com.example.backend.SLA.repository.AwaitingRaisedRepository;
import com.example.backend.SLA.repository.BreakdownRequestRepository;
import com.example.backend.SLA.repository.SlaCheckpointRepository;


/**
 * SLA clock calculator using injected time-calculation strategies.
 * Supports multiple bases (working-hours, calendar-hours, etc.) via strategy pattern.
 *
 * This class is stateless; strategies are injected per instance or per-call
 * so that response and resolution clocks can use different strategies if needed,
 * or the same strategy can be shared across multiple breakdowns.
 *
 * The strategy is responsible for respecting workshop calendars, holidays,
 * shift hours, and any other calendar basis logic. SlaCalculator remains
 * agnostic to the time calculation detail.
 */
public class SlaCalculator {

    // ans: US-4.1 "soon to breach" support - a clock is AT_RISK once less than this
    // many working minutes remain before its target. Business-tunable; consider
    // moving to SlaPolicy as a per-priority column if P1 vs P3 need different
    // warning windows rather than one fixed value for all priorities.
    private static final long AT_RISK_THRESHOLD_MINUTES = 30;

    private final SlaTimeCalculationStrategy responseStrategy;
    private final SlaTimeCalculationStrategy resolutionStrategy;
    private final AwaitingRaisedRepository awaitingRaisedRepository;
    private final SlaCheckpointRepository slaCheckpointRepository;
    private final BreakdownRequestRepository breakdownRequestRepository;

    /**
     * Constructor accepting separate strategies for response and resolution phases.
     * This allows SlaPolicy.calendarBasis to be applied differently per phase if needed,
     * though typically both phases use the same strategy derived from the policy.
     */
    public SlaCalculator(
            SlaTimeCalculationStrategy responseStrategy,
            SlaTimeCalculationStrategy resolutionStrategy,
            AwaitingRaisedRepository awaitingRaisedRepository,
            SlaCheckpointRepository slaCheckpointRepository,
            BreakdownRequestRepository breakdownRequestRepository) {
        this.responseStrategy = responseStrategy;
        this.resolutionStrategy = resolutionStrategy;
        this.awaitingRaisedRepository = awaitingRaisedRepository;
        this.slaCheckpointRepository = slaCheckpointRepository;
        this.breakdownRequestRepository = breakdownRequestRepository;
    }


    /**
     * Check if the response SLA is already in breach.
     * Returns early if a prior calculation cached the result (responseBreachfindbyID returned true).
     * Otherwise, delegates to getResponseRemainingMinutes to compute current status.
     */
    public boolean isResponseBreach(long breakdownID) {
        Boolean alreadyBreached = slaCheckpointRepository.responseBreachfindbyID(breakdownID);
        if (Boolean.TRUE.equals(alreadyBreached)) {
            return true;
        }

        // If a response time has already been recorded, we've already evaluated breach status once
        // and it wasn't true (else alreadyBreached would have been set). Return early.
        if (slaCheckpointRepository.respondedatbyID(breakdownID) != null) {
            return false;
        }

        long remaining = getResponseRemainingMinutes(breakdownID);
        if(remaining<0) slaCheckpointRepository.updateResponseBreach(breakdownID);
        return remaining < 0;
    }

    /**
     * Check if the resolution SLA is already in breach.
     * Returns early if a prior calculation cached the result (resolutionBreachfindbyID returned true).
     * Otherwise, delegates to getResolutionRemainingMinutes to compute current status.
     */
    public boolean isResolutionBreach(long breakdownID, long workshopID) {
        Boolean alreadyBreached = slaCheckpointRepository.resolutionBreachfindbyID(breakdownID);
        if (Boolean.TRUE.equals(alreadyBreached)) {
            return true;
        }

        if (slaCheckpointRepository.resolutionByID(breakdownID) != null) {
            return false;
        }

        Long remaining = getResolutionRemainingMinutes(breakdownID, workshopID);
        if(remaining != null && remaining < 0) slaCheckpointRepository.updateResolutionBreach(breakdownID);
        return remaining != null && remaining < 0;
    }


    /**
     * Calculate total awaiting time for a breakdown (sum of all awaiting pauses).
     * Uses the resolution strategy since awaiting time is typically measured during the resolution phase.
     */
    public long calculateAwaitingTime(long breakdownID, long workshopID) {
        List<AwaitingRaised> awaitingRecords = awaitingRaisedRepository.getBreakdownRequestId(breakdownID);

        long totalAwaitingMinutes = 0;
        for (AwaitingRaised awaiting : awaitingRecords) {
            OffsetDateTime raiseTime = awaiting.getRaisedAt();
            OffsetDateTime resolveTime = awaiting.getResolvedAt() != null ? awaiting.getResolvedAt() : OffsetDateTime.now();
            totalAwaitingMinutes += resolutionStrategy.calculateElapsedTime(raiseTime, resolveTime, workshopID);
        }

        return totalAwaitingMinutes;
    }

    // ---- US-4.1 "soon to breach" support -----------------------------------

    /**
     * Compute working minutes still available before the response target is missed.
     * Negative means already breached by that many minutes.
     *
     * Uses the injected responseStrategy to measure elapsed time per SlaPolicy.calendarBasis.
     *
     */
    public long getResponseRemainingMinutes(long breakdownID, long workshopID) {
        long targetMinutes = breakdownRequestRepository.responsetimebyid(breakdownID);
        OffsetDateTime reportedAt = breakdownRequestRepository.requestraisedtimebyid(breakdownID);
        long elapsedMinutes = responseStrategy.calculateElapsedTime(reportedAt, OffsetDateTime.now(), workshopID);
        return targetMinutes - elapsedMinutes;
    }

    /**
     * Overload for backward compatibility when workshopID is not available.
     * Falls back to computing response remaining without workshop context.
     * This is a workaround for the workshop-assignment gap; prefer the workshopID version.
     */
    public long getResponseRemainingMinutes(long breakdownID) {
        long targetMinutes = breakdownRequestRepository.responsetimebyid(breakdownID);
        OffsetDateTime reportedAt = breakdownRequestRepository.requestraisedtimebyid(breakdownID);
        // Use workshopID = 0 as a sentinel; strategies that depend on workshop context (working-hours)
        // should handle or reject this gracefully. Calendar-hours strategy ignores it.
        long elapsedMinutes = responseStrategy.calculateElapsedTime(reportedAt, OffsetDateTime.now(), 0L);
        return targetMinutes - elapsedMinutes;
    }

    /**
     * Compute working minutes still available before the resolution target is missed.
     * Returns null if the resolution clock hasn't started yet (breakdown not yet responded to).
     *
     * Uses the injected resolutionStrategy to measure elapsed time per SlaPolicy.calendarBasis,
     * and subtracts awaiting pauses (periods when the team was blocked waiting for parts/approval).
     */
    public Long getResolutionRemainingMinutes(long breakdownID, long workshopID) {
        OffsetDateTime respondedAt = slaCheckpointRepository.respondedatbyID(breakdownID);
        if (respondedAt == null) {
            return null; // clock hasn't started yet
        }

        long targetMinutes = breakdownRequestRepository.resolutiontimebyid(breakdownID);
        long elapsedMinutes = resolutionStrategy.calculateElapsedTime(respondedAt, OffsetDateTime.now(), workshopID);
        long awaitingMinutes = calculateAwaitingTime(breakdownID, workshopID);

        return targetMinutes - (elapsedMinutes - awaitingMinutes);
    }

    /**
     * Classify remaining minutes into a status for the "soon to breach" dashboard.
     * Returns OK, AT_RISK, or BREACHED.
     */
    public SlaClockStatus classify(Long remainingMinutes) {
        if (remainingMinutes == null) {
            return SlaClockStatus.OK; // clock hasn't started - nothing to warn about yet
        }
        if (remainingMinutes < 0) {
            return SlaClockStatus.BREACHED;
        }
        if (remainingMinutes <= AT_RISK_THRESHOLD_MINUTES) {
            return SlaClockStatus.AT_RISK;
        }
        return SlaClockStatus.OK;
    }




}
