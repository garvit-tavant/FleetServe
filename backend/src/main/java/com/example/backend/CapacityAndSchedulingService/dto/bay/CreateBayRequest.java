package com.example.backend.CapacityAndSchedulingService.dto.bay;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateBayRequest {

    @NotBlank
    @Size(max = 30)
    private String bayCode;

    public CreateBayRequest() {
    }

    public String getBayCode() {
        return bayCode;
    }

    public void setBayCode(String bayCode) {
        this.bayCode = bayCode;
    }
}