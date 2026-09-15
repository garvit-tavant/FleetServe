package com.example.backend.CapacityAndSchedulingService.dto.technicianskill;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class AssignSkillRequest {

    @NotBlank(message = "Skill code is required")
    private String skillCode;

    @NotNull(message = "Valid from date is required")
    private LocalDate validFrom;

    private LocalDate validTo;

    // getters setters


    public String getSkillCode() {
        return skillCode;
    }

    public void setSkillCode(String skillCode) {
        this.skillCode = skillCode;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDate validTo) {
        this.validTo = validTo;
    }
}