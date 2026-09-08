package com.example.backend.SLA.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

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

    @Column(name="priority", nullable = false)
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    private com.example.backend.SLA.status.BreakdownPriority priority;

    @Column(name="response_target_minutes", nullable = false)
    private Integer responseTargetMinutes;

    @Column(name="resolution_target_minutes", nullable = false)
    private Integer resolutionTargetMinutes;

    @Column(name="calendar_basis", nullable = false)
    private String calendarBasis;

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

    public com.example.backend.SLA.status.BreakdownPriority getPriority() {
        return priority;
    }

    public void setPriority(com.example.backend.SLA.status.BreakdownPriority priority) {
        this.priority = priority;
    }

    public Integer getResponseTargetMinutes() {
        return responseTargetMinutes;
    }

    public void setResponseTargetMinutes(Integer responseTargetMinutes) {
        this.responseTargetMinutes = responseTargetMinutes;
    }

    public Integer getResolutionTargetMinutes() {
        return resolutionTargetMinutes;
    }

    public void setResolutionTargetMinutes(Integer resolutionTargetMinutes) {
        this.resolutionTargetMinutes = resolutionTargetMinutes;
    }

    public String getCalendarBasis() {
        return calendarBasis;
    }

    public void setCalendarBasis(String calendarBasis) {
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
