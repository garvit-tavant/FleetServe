package com.example.backend.AssetManagamentService.dto.odometer;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class AddOdometerReadingRequest {
    @NotNull(message = "Odometer reading is required")
    @DecimalMin(
    value = "0.000",
    inclusive = true,
    message = "Odometer reading cannot be negative"
            )
    private BigDecimal readingKm;

    // getters setters

    public BigDecimal getReadingKm() {
        return readingKm;
    }

    public void setReadingKm(BigDecimal readingKm) {
        this.readingKm = readingKm;
    }
}