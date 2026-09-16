package com.example.backend.ExecutionService.dto.workorderlabour;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class WorkOrderLabourCreateRequest {

    @NotNull
    @Positive
    private Long technicianId;

    @NotNull
    @Positive
    private BigDecimal hours;

    @NotNull
    @Positive
    private BigDecimal rateApplied;

    // getters setters


    public Long getTechnicianId() {
        return technicianId;
    }

    public void setTechnicianId(Long technicianId) {
        this.technicianId = technicianId;
    }

    public BigDecimal getHours() {
        return hours;
    }

    public void setHours(BigDecimal hours) {
        this.hours = hours;
    }

    public BigDecimal getRateApplied() {
        return rateApplied;
    }

    public void setRateApplied(BigDecimal rateApplied) {
        this.rateApplied = rateApplied;
    }
}