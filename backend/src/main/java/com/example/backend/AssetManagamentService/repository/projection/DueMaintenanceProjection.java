package com.example.backend.AssetManagamentService.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DueMaintenanceProjection {

    Long getAssetId();

    String getVin();

    String getAssetClassCode();

    String getMaintenancePlanCode();

    LocalDate getNextDueDate();

    BigDecimal getNextDueKm();

    BigDecimal getCurrentOdometerKm();

    /** Negative once the distance threshold has been passed. Null for time-only plans. */
    BigDecimal getKmRemaining();

    /** Negative once the time threshold has been passed. Null for distance-only plans. */
    Integer getDaysRemaining();

    String getDueStatus();
}
