package com.example.backend.ExecutionService.dto.booking;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/**
 * Books the earliest feasible slot for a breakdown that has been triaged.
 *
 * <p>The required skill, required capability and estimated duration are read
 * from the breakdown itself rather than supplied here, so the job is scheduled
 * against what triage recorded rather than against whoever fills in this form.
 *
 * <p>The workshop must belong to the depot that reported the breakdown.
 */
public record BookBreakdownSlotRequest(
        @NotNull(message = "workshopId is required")
        @Positive(message = "workshopId must be positive")
        Long workshopId,

        /** Optional. Defaults to today in the workshop's own time zone. */
        LocalDate searchFrom) {
}
