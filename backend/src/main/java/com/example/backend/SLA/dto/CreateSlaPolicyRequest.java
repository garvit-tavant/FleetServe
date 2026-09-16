package com.example.backend.SLA.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public class CreateSlaPolicyRequest {

    @NotNull
    private BreakdownPriority priority;

    @NotNull
    @Positive
    private Long responseTargetMinutes;

    @NotNull
    @Positive
    private Long resolutionTargetMinutes;

    @NotNull
    private SlaBasis calendarBasis;

    @NotNull
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    public BreakdownPriority getPriority() {
        return priority;
    }

    public void setPriority(BreakdownPriority priority) {
        this.priority = priority;
    }

    public Long getResponseTargetMinutes() {
        return responseTargetMinutes;
    }

    public void setResponseTargetMinutes(Long responseTargetMinutes) {
        this.responseTargetMinutes = responseTargetMinutes;
    }

    public Long getResolutionTargetMinutes() {
        return resolutionTargetMinutes;
    }

    public void setResolutionTargetMinutes(Long resolutionTargetMinutes) {
        this.resolutionTargetMinutes = resolutionTargetMinutes;
    }

    public SlaBasis getCalendarBasis() {
        return calendarBasis;
    }

    public void setCalendarBasis(SlaBasis calendarBasis) {
        this.calendarBasis = calendarBasis;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(LocalDate effectiveTo) {
        this.effectiveTo = effectiveTo;
    }
}