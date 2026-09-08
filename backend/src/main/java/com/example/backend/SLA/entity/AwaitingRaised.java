package com.example.backend.SLA.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "awaiting_raised")
public class AwaitingRaised {

    /*
     id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    breakdown_request_id BIGINT       NOT NULL,
    raised_at         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at         TIMESTAMPTZ  DEFAULT NULL,
     */



    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK resolved as an association: many awaiting periods per breakdown
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "breakdown_request_id", nullable = false)
    private BreakdownRequest breakdownRequest;   // it should be one breakdown request to many awaiting periods

    @Column(name = "raised_at", nullable = false)
    private OffsetDateTime raisedAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt = null; // default is null according to db migration script

    /*
     * AWAITING_PARTS or DEPOT_UNREACHABLE. Both suspend the service-level clock
     * identically; the reason is kept so the two causes stay separable in
     * reporting. See docs/open-questions.md entry 12.
     */
    @Column(name = "pause_reason", nullable = false, length = 30)
    private String pauseReason;

    //getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BreakdownRequest getBreakdownRequest() {
        return breakdownRequest;
    }

    public void setBreakdownRequest(BreakdownRequest breakdownRequest) {
        this.breakdownRequest = breakdownRequest;
    }

    public OffsetDateTime getRaisedAt() {
        return raisedAt;
    }

    public void setRaisedAt(OffsetDateTime raisedAt) {
        this.raisedAt = raisedAt;
    }

    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(OffsetDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getPauseReason() {
        return pauseReason;
    }

    public void setPauseReason(String pauseReason) {
        this.pauseReason = pauseReason;
    }
}
