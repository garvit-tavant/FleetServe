package com.example.backend.SLA.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

import com.example.backend.SLA.dto.BreakdownPriority;
import com.example.backend.SLA.dto.SlaBasis;

// ans: removed unused jakarta.annotation.Generated import (wrong type, unrelated to JPA)
// added jakarta.persistence.GeneratedValue, GenerationType, and Column which were missing,
// and replaced java.util.Date with java.time.LocalDate for the date fields.



@Entity
@Table(name = "sla_policy")
public class SlaPolicy {
    /*
     id                        BIGINT GENERATED ALWAYS AS IDENTITY,
    priority                  VARCHAR(20) NOT NULL,
    response_target_minutes   INTEGER     NOT NULL,
    resolution_target_minutes INTEGER     NOT NULL,
    calendar_basis            VARCHAR(20) NOT NULL,
    effective_from            DATE        NOT NULL,
    effective_to              DATE,
    */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="priority", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BreakdownPriority priority;

    @Column(name="response_target_minutes", nullable = false)
    private Long responseTargetMinutes;

    @Column(name="resolution_target_minutes", nullable = false)
    private Long resolutionTargetMinutes;

    @Column(name="calendar_basis", nullable = false)
    @Enumerated(EnumType.STRING)
    private SlaBasis calendarBasis;

    @Column(name="effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name="effective_to")
    private LocalDate effectiveTo;

    // getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
