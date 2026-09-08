package com.example.backend.ExecutionService.dto.booking;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/**
 * Books the earliest feasible slot for a maintenance plan that is due on an
 * asset.
 *
 * <p>Duration, required skill and required capability are taken from the plan,
 * not from the caller, so scheduling follows policy rather than whoever happens
 * to be filling in the form.
 */
public record BookPreventiveSlotRequest(
        @NotNull(message = "assetId is required")
        @Positive(message = "assetId must be positive")
        Long assetId,

        @NotNull(message = "maintenancePlanId is required")
        @Positive(message = "maintenancePlanId must be positive")
        Long maintenancePlanId,

        @NotNull(message = "workshopId is required")
        @Positive(message = "workshopId must be positive")
        Long workshopId,

        /** Optional. Defaults to today in the workshop's own time zone. */
        LocalDate searchFrom) {
}
