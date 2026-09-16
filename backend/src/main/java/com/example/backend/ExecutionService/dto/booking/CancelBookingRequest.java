package com.example.backend.ExecutionService.dto.booking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CancelBookingRequest {

    @NotBlank(message = "Cancellation reason is required")
    @Size(
            max = 1000,
            message = "Cancellation reason cannot exceed 1000 characters")
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(
            String reason) {
        this.reason = reason;
    }
}