package com.example.backend.AssetManagamentService.dto.assetclass;

import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * The DUE_SOON warning windows for one asset-class-and-plan pairing.
 *
 * <p>A null field means no amber warning on that axis; the asset goes straight
 * from OK to OVERDUE. Each window must be shorter than the interval it
 * qualifies, which the service checks and a database trigger enforces.
 */
public record DueSoonWindowRequest(
        @Positive(message = "Distance warning window must be positive")
        BigDecimal dueSoonDistanceKm,

        @Positive(message = "Day warning window must be positive")
        Integer dueSoonDays) {
}
