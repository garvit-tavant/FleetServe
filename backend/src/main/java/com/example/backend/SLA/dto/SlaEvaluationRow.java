package com.example.backend.SLA.dto;

import java.time.OffsetDateTime;

/**
 * Everything needed to evaluate both service-level clocks for one breakdown,
 * fetched in a single query so a sweep over open breakdowns does not issue a
 * cascade of per-row lookups.
 *
 * <p>{@code workshopId} is null until the breakdown has been booked, because the
 * workshop is only known once a booking exists.
 */
public record SlaEvaluationRow(
        Long breakdownId,
        com.example.backend.SLA.status.BreakdownPriority priority,
        OffsetDateTime reportedAt,
        Long workshopId,
        Integer responseTargetMinutes,
        Integer resolutionTargetMinutes,
        String calendarBasis,
        OffsetDateTime respondedAt,
        OffsetDateTime resolvedAt,
        Boolean responseBreach,
        Boolean resolutionBreach) {
}
