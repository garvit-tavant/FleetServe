package com.example.backend.CapacityAndSchedulingService.dto.bayCapability;

public class BayCapabilityResponse {

    private Long id;
    private Long bayId;
    private String capabilityCode;
    private String capabilityName;

    public BayCapabilityResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBayId() {
        return bayId;
    }

    public void setBayId(Long bayId) {
        this.bayId = bayId;
    }

    public String getCapabilityCode() {
        return capabilityCode;
    }

    public void setCapabilityCode(
            String capabilityCode
    ) {
        this.capabilityCode = capabilityCode;
    }

    public String getCapabilityName() {
        return capabilityName;
    }

    public void setCapabilityName(
            String capabilityName
    ) {
        this.capabilityName = capabilityName;
    }
}