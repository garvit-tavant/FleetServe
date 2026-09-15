package com.example.backend.CapacityAndSchedulingService.dto.depot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateDepotRequest {

    @NotBlank(message = "Depot code is required")
    @Size(max = 30)
    private String code;

    @NotBlank(message = "Region is required")
    @Size(max = 100)
    private String region;

    public CreateDepotRequest() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}