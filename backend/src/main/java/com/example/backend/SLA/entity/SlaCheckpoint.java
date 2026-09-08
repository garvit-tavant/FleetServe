package com.example.backend.SLA.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

import com.example.backend.ExecutionService.entity.Booking;

@Entity
@Table(name = "sla_checkpoint")
public class SlaCheckpoint {

    /*
    booking_id          BIGINT       NOT NULL,
    responded_at        TIMESTAMPTZ  DEFAULT NULL,
    resolved_at         TIMESTAMPTZ  DEFAULT NULL,
    response_breach     BOOLEAN DEFAULT FALSE,
    resolution_breach    BOOLEAN DEFAULT FALSE,
    accumulated_awaiting_minutes BIGINT NOT NULL DEFAULT 0,
    last_awaiting_raised_at TIMESTAMPTZ DEFAULT NULL
     */

    // Keyed on Booking, not BreakdownRequest: preventive jobs (driven by a
    // maintenance plan) have no BreakdownRequest but still need clock
    // tracking. The BreakdownRequest, where one exists, is reached via
    // booking.getBreakdownRequest().
   
    @Id
    @Column(name = "booking_id")
    private Long bookingId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Column(name = "responded_at")
    private OffsetDateTime respondedAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    // Breach flags only ever apply to corrective jobs, since SLA targets live
    // on breakdown_request.sla_policy_id. For preventive jobs they stay FALSE.
    @Column(name = "response_breach")
    private Boolean responseBreach = Boolean.FALSE;  //

    @Column(name = "resolution_breach")
    private Boolean resolutionBreach = Boolean.FALSE;

    // Precomputed running total of all *closed* awaiting-parts/approval pauses,
    // in minutes (per whatever calendar basis was applied at close time).
    // Replaces summing the now-removed awaiting_raised history table on every
    // SLA check.
    @Column(name = "accumulated_awaiting_minutes", nullable = false)
    private Long accumulatedAwaitingMinutes = 0L;

    // Start time of the currently open awaiting pause, or null if the job is
    // not currently paused. Only one pause can be open at a time (raising
    // again while already open throws; see SlaCalculator.raiseAwaiting).
    @Column(name = "last_awaiting_raised_at")
    private OffsetDateTime lastAwaitingRaisedAt;

    // getters and setters

   
    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public OffsetDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(OffsetDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }

    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(OffsetDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public Boolean getResponseBreach() {
        return responseBreach;
    }

    public void setResponseBreach(Boolean responseBreach) {
        this.responseBreach = responseBreach;
    }

    public Boolean getResolutionBreach() {
        return resolutionBreach;
    }

    public void setResolutionBreach(Boolean resolutionBreach) {
        this.resolutionBreach = resolutionBreach;
    }

    public Long getAccumulatedAwaitingMinutes() {
        return accumulatedAwaitingMinutes;
    }

    public void setAccumulatedAwaitingMinutes(Long accumulatedAwaitingMinutes) {
        this.accumulatedAwaitingMinutes = accumulatedAwaitingMinutes;
    }

    public OffsetDateTime getLastAwaitingRaisedAt() {
        return lastAwaitingRaisedAt;
    }

    public void setLastAwaitingRaisedAt(OffsetDateTime lastAwaitingRaisedAt) {
        this.lastAwaitingRaisedAt = lastAwaitingRaisedAt;
    }
}
