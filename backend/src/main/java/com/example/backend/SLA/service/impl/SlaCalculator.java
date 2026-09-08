package com.example.backend.SLA.service.impl;

import java.time.OffsetDateTime;

import com.example.backend.ExecutionService.repository.BookingRepository;
import com.example.backend.ExecutionService.repository.WorkOrderRepository;
import com.example.backend.SLA.repository.BreakdownRequestRepository;
import com.example.backend.SLA.repository.SlaCheckpointRepository;
import com.example.backend.SLA.service.SlaTimeCalculationStrategy;
import com.example.backend.SLA.dto.SlaClockStatus;

/**
 * SLA clock arithmetic, keyed on booking_id.
 *
 * Every booking - corrective (breakdown-driven) or preventive
 * (maintenance-plan-driven) - has exactly one sla_checkpoint, so clock
 * tracking (responded/resolved timestamps, awaiting-parts pauses) works
 * uniformly for both.
 *
 * Breach evaluation, however, needs *targets*, and targets live on
 * breakdown_request.sla_policy. Preventive bookings have no policy, so the
 * breach / remaining-minutes methods return false / null for them rather than
 * inventing a target.
 */
public class SlaCalculator {

    private static final long AT_RISK_THRESHOLD_MINUTES = 30;

    private final SlaTimeCalculationStrategy responseStrategy;
    private final SlaTimeCalculationStrategy resolutionStrategy;
    private final SlaCheckpointRepository slaCheckpointRepository;
    private final BreakdownRequestRepository breakdownRequestRepository;
    private final BookingRepository bookingRepository;
    private final WorkOrderRepository workOrderRepository;

    public SlaCalculator(SlaTimeCalculationStrategy responseStrategy,
                         SlaTimeCalculationStrategy resolutionStrategy,
                         SlaCheckpointRepository slaCheckpointRepository,
                         BreakdownRequestRepository breakdownRequestRepository,
                         BookingRepository bookingRepository,
                         WorkOrderRepository workOrderRepository) {
        this.responseStrategy = responseStrategy;
        this.resolutionStrategy = resolutionStrategy;
        this.slaCheckpointRepository = slaCheckpointRepository;
        this.breakdownRequestRepository = breakdownRequestRepository;
        this.bookingRepository = bookingRepository;
        this.workOrderRepository = workOrderRepository;
    }

    // ---------------------------------------------------------------- helpers

    /** Workshop lives on the booking, so this resolves for preventive jobs too. */
    private Long workshopIdOf(Long bookingId) {
        return bookingRepository.workshopIdByBookingId(bookingId);
    }

    /** Null for preventive bookings - no breakdown request, hence no SLA targets. */
    private Long breakdownIdOf(Long bookingId) {
        return bookingRepository.breakdownRequestIdByBookingId(bookingId);
    }

    // --------------------------------------------------------------- breaches

    public boolean isResponseBreach(Long bookingId) {
        if (Boolean.TRUE.equals(slaCheckpointRepository.responseBreachByBookingId(bookingId))) {
            return true; // already flagged; breaches are sticky
        }

        Long breakdownId = breakdownIdOf(bookingId);
        if (breakdownId == null) {
            return false; // preventive job: no response target to breach
        }

        Long workshopId = workshopIdOf(bookingId);
        OffsetDateTime reportedAt = breakdownRequestRepository.requestraisedtimebyid(breakdownId);
        if (workshopId == null || reportedAt == null) {
            return false;
        }

        // Not responded yet -> measure against now, so an overdue open ticket
        // still breaches instead of waiting for a response that never comes.
        OffsetDateTime respondedAt = slaCheckpointRepository.respondedAtByBookingId(bookingId);
        OffsetDateTime end = (respondedAt != null) ? respondedAt : OffsetDateTime.now();

        long targetMinutes = breakdownRequestRepository.responsetimebyid(breakdownId);
        long elapsedMinutes = responseStrategy.calculateElapsedTime(reportedAt, end, workshopId);

        if (elapsedMinutes > targetMinutes) {
            slaCheckpointRepository.updateResponseBreach(bookingId);
            return true;
        }
        return false;
    }

    public boolean isResolutionBreach(Long bookingId) {
        if (Boolean.TRUE.equals(slaCheckpointRepository.resolutionBreachByBookingId(bookingId))) {
            return true;
        }

        Long breakdownId = breakdownIdOf(bookingId);
        if (breakdownId == null) {
            return false; // preventive job: no resolution target to breach
        }

        Long workshopId = workshopIdOf(bookingId);
        OffsetDateTime startedAt = workOrderRepository.findStartedAtByBookingId(bookingId);
        if (workshopId == null || startedAt == null) {
            return false; // clock hasn't started yet
        }

        OffsetDateTime resolvedAt = slaCheckpointRepository.resolvedAtByBookingId(bookingId);
        OffsetDateTime end = (resolvedAt != null) ? resolvedAt : OffsetDateTime.now();

        long targetMinutes = breakdownRequestRepository.resolutiontimebyid(breakdownId);
        long elapsedMinutes = resolutionStrategy.calculateElapsedTime(startedAt, end, workshopId);
        long awaitingMinutes = calculateAwaitingTime(bookingId);

        if ((elapsedMinutes - awaitingMinutes) > targetMinutes) {
            slaCheckpointRepository.updateResolutionBreach(bookingId);
            return true;
        }
        return false;
    }

    // ------------------------------------------------------- awaiting (pauses)

    /**
     * Total paused minutes so far: the precomputed running total, plus - if a
     * pause is currently open - the time elapsed since it was raised.
     */
    public long calculateAwaitingTime(Long bookingId) {
        Long accumulated = slaCheckpointRepository.accumulatedAwaitingMinutesByBookingId(bookingId);
        long total = (accumulated != null) ? accumulated : 0L;

        OffsetDateTime openSince = slaCheckpointRepository.lastAwaitingRaisedAtByBookingId(bookingId);
        if (openSince != null) {
            Long workshopId = workshopIdOf(bookingId);
            if (workshopId != null) {
                total += resolutionStrategy.calculateElapsedTime(openSince, OffsetDateTime.now(), workshopId);
            }
        }
        return total;
    }

    /**
     * Opens an awaiting-parts pause. Throws if one is already open (or the
     * checkpoint is missing) rather than silently doing nothing, so an illegal
     * double-raise surfaces as a real error.
     */
    public void raiseAwaiting(Long bookingId, OffsetDateTime awaitingPartsAt) {
        int updated = slaCheckpointRepository.raiseAwaiting(bookingId, awaitingPartsAt);
        if (updated == 0) {
            throw new IllegalStateException(
                    "Cannot raise awaiting-parts for booking " + bookingId
                            + ": a pause is already open, or no SLA checkpoint exists.");
        }
    }

    /** Closes the open pause, folding its duration into the running total. */
    public void resolveAwaiting(Long bookingId, OffsetDateTime awaitingPartsResolvedAt) {
        OffsetDateTime openSince = slaCheckpointRepository.lastAwaitingRaisedAtByBookingId(bookingId);
        if (openSince == null) {
            throw new IllegalStateException(
                    "Cannot resolve awaiting-parts for booking " + bookingId + ": no pause is open.");
        }
        Long workshopId = workshopIdOf(bookingId);
        long minutes = (workshopId == null)
                ? 0L
                : resolutionStrategy.calculateElapsedTime(openSince, awaitingPartsResolvedAt, workshopId);
        slaCheckpointRepository.resolveAwaiting(bookingId, minutes);
    }

    // -------------------------------------------------------- remaining clocks

    /** Null when there is no response target (preventive job) or no start point. */
    public Long getResponseRemainingMinutes(Long bookingId) {
        Long breakdownId = breakdownIdOf(bookingId);
        if (breakdownId == null) {
            return null;
        }
        Long workshopId = workshopIdOf(bookingId);
        OffsetDateTime reportedAt = breakdownRequestRepository.requestraisedtimebyid(breakdownId);
        if (workshopId == null || reportedAt == null) {
            return null;
        }
        long targetMinutes = breakdownRequestRepository.responsetimebyid(breakdownId);
        long elapsedMinutes = responseStrategy.calculateElapsedTime(reportedAt, OffsetDateTime.now(), workshopId);
        return targetMinutes - elapsedMinutes;
    }

    /** Null when there is no resolution target (preventive job) or work hasn't started. */
    public Long getResolutionRemainingMinutes(Long bookingId) {
        Long breakdownId = breakdownIdOf(bookingId);
        if (breakdownId == null) { // no breakdown request means no resolution target
            return null;
        }
        Long workshopId = workshopIdOf(bookingId);
        OffsetDateTime startedAt = workOrderRepository.findStartedAtByBookingId(bookingId);
        if (workshopId == null || startedAt == null) {
            return null; // clock hasn't started yet
        }
        long targetMinutes = breakdownRequestRepository.resolutiontimebyid(breakdownId);
        long elapsedMinutes = resolutionStrategy.calculateElapsedTime(startedAt, OffsetDateTime.now(), workshopId);
        long awaitingMinutes = calculateAwaitingTime(bookingId);
        return targetMinutes - (elapsedMinutes - awaitingMinutes);
    }

    public SlaClockStatus classify(Long remainingMinutes) {
        if (remainingMinutes == null) return SlaClockStatus.OK;
        if (remainingMinutes < 0) return SlaClockStatus.BREACHED;
        if (remainingMinutes <= AT_RISK_THRESHOLD_MINUTES) return SlaClockStatus.AT_RISK;
        return SlaClockStatus.OK;
    }
}
