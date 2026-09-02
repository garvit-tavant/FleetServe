package com.example.backend.CapacityAndSchedulingService.dto.technician;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class CreateTechnicianRequest {

    @NotNull(message = "App user id is required")
    @Positive(message = "App user id must be positive")
    private Long appUserId;

    @NotNull(message = "Workshop id is required")
    @Positive(message = "Workshop id must be positive")
    private Long workshopId;

    @NotNull(message = "Hourly rate is required")
    private BigDecimal hourlyRate;

    // getters setters


    public Long getAppUserId() {
        return appUserId;
    }

    public void setAppUserId(Long appUserId) {
        this.appUserId = appUserId;
    }

    public Long getWorkshopId() {
        return workshopId;
    }

    public void setWorkshopId(Long workshopId) {
        this.workshopId = workshopId;
    }

    public BigDecimal getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(BigDecimal hourlyRate) {
        this.hourlyRate = hourlyRate;
    }
}