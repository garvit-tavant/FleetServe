package com.example.backend.ExecutionService.dto.workorder;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class CompleteWorkOrderRequest {

    @NotNull(message = "Odometer reading at service is required")
    @DecimalMin(
            value = "0.000",
            inclusive = true,
            message = "Odometer reading cannot be negative")
    @Digits(
            integer = 9,
            fraction = 3,
            message = "Odometer reading must contain at most "
                    + "9 integer digits and 3 decimal places")
    private BigDecimal odometerAtService;

    @NotNull(message = "Hours worked is required")
    @Positive(message = "Hours worked must be greater than zero")
    @Digits(
            integer = 3,
            fraction = 2,
            message = "Hours worked must contain at most "
                    + "3 integer digits and 2 decimal places")
    private BigDecimal hoursWorked;

    public BigDecimal getOdometerAtService() {
        return odometerAtService;
    }

    public void setOdometerAtService(
            BigDecimal odometerAtService) {
        this.odometerAtService = odometerAtService;
    }

    public BigDecimal getHoursWorked() {
        return hoursWorked;
    }

    public void setHoursWorked(
            BigDecimal hoursWorked) {
        this.hoursWorked = hoursWorked;
    }
}