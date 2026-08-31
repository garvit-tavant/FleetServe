package com.example.backend.AssetManagamentService.dto.asset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ReinstateAssetRequest {

    @NotBlank(message = "Reinstatement reason is required")
    @Size(
    max = 500,
    message = "Reinstatement reason cannot exceed 500 characters"
            )
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
