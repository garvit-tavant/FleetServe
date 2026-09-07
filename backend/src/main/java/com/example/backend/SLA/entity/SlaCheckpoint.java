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

@Entity
@Table(name = "sla_checkpoint")
public class SlaCheckpoint {

    /*
    breakdown_request_id BIGINT       NOT NULL,
    responded_at        TIMESTAMPTZ  DEFAULT NULL,
    resolved_at         TIMESTAMPTZ  DEFAULT NULL,
    response_breach     BOOLEAN DEFAULT FALSE,
    resolution_breach    BOOLEAN DEFAULT FALSE,
     */


    // PK is also the FK to breakdown_request (1 : 0..1)
    @Id
    @Column(name = "breakdown_request_id")
    private Long breakdownRequestId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "breakdown_request_id")
    private BreakdownRequest breakdownRequest;

    @Column(name = "responded_at")
    private OffsetDateTime respondedAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "response_breach")
    private Boolean responseBreach = Boolean.FALSE;  //

    @Column(name = "resolution_breach")
    private Boolean resolutionBreach = Boolean.FALSE;

    // getters and setters

    public Long getBreakdownRequestId() {
        return breakdownRequestId;
    }

    public void setBreakdownRequestId(Long breakdownRequestId) {
        this.breakdownRequestId = breakdownRequestId;
    }

    public BreakdownRequest getBreakdownRequest() {
        return breakdownRequest;
    }

    public void setBreakdownRequest(BreakdownRequest breakdownRequest) {
        this.breakdownRequest = breakdownRequest;
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
}
