package com.example.backend.ExecutionService.dto.workorder;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class StartWorkOrderRequest {

    @NotNull
    @Positive
    private Long odometerAtService;

    // getters setters


    public Long getOdometerAtService() {
        return odometerAtService;
    }

    public void setOdometerAtService(Long odometerAtService) {
        this.odometerAtService = odometerAtService;
    }
}