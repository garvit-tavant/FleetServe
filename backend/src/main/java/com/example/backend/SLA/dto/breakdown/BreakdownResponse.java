package com.example.backend.SLA.dto.breakdown;

import java.time.OffsetDateTime;

import com.example.backend.SLA.status.BreakdownPriority;
import com.example.backend.SLA.status.BreakdownStatus;

/**
 * A breakdown request as returned by the API.
 *
 * <p>{@code slaPolicyId} is the policy version pinned at intake, so the targets
 * that applied when the breakdown was raised stay visible even if the policy is
 * revised later.
 *
 * <p>The linked booking is deliberately not exposed here. It is a lazy
 * one-to-one that Hibernate cannot proxy, so touching it while mapping a page of
 * results would issue one extra query per row. Bookings are discoverable through
 * the booking API instead.
 */
public record BreakdownResponse(
        Long id,
        Long assetId,
        String assetVin,
        Long depotId,
        Long reportedById,
        OffsetDateTime reportedAt,
        BreakdownPriority priority,
        String description,
        BreakdownStatus status,
        Long slaPolicyId,
        Integer responseTargetMinutes,
        Integer resolutionTargetMinutes,
        String requiredSkillCode,
        String requiredCapabilityCode,
        Integer estimatedDurationMinutes) {
}
