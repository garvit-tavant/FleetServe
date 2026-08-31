package com.example.backend.AssetManagamentService.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "maintenance_plan")
public class MaintenancePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(name = "distance_interval_km", precision = 12, scale = 3)
    private BigDecimal distanceIntervalKm;

    @Column(name = "time_interval_days")
    private Integer timeIntervalDays;

    @Column(name = "estimated_duration_minutes", nullable = false)
    private Integer estimatedDurationMinutes;

    @Column(name = "required_skill_code", length = 50)
    private String requiredSkillCode;

    @Column(name = "required_capability_code", length = 50)
    private String requiredCapabilityCode;

    public MaintenancePlan() {
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public BigDecimal getDistanceIntervalKm() {
        return distanceIntervalKm;
    }

    public Integer getTimeIntervalDays() {
        return timeIntervalDays;
    }

    public Integer getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public String getRequiredSkillCode() {
        return requiredSkillCode;
    }

    public String getRequiredCapabilityCode() {
        return requiredCapabilityCode;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setDistanceIntervalKm(BigDecimal distanceIntervalKm) {
        this.distanceIntervalKm = distanceIntervalKm;
    }

    public void setTimeIntervalDays(Integer timeIntervalDays) {
        this.timeIntervalDays = timeIntervalDays;
    }

    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }

    public void setRequiredSkillCode(String requiredSkillCode) {
        this.requiredSkillCode = requiredSkillCode;
    }

    public void setRequiredCapabilityCode(String requiredCapabilityCode) {
        this.requiredCapabilityCode = requiredCapabilityCode;
    }
}