package com.example.backend.CapacityAndSchedulingService.dto.workingcalendar;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public class UpdateWorkingCalendarRequest {

    @NotNull(message = "Open time is required")
    private LocalTime openTime;

    @NotNull(message = "Close time is required")
    private LocalTime closeTime;

    // getters setters

    public LocalTime getOpenTime() {
        return openTime;
    }

    public void setOpenTime(LocalTime openTime) {
        this.openTime = openTime;
    }

    public LocalTime getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(LocalTime closeTime) {
        this.closeTime = closeTime;
    }
}