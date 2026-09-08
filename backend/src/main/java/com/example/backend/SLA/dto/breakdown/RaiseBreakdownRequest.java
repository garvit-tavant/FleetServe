package com.example.backend.SLA.dto.breakdown;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import com.example.backend.SLA.status.BreakdownPriority;

/**
 * Raises a breakdown (US-4.1).
 *
 * <p>The depot is not supplied: it is taken from the asset's home depot, so a
 * breakdown cannot be filed against a depot that has nothing to do with the
 * vehicle. The reporter comes from the authenticated user, not the body.
 *
 * <p>The triage fields are optional at intake. When a required skill is given
 * and no duration is, the skill's standard time is used.
 */
public record RaiseBreakdownRequest(

        @NotNull(message = "assetId is required")
        @Positive(message = "assetId must be positive")
        Long assetId,

        @NotNull(message = "priority is required; one of P1, P2, P3")
        BreakdownPriority priority,

        @NotBlank(message = "description is required")
        @Size(max = 2000, message = "description may not exceed 2000 characters")
        String description,

        /** Optional at intake; triage may add it later. */
        String requiredSkillCode,

        /** Optional at intake; triage may add it later. */
        String requiredCapabilityCode,

        /** Optional. Defaults to the required skill's standard time. */
        @Positive(message = "estimatedDurationMinutes must be positive")
        @Max(value = 1440, message = "a single booking may not exceed 24 hours")
        Integer estimatedDurationMinutes) {
}
