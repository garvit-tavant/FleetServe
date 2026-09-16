package com.example.backend.ExecutionService.dto.workorder;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class IssueWorkOrderPartRequest {

    @NotNull(message = "Part ID is required")
    @Positive(message = "Part ID must be greater than zero")
    private Long partId;

    @NotNull(message = "Quantity is required")
    @DecimalMin(
            value = "0.001",
            inclusive = true,
            message = "Quantity must be greater than zero")
    @Digits(
            integer = 9,
            fraction = 3,
            message = "Quantity must contain at most "
                    + "9 integer digits and 3 decimal places")
    private BigDecimal quantity;

    public Long getPartId() {
        return partId;
    }

    public void setPartId(
            Long partId) {
        this.partId = partId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(
            BigDecimal quantity) {
        this.quantity = quantity;
    }
}