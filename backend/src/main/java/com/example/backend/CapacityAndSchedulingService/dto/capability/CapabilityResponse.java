package com.example.backend.CapacityAndSchedulingService.dto.capability;

public class CapabilityResponse {

    private String capabilityCode;
    private String name;
    private String description;

    public CapabilityResponse() {
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
