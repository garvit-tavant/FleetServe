package com.example.backend.ExecutionService.dto.workorder;

import java.math.BigDecimal;

public class CreatePartRequirementRequest {

    private Long partId;

    private BigDecimal quantityRequired;

    private String reason;

    public Long getPartId() {
        return partId;
    }

    public void setPartId(
            Long partId
    ) {
        this.partId = partId;
    }

    public BigDecimal getQuantityRequired() {
        return quantityRequired;
    }

    public void setQuantityRequired(
            BigDecimal quantityRequired
    ) {
        this.quantityRequired = quantityRequired;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(
            String reason
    ) {
        this.reason = reason;
    }
}