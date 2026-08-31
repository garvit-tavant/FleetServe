package com.example.backend.AssetManagamentService.dto.maintenanceplan;

import java.math.BigDecimal;

public class MaintenancePlanResponse {

    private Long id;

    private String code;

    private BigDecimal distanceIntervalKm;

    private Integer timeIntervalDays;

    private Integer estimatedDurationMinutes;

    private String requiredSkillCode;

    private String requiredCapabilityCode;

    // getters setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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