package com.example.backend.ExecutionService.dto.workorder;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class WorkOrderCreateRequest {

    @NotNull
    @Positive
    private Long bookingId;

    // getters setters


    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }
}