package com.example.backend.AssetManagamentService.dto.maintenanceplan;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class CreateMaintenancePlanRequest {

    @NotBlank(message = "Maintenance-plan code is required")
    @Size(
    max = 50,
    message = "Maintenance-plan code cannot exceed 50 characters"
            )
    private String code;
    @DecimalMin(
    value = "0.001",
    inclusive = true,
    message = "Distance interval must be greater than zero"
            )
    private BigDecimal distanceIntervalKm;
    @Positive(
    message = "Time interval must be greater than zero"
            )
    private Integer timeIntervalDays;
    @NotNull(
    message = "Estimated duration is required"
            )
    @Positive(
    message = "Estimated duration must be greater than zero"
            )
    private Integer estimatedDurationMinutes;
    @Size(
    max = 50,
    message = "Required skill code cannot exceed 50 characters"
            )
    private String requiredSkillCode;
    @Size(
    max = 50,
    message = "Required capability code cannot exceed 50 characters"
            )
    private String requiredCapabilityCode;

    // getters setters

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public BigDecimal getDistanceIntervalKm() {
        return distanceIntervalKm;
    }

    public void setDistanceIntervalKm(BigDecimal distanceIntervalKm) {
        this.distanceIntervalKm = distanceIntervalKm;
    }

    public Integer getTimeIntervalDays() {
        return timeIntervalDays;
    }

    public void setTimeIntervalDays(Integer timeIntervalDays) {
        this.timeIntervalDays = timeIntervalDays;
    }

    public Integer getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }

    public String getRequiredSkillCode() {
        return requiredSkillCode;
    }

    public void setRequiredSkillCode(String requiredSkillCode) {
        this.requiredSkillCode = requiredSkillCode;
    }

    public String getRequiredCapabilityCode() {
        return requiredCapabilityCode;
    }

    public void setRequiredCapabilityCode(String requiredCapabilityCode) {
        this.requiredCapabilityCode = requiredCapabilityCode;
    }
}