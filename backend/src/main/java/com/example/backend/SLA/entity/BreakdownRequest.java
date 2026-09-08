package com.example.backend.SLA.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

import com.example.backend.ExecutionService.entity.Booking;

@Entity
@Table(name = "breakdown_request")
public class BreakdownRequest {
    /*
    id             BIGINT GENERATED ALWAYS AS IDENTITY,
    asset_id       BIGINT       NOT NULL,
    depot_id       BIGINT       NOT NULL,
    reported_by_id BIGINT       NOT NULL,
    reported_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    priority       VARCHAR(20)  NOT NULL,
    description    VARCHAR(2000) NOT NULL,
    status         VARCHAR(30)  NOT NULL DEFAULT 'REPORTED',
    sla_policy_id  BIGINT       NOT NULL,
    resulting_booking_id BIGINT ,
    */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @Column(name = "depot_id", nullable = false)
    private Long depotId;

    @Column(name = "reported_by_id", nullable = false)
    private Long reportedById;

    @Column(name = "reported_at", nullable = false)
    private OffsetDateTime reportedAt;

    @Column(name = "priority", nullable = false, length = 20)
    private String priority;

    @Column(name = "description", nullable = false, length = 2000)
    private String description;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    // FK resolved as an association instead of a raw id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sla_policy_id", nullable = false)
    private SlaPolicy slaPolicy;

    @OneToOne(mappedBy = "breakdownRequest", fetch = FetchType.LAZY, optional = true) // 1 to 0/1 mapping
    private Booking booking;

    // getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAssetId() {
        return assetId;
    }

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }

    public Long getDepotId() {
        return depotId;
    }

    public void setDepotId(Long depotId) {
        this.depotId = depotId;
    }

    public Long getReportedById() {
        return reportedById;
    }

    public void setReportedById(Long reportedById) {
        this.reportedById = reportedById;
    }

    public OffsetDateTime getReportedAt() {
        return reportedAt;
    }

    public void setReportedAt(OffsetDateTime reportedAt) {
        this.reportedAt = reportedAt;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public SlaPolicy getSlaPolicy() {
        return slaPolicy;
    }

    public void setSlaPolicy(SlaPolicy slaPolicy) {
        this.slaPolicy = slaPolicy;
    }
}
