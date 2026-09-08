package com.example.backend.SLA.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.AssetManagamentService.exception.ResourceNotFoundException;
import com.example.backend.SLA.calendar.PauseInterval;
import com.example.backend.SLA.calendar.WorkshopCalendar;
import com.example.backend.SLA.calendar.WorkshopCalendarProvider;
import com.example.backend.SLA.dto.SlaClockSnapshot;
import com.example.backend.SLA.dto.SlaClockStatus;
import com.example.backend.SLA.dto.SlaEvaluationRow;
import com.example.backend.SLA.entity.AwaitingRaised;
import com.example.backend.SLA.repository.AwaitingRaisedRepository;
import com.example.backend.SLA.repository.BreakdownRequestRepository;
import com.example.backend.SLA.repository.SlaCheckpointRepository;

/**
 * Evaluates the response and resolution clocks for a breakdown.
 *
 * <p>Both clocks start at {@code reported_at}. The schema constrains
 * {@code resolution_target_minutes >= response_target_minutes}, which only makes
 * sense if the two targets share an origin.
 *
 * <p>A clock stops at its recorded timestamp and keeps running against the
 * injected clock until then. A breach recorded late is still a breach: the
 * comparison is always target against consumed minutes, never merely "was a
 * timestamp written".
 */
@Service
@Transactional(readOnly = true)
public class SlaClockService {

    private final Clock clock;
    private final SlaCalculatorRegistry calculatorRegistry;
    private final WorkshopCalendarProvider calendarProvider;
    private final BreakdownRequestRepository breakdownRequestRepository;
    private final AwaitingRaisedRepository awaitingRaisedRepository;
    private final SlaCheckpointRepository slaCheckpointRepository;
    private final double atRiskFraction;

    public SlaClockService(
            Clock clock,
            SlaCalculatorRegistry calculatorRegistry,
            WorkshopCalendarProvider calendarProvider,
            BreakdownRequestRepository breakdownRequestRepository,
            AwaitingRaisedRepository awaitingRaisedRepository,
            SlaCheckpointRepository slaCheckpointRepository,
            @Value("${fleetserve.sla.at-risk-fraction:0.2}") double atRiskFraction) {
        this.clock = clock;
        this.calculatorRegistry = calculatorRegistry;
        this.calendarProvider = calendarProvider;
        this.breakdownRequestRepository = breakdownRequestRepository;
        this.awaitingRaisedRepository = awaitingRaisedRepository;
        this.slaCheckpointRepository = slaCheckpointRepository;
        this.atRiskFraction = atRiskFraction;
    }

    public SlaClockSnapshot evaluate(long breakdownId) {
        SlaEvaluationRow row = breakdownRequestRepository.findForEvaluation(breakdownId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Breakdown request not found: " + breakdownId));

        return evaluate(row, toPauses(awaitingRaisedRepository.findByBreakdownRequestId(breakdownId)));
    }

    /** Evaluates every open breakdown and persists any newly detected breach. */
    @Transactional
    public List<SlaClockSnapshot> refreshOpenBreakdowns() {
        List<SlaEvaluationRow> rows = breakdownRequestRepository.findOpenForEvaluation();
        if (rows.isEmpty()) {
            return List.of();
        }

        List<Long> ids = rows.stream().map(SlaEvaluationRow::breakdownId).toList();
        Map<Long, List<AwaitingRaised>> pausesByBreakdown =
                awaitingRaisedRepository.findByBreakdownRequestIds(ids).stream()
                        .collect(Collectors.groupingBy(a -> a.getBreakdownRequest().getId()));

        List<SlaClockSnapshot> snapshots = new ArrayList<>(rows.size());
        for (SlaEvaluationRow row : rows) {
            SlaClockSnapshot snapshot = evaluate(
                    row, toPauses(pausesByBreakdown.getOrDefault(row.breakdownId(), List.of())));
            snapshots.add(snapshot);
            persistBreachFlags(row, snapshot);
        }
        return snapshots;
    }

    private void persistBreachFlags(SlaEvaluationRow row, SlaClockSnapshot snapshot) {
        if (!Boolean.valueOf(snapshot.responseBreached()).equals(row.responseBreach())) {
            slaCheckpointRepository.updateResponseBreach(row.breakdownId(), snapshot.responseBreached());
        }
        if (!Boolean.valueOf(snapshot.resolutionBreached()).equals(row.resolutionBreach())) {
            slaCheckpointRepository.updateResolutionBreach(row.breakdownId(), snapshot.resolutionBreached());
        }
    }

    private SlaClockSnapshot evaluate(SlaEvaluationRow row, Collection<PauseInterval> pauses) {
        Instant reportedAt = toInstant(row.reportedAt());
        Instant now = clock.instant();

        Instant responseEnd = row.respondedAt() != null ? toInstant(row.respondedAt()) : now;
        Instant resolutionEnd = row.resolvedAt() != null ? toInstant(row.resolvedAt()) : now;
        Instant latest = resolutionEnd.isAfter(responseEnd) ? resolutionEnd : responseEnd;

        // Without a booking there is no workshop and therefore no calendar, so
        // fall back to continuous measurement rather than silently measuring zero.
        boolean hasWorkshop = row.workshopId() != null;
        SlaCalculator calculator = hasWorkshop
                ? calculatorRegistry.forBasis(row.calendarBasis())
                : calculatorRegistry.elapsedFallback();
        WorkshopCalendar calendar = hasWorkshop
                ? calendarProvider.load(row.workshopId(), localDate(reportedAt), localDate(latest))
                : calendarProvider.continuousCalendar();

        long responseElapsed = calculator.elapsedMinutes(reportedAt, responseEnd, calendar, pauses);
        long resolutionElapsed = calculator.elapsedMinutes(reportedAt, resolutionEnd, calendar, pauses);

        int responseTarget = row.responseTargetMinutes() == null ? 0 : row.responseTargetMinutes();
        int resolutionTarget = row.resolutionTargetMinutes() == null ? 0 : row.resolutionTargetMinutes();

        long responseRemaining = responseTarget - responseElapsed;
        long resolutionRemaining = resolutionTarget - resolutionElapsed;

        long pausedMinutes = com.example.backend.SLA.calendar.WorkingCalendarArithmetic
                .clipAndMergePauses(pauses, reportedAt, latest).stream()
                .mapToLong(i -> java.time.Duration.between(i.start(), i.end()).toMinutes())
                .sum();

        return new SlaClockSnapshot(
                row.breakdownId(),
                row.priority(),
                calculator.basis(),
                responseElapsed,
                responseTarget,
                responseRemaining,
                classify(responseRemaining, responseTarget),
                row.respondedAt() != null,
                resolutionElapsed,
                resolutionTarget,
                resolutionRemaining,
                classify(resolutionRemaining, resolutionTarget),
                row.resolvedAt() != null,
                pausedMinutes);
    }

    /**
     * OK, AT_RISK or BREACHED. AT_RISK is a fraction of the target rather than a
     * fixed number of minutes, so a P1 with a one-hour target and a P3 with a
     * one-day target both get a proportionate warning window.
     */
    SlaClockStatus classify(long remainingMinutes, int targetMinutes) {
        if (remainingMinutes < 0) {
            return SlaClockStatus.BREACHED;
        }
        if (targetMinutes > 0 && remainingMinutes <= Math.round(targetMinutes * atRiskFraction)) {
            return SlaClockStatus.AT_RISK;
        }
        return SlaClockStatus.OK;
    }

    private static List<PauseInterval> toPauses(Collection<AwaitingRaised> rows) {
        List<PauseInterval> pauses = new ArrayList<>(rows.size());
        for (AwaitingRaised row : rows) {
            pauses.add(new PauseInterval(
                    row.getPauseReason(),
                    toInstant(row.getRaisedAt()),
                    toInstant(row.getResolvedAt())));
        }
        return pauses;
    }

    private LocalDate localDate(Instant instant) {
        return instant.atZone(clock.getZone()).toLocalDate();
    }

    private static Instant toInstant(OffsetDateTime value) {
        return value == null ? null : value.toInstant();
    }
}
