package com.example.backend.ExecutionService.dto.booking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class CreateCorrectiveBookingRequest {

    @NotBlank
    private String requiredSkillCode;

    @NotBlank
    private String requiredCapabilityCode;

    @NotNull
    @Positive
    private Integer estimatedDurationMinutes;

    public String getRequiredSkillCode() {
        return requiredSkillCode;
    }

    public void setRequiredSkillCode(
            String requiredSkillCode) {
        this.requiredSkillCode = requiredSkillCode;
    }

    public String getRequiredCapabilityCode() {
        return requiredCapabilityCode;
    }

    public void setRequiredCapabilityCode(
            String requiredCapabilityCode) {
        this.requiredCapabilityCode = requiredCapabilityCode;
    }

    public Integer getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public void setEstimatedDurationMinutes(
            Integer estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }
}