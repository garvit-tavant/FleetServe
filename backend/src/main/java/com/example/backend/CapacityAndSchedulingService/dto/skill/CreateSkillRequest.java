package com.example.backend.CapacityAndSchedulingService.dto.skill;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class CreateSkillRequest {

    @NotBlank(message = "Skill code is required")
    @Size(max = 50)
    private String skillCode;

    @NotBlank(message = "Skill name is required")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Skill description is required")
    @Size(max = 255)
    private String description;

    @NotNull(message = "Time is required")
    @Positive(message = "Time must be greater than zero")
    private Long time;

    // getters setters


    public String getSkillCode() {
        return skillCode;
    }

    public void setSkillCode(String skillCode) {
        this.skillCode = skillCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }
}