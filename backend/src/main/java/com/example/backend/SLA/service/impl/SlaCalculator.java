package com.example.backend.SLA.service.impl;

import java.time.Clock;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;

import com.example.backend.ExecutionService.repository.BookingRepository;
import com.example.backend.ExecutionService.repository.WorkOrderRepository;
import com.example.backend.SLA.dto.BreakdownPriority;
import com.example.backend.SLA.dto.SlaBasis;
import com.example.backend.SLA.entity.SlaPolicy;
import com.example.backend.SLA.repository.BreakdownRequestRepository;
import com.example.backend.SLA.repository.SlaCheckpointRepository;
import com.example.backend.SLA.service.SlaCalculatorResolver;
import com.example.backend.SLA.service.SlaPolicyService;
import com.example.backend.SLA.service.SlaTimeCalculationStrategy;
import com.example.backend.common.exception.GlobalExceptionHandler.ConflictException;

/**
 * SLA clock orchestrator, keyed on booking_id. KEEPS ITS NAME (Option B): the
 * extension point is the {@link SlaTimeCalculationStrategy} interface, which is
 * the "SlaCalculator" the spec (Section 3.2) refers to — see ADR / open-questions.
 *
 * <p>DYNAMIC STRATEGY SELECTION (Edits 1-6):
 *   - Response clock  (reportedAt -> startedAt): ALWAYS CALENDAR_TIME, because it
 *     starts before a booking exists, so there is no workshop calendar to query.
 *   - Resolution clock (reportedAt -> completedAt, minus AWAITING_PARTS): uses the
 *     pinned policy's calendar_basis (WORKING_TIME by default), resolved per call.
 *
 * <p>The service holds NO mutable strategy field — it resolves a local strategy
 * each call via {@link SlaCalculatorResolver}, so it stays stateless and
 * thread-safe as a Spring singleton. All "now" reads go through the injected
 * {@link Clock} (mandated by Section 3.2.1) so SLA behaviour is testable.
 */
@Service
public class SlaCalculator {

    // Edit 1: two fixed strategy fields replaced by one resolver + policy + clock.
    private final SlaCalculatorResolver strategyResolver;
    private final SlaPolicyService slaPolicyService;
    private final Clock clock;
    private final SlaCheckpointRepository slaCheckpointRepository;
    private final BreakdownRequestRepository breakdownRequestRepository;
    private final BookingRepository bookingRepository;
    private final WorkOrderRepository workOrderRepository;

    // Edit 2: constructor updated to match.
    public SlaCalculator(SlaCalculatorResolver strategyResolver,
                         SlaPolicyService slaPolicyService,
                         Clock clock,
                         SlaCheckpointRepository slaCheckpointRepository,
                         BreakdownRequestRepository breakdownRequestRepository,
                         BookingRepository bookingRepository,
                         WorkOrderRepository workOrderRepository) {
        this.strategyResolver = strategyResolver;
        this.slaPolicyService = slaPolicyService;
        this.clock = clock;
        this.slaCheckpointRepository = slaCheckpointRepository;
        this.breakdownRequestRepository = breakdownRequestRepository;
        this.bookingRepository = bookingRepository;
        this.workOrderRepository = workOrderRepository;
    }

    // ---------------------------------------------------------------- helpers

    /**
     * Looks up the workshop assigned to a booking.
     * Requires: {@code bookingId} to exist. May return {@code null} if the booking
     * has not yet been assigned to a workshop (e.g. still pre-booking).
     */
    private Long workshopIdOf(Long bookingId) {
        return bookingRepository.workshopIdByBookingId(bookingId);
    }

    /**
     * Looks up the breakdown request linked to a booking.
     * Requires: {@code bookingId} to exist. May return {@code null} if the booking
     * was not created from a breakdown (e.g. a planned/scheduled maintenance booking).
     */
    private Long breakdownIdOf(Long bookingId) {
        return bookingRepository.breakdownRequestIdByBookingId(bookingId);
    }

    /**
     * Resolves the SLA policy that governs a breakdown: returns the explicitly
     * pinned policy if one was recorded at breakdown-raise time, otherwise falls
     * back to looking up the policy that was effective (by priority + reported date)
     * at the time the breakdown was raised.
     *
     * <p>Requires: a valid {@code breakdownId}. Returns {@code null} (never throws)
     * if the breakdown's priority/reported-time cannot be found, so callers can
     * treat "no policy" as a safe no-op rather than an error.
     */
    private SlaPolicy pinnedPolicyOf(Long breakdownId) {
        SlaPolicy pinned = breakdownRequestRepository.pinnedSlaPolicyById(breakdownId);
        if (pinned != null) {
            return pinned;
        }
        BreakdownPriority priority = breakdownRequestRepository.priorityById(breakdownId);
        OffsetDateTime reportedAt = breakdownRequestRepository.requestraisedtimebyid(breakdownId);
        if (priority == null || reportedAt == null) {
            return null;
        }
        return slaPolicyService.getEffectivePolicy(priority, reportedAt.toLocalDate());
    }

    // Edit 3: the two "which strategy?" helpers — this is what makes it dynamic.

    /**
     * Returns the strategy used for the RESPONSE clock.
     * Always {@code CALENDAR_TIME} because the response phase starts before a
     * booking/workshop exists, so there is no working calendar to consult yet.
     */
    private SlaTimeCalculationStrategy responseStrategy() {
        return strategyResolver.resolve(SlaBasis.CALENDAR_TIME);
    }

    /**
     * Returns the strategy used for the RESOLUTION clock, based on the given
     * policy's {@code calendarBasis}. Falls back to {@code WORKING_TIME} if
     * {@code policy} is {@code null} (no SLA policy in effect).
     *
     * <p>Callers that already hold a fetched {@link SlaPolicy} should use this
     * overload directly instead of re-resolving the policy from a breakdownId,
     * to avoid duplicate DB/service calls.
     */
    private SlaTimeCalculationStrategy resolutionStrategy(SlaPolicy policy) {
        SlaBasis basis = (policy == null) ? SlaBasis.WORKING_TIME : policy.getCalendarBasis();
        return strategyResolver.resolve(basis);
    }

    // --------------------------------------------------------------- breaches

    /**
     * Checks whether the RESPONSE SLA (reportedAt -> work-order startedAt) has been
     * breached for the given booking, and persists the breach flag if so.
     *
     * <p>Requires: the booking to be linked to a breakdown with a valid
     * {@code reportedAt} timestamp and an effective SLA policy. If any of these
     * are missing, this returns {@code false} (treated as "not breached / not
     * applicable") rather than throwing.
     *
     * <p>Behaviour: sticky — once a breach has been recorded, subsequent calls
     * return {@code true} immediately without recomputing.
     *
     * @param bookingId the booking whose response clock is being checked
     * @return {@code true} if the response target has been (or was already) breached
     */
    public boolean isResponseBreach(Long bookingId) {
        if (Boolean.TRUE.equals(slaCheckpointRepository.responseBreachByBookingId(bookingId))) {
            return true; // sticky
        }
        Long breakdownId = breakdownIdOf(bookingId);
        if (breakdownId == null) {
            return false; // preventive: no response target
        }
        Long workshopId = workshopIdOf(bookingId); // may be null pre-booking; unused by CALENDAR_TIME
        OffsetDateTime reportedAt = breakdownRequestRepository.requestraisedtimebyid(breakdownId);
        if (reportedAt == null) {
            return false;
        }
        OffsetDateTime startedAt = workOrderRepository.findStartedAtByBookingId(bookingId);
        OffsetDateTime end = (startedAt != null) ? startedAt : OffsetDateTime.now(clock);

        SlaPolicy policy = pinnedPolicyOf(breakdownId);
        if (policy == null) return false;

        // Edit 4: dynamic — response always CALENDAR_TIME.
        long elapsedMinutes = responseStrategy().calculateElapsedTime(reportedAt, end, workshopId);

        if (elapsedMinutes > policy.getResponseTargetMinutes()) {
            slaCheckpointRepository.updateResponseBreach(bookingId);
            return true;
        }
        return false;
    }

    /**
     * Checks whether the RESOLUTION SLA (reportedAt -> completedAt, minus any
     * AWAITING_PARTS pause time) has been breached for the given booking, and
     * persists the breach flag if so.
     *
     * <p>Requires: the booking to have an assigned workshop (the resolution clock
     * has not started otherwise), a valid breakdown {@code reportedAt}, and an
     * effective SLA policy. Missing any of these returns {@code false}.
     *
     * <p>Behaviour: sticky — once recorded, subsequent calls return {@code true}
     * immediately. Elapsed time is computed using the policy's {@code calendarBasis}
     * strategy and reduced by accumulated awaiting-parts pause time.
     *
     * @param bookingId the booking whose resolution clock is being checked
     * @return {@code true} if the resolution target has been (or was already) breached
     */
    public boolean isResolutionBreach(Long bookingId) {
        if (Boolean.TRUE.equals(slaCheckpointRepository.resolutionBreachByBookingId(bookingId))) {
            return true; // sticky
        }
        Long breakdownId = breakdownIdOf(bookingId);
        if (breakdownId == null) {
            return false; // preventive: no resolution target
        }
        Long workshopId = workshopIdOf(bookingId);
        if (workshopId == null) {
            return false; // no workshop yet -> resolution clock hasn't started
        }
        OffsetDateTime reportedAt = breakdownRequestRepository.requestraisedtimebyid(breakdownId);
        if (reportedAt == null) {
            return false;
        }
        OffsetDateTime completedAt = slaCheckpointRepository.resolvedAtByBookingId(bookingId);
        OffsetDateTime end = (completedAt != null) ? completedAt : OffsetDateTime.now(clock);

        SlaPolicy policy = pinnedPolicyOf(breakdownId);
        if (policy == null) return false;

        // Edit 5: dynamic — resolution uses the policy basis.
        long elapsedMinutes = resolutionStrategy(policy)
                .calculateElapsedTime(reportedAt, end, workshopId);
        long awaitingMinutes = calculateAwaitingTime(bookingId, policy);

        if ((elapsedMinutes - awaitingMinutes) > policy.getResolutionTargetMinutes()) {
            slaCheckpointRepository.updateResolutionBreach(bookingId);
            return true;
        }
        return false;
    }

    // ------------------------------------------------------- awaiting (pauses)

    /**
     * Edit 6: pauses belong to the RESOLUTION clock, so they are counted with the
     * resolution strategy, and now() -> now(clock).
     *
     * <p>Computes the total AWAITING_PARTS pause time accumulated so far for a
     * booking: previously-closed pauses (stored) plus any currently-open pause
     * (computed live up to {@code now(clock)}).
     *
     * <p>Requires: an SLA checkpoint row to already exist for {@code bookingId}.
     *
     * @param bookingId the booking to compute awaiting-parts time for
     * @return total accumulated + open awaiting-parts minutes (never negative)
     * @throws ConflictException if no SLA checkpoint exists for the booking
     */
    public long calculateAwaitingTime(Long bookingId) {
        return calculateAwaitingTime(bookingId, null);
    }

    /**
     * Overload that accepts an already-fetched {@link SlaPolicy} to avoid
     * re-querying it (used by callers that resolved the policy earlier in the
     * same call, e.g. {@link #isResolutionBreach}, {@link #getResolutionRemainingMinutes}).
     * If {@code policy} is {@code null}, it is resolved lazily via the booking's
     * breakdown only when an open pause needs to be measured.
     */
    private long calculateAwaitingTime(Long bookingId, SlaPolicy policy) {
        if (!slaCheckpointRepository.findByBookingId(bookingId).isPresent()) {
            throw new ConflictException("No SLA checkpoint for booking " + bookingId);
        }
        Long accumulated = slaCheckpointRepository.accumulatedAwaitingMinutesByBookingId(bookingId);
        long total = (accumulated != null) ? Math.max(0L, accumulated) : 0L;

        OffsetDateTime openSince = slaCheckpointRepository.lastAwaitingRaisedAtByBookingId(bookingId);
        if (openSince != null) {
            Long workshopId = workshopIdOf(bookingId);
            Long breakdownId = breakdownIdOf(bookingId);
            SlaPolicy effectivePolicy = (policy != null) ? policy : pinnedPolicyOf(breakdownId);
            if (workshopId != null) {
                long open = resolutionStrategy(effectivePolicy)
                        .calculateElapsedTime(openSince, OffsetDateTime.now(clock), workshopId);
                total += Math.max(0L, open);
            }
        }
        return total;
    }


    // raises AWAITING PARTS
    /**
     * Opens an AWAITING_PARTS pause on the resolution clock for a booking.
     *
     * <p>Requires: a non-null {@code awaitingPartsAt} timestamp, an existing SLA
     * checkpoint for the booking, and no pause already open (only one pause window
     * can be open at a time).
     *
     * @param bookingId       the booking to pause
     * @param awaitingPartsAt the timestamp the pause started
     * @throws IllegalArgumentException if {@code awaitingPartsAt} is {@code null}
     * @throws ConflictException        if a pause is already open or no checkpoint exists
     */
    public void raiseAwaiting(Long bookingId, OffsetDateTime awaitingPartsAt) {
        if (awaitingPartsAt == null) {
            throw new IllegalArgumentException("awaitingPartsAt is required");
        }
        int updated = slaCheckpointRepository.raiseAwaiting(bookingId, awaitingPartsAt);
        if (updated == 0) {
            throw new ConflictException(
                    "Cannot raise awaiting-parts for booking " + bookingId
                            + ": a pause is already open, or no SLA checkpoint exists.");
        }
    }

    /**
     * Closes the currently-open AWAITING_PARTS pause for a booking, accumulating
     * its elapsed duration (measured with the policy's resolution strategy) into
     * the checkpoint's stored awaiting-minutes total.
     *
     * <p>Requires: a non-null {@code awaitingPartsResolvedAt} timestamp that is not
     * before the pause's start time, and an already-open pause for the booking.
     * Uses {@code Math.max(0L, ...)} to guard against negative durations from
     * calendar-basis edge cases.
     *
     * @param bookingId              the booking whose pause is being closed
     * @param awaitingPartsResolvedAt the timestamp the pause ended/resumed
     * @throws IllegalArgumentException if {@code awaitingPartsResolvedAt} is {@code null}
     *                                   or precedes the pause's start time
     * @throws ConflictException        if no pause is open, or it was resolved concurrently
     */
    public void resolveAwaiting(Long bookingId, OffsetDateTime awaitingPartsResolvedAt) {
        if (awaitingPartsResolvedAt == null) {
            throw new IllegalArgumentException("awaitingPartsResolvedAt is required");
        }
        OffsetDateTime openSince = slaCheckpointRepository.lastAwaitingRaisedAtByBookingId(bookingId);
        if (openSince == null) {
            throw new ConflictException(
                    "Cannot resolve awaiting-parts for booking " + bookingId + ": no pause is open.");
        }
        if (awaitingPartsResolvedAt.isBefore(openSince)) {
            throw new IllegalArgumentException("Resume time is before the pause was raised");
        }
        Long workshopId = workshopIdOf(bookingId);
        Long breakdownId = breakdownIdOf(bookingId);
        SlaPolicy policy = (breakdownId == null) ? null : pinnedPolicyOf(breakdownId);
        long minutes = (workshopId == null) ? 0L
                : Math.max(0L, resolutionStrategy(policy)
                        .calculateElapsedTime(openSince, awaitingPartsResolvedAt, workshopId));
        int updated = slaCheckpointRepository.resolveAwaitingIfOpen(bookingId, openSince, minutes);
        if (updated == 0) {
            throw new ConflictException(
                    "Pause for booking " + bookingId + " was already resolved concurrently.");
        }
    }

    // -------------------------------------------------------- remaining clocks

    /**
     * Computes the remaining minutes on the RESPONSE clock (target minus elapsed
     * time from reportedAt to work-order startedAt, or now if not yet started).
     *
     * <p>Requires: the booking to be linked to a breakdown with a valid
     * {@code reportedAt} and an effective SLA policy. Returns {@code null} if
     * any of these are missing (i.e. "not applicable" rather than an error).
     * A negative result indicates the response target is already breached.
     *
     * @param bookingId the booking to compute remaining response time for
     * @return remaining response minutes, or {@code null} if not applicable
     */
    public Long getResponseRemainingMinutes(Long bookingId) {
        Long breakdownId = breakdownIdOf(bookingId);
        if (breakdownId == null) return null;
        OffsetDateTime reportedAt = breakdownRequestRepository.requestraisedtimebyid(breakdownId);
        if (reportedAt == null) return null;
        SlaPolicy policy = pinnedPolicyOf(breakdownId);
        if (policy == null) return null;

        Long workshopId = workshopIdOf(bookingId);
        OffsetDateTime startedAt = workOrderRepository.findStartedAtByBookingId(bookingId);
        OffsetDateTime end = (startedAt != null) ? startedAt : OffsetDateTime.now(clock);
        long elapsed = responseStrategy().calculateElapsedTime(reportedAt, end, workshopId);
        return policy.getResponseTargetMinutes() - elapsed;
    }

    /**
     * Computes the remaining minutes on the RESOLUTION clock (target minus
     * elapsed time from reportedAt to completedAt/now, minus accumulated
     * awaiting-parts pause time), measured using the policy's calendar basis.
     *
     * <p>Requires: an assigned workshop (resolution clock must have started), a
     * valid breakdown {@code reportedAt}, and an effective SLA policy. Returns
     * {@code null} if any of these are missing. A negative result indicates the
     * resolution target is already breached.
     *
     * @param bookingId the booking to compute remaining resolution time for
     * @return remaining resolution minutes, or {@code null} if not applicable
     */
    public Long getResolutionRemainingMinutes(Long bookingId) {
        Long breakdownId = breakdownIdOf(bookingId);
        if (breakdownId == null) return null;
        Long workshopId = workshopIdOf(bookingId);
        if (workshopId == null) return null;
        OffsetDateTime reportedAt = breakdownRequestRepository.requestraisedtimebyid(breakdownId);
        if (reportedAt == null) return null;
        SlaPolicy policy = pinnedPolicyOf(breakdownId);
        if (policy == null) return null;

        OffsetDateTime completedAt = slaCheckpointRepository.resolvedAtByBookingId(bookingId);
        OffsetDateTime end = (completedAt != null) ? completedAt : OffsetDateTime.now(clock);
        long elapsed = resolutionStrategy(policy).calculateElapsedTime(reportedAt, end, workshopId);
        long awaiting = calculateAwaitingTime(bookingId, policy);
        return policy.getResolutionTargetMinutes() - (elapsed - awaiting);
    }
}
