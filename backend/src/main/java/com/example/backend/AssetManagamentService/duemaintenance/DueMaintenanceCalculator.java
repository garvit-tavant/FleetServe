package com.example.backend.AssetManagamentService.duemaintenance;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class DueMaintenanceCalculator {

    private DueMaintenanceCalculator() {
    }

    public static BigDecimal nextDueKm(
            BigDecimal baselineKm,
            BigDecimal intervalKm
    ) {
        if (intervalKm == null) {
            return null;
        }

        return baselineKm.add(intervalKm);
    }

    public static LocalDate nextDueDate(
            LocalDate baselineDate,
            Integer intervalDays
    ) {
        if (intervalDays == null) {
            return null;
        }

        return baselineDate.plusDays(intervalDays);
    }

}