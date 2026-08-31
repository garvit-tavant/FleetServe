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

    String getDueStatus();
}