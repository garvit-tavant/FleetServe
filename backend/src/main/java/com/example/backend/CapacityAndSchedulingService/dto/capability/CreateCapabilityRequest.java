package com.example.backend.CapacityAndSchedulingService.dto.capability;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateCapabilityRequest {

    @NotBlank(message = "Capability code is required")
    @Size(max = 50)
    private String capabilityCode;

    @NotBlank(message = "Capability name is required")
    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    public CreateCapabilityRequest() {
    }

    public String getCapabilityCode() {
        return capabilityCode;
    }

    public void setCapabilityCode(String capabilityCode) {
        this.capabilityCode = capabilityCode;
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

    public void setDescription(
            String description
    ) {
        this.description = description;
    }

}