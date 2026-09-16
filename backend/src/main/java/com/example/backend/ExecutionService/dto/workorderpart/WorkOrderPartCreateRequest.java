package com.example.backend.ExecutionService.dto.workorderpart;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class WorkOrderPartCreateRequest {

    @NotNull
    @Positive
    private Long partId;

    @NotNull
    @Positive
    private BigDecimal quantity;

    @NotNull
    @Positive
    private Long movementId;

    // getters setters


    public Long getPartId() {
        return partId;
    }

    public void setPartId(Long partId) {
        this.partId = partId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public Long getMovementId() {
        return movementId;
    }

    public void setMovementId(Long movementId) {
        this.movementId = movementId;
    }
}