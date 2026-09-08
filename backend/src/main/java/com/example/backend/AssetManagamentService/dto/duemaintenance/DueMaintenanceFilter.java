package com.example.backend.AssetManagamentService.dto.duemaintenance;

import com.example.backend.AssetManagamentService.status.DueStatus;

import java.math.BigDecimal;

/**
 * Filters for the preventive maintenance due list (US-1.3).
 *
 * <p>The two threshold fields are optional overrides. When null, each plan's own
 * stored {@code due_soon_distance_km} and {@code due_soon_days} apply, which is
 * the normal case; supplying them lets a planner ask "what would next month look
 * like if I widened the warning window".
 */
public record DueMaintenanceFilter(
        BigDecimal distanceSoonThresholdOverride,
        Integer timeSoonThresholdOverride,
        Long depotId,
        String assetClassCode,
        DueStatus status,
        boolean dueOnly) {

    /** Default view: due and overdue rows only, using each plan's stored thresholds. */
    public static DueMaintenanceFilter defaults() {
        return new DueMaintenanceFilter(null, null, null, null, null, true);
    }
}
