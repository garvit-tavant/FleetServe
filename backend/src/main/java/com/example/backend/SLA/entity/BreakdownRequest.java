package com.example.backend.SLA.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

import com.example.backend.AssetManagamentService.entity.Asset;
import com.example.backend.CapacityAndSchedulingService.entity.Depot;
import com.example.backend.ExecutionService.entity.Booking;
import com.example.backend.SLA.dto.BreakdownPriority;
import com.example.backend.SLA.dto.BreakdownStatus;
import com.example.backend.SecurityService.entity.AppUser;

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "depot_id", nullable = false)
    private Depot depot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reported_by_id", nullable = false)
    private AppUser reportedBy;

    @Column(name = "reported_at", nullable = false)
    private OffsetDateTime reportedAt;

    @Column(name = "priority", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BreakdownPriority priority;

    @Column(name = "description", nullable = false, length = 2000)
    private String description;

    @Column(name = "status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private BreakdownStatus status;

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

    public Asset getAsset() {
        return asset;
    }

    public void setAsset(Asset asset) {
        this.asset = asset;
    }

    public Depot getDepot() {
        return depot;
    }

    public void setDepot(Depot depot) {
        this.depot = depot;
    }

    public AppUser getReportedBy() {
        return reportedBy;
    }

    public void setReportedBy(AppUser reportedBy) {
        this.reportedBy = reportedBy;
    }

    public OffsetDateTime getReportedAt() {
        return reportedAt;
    }

    public void setReportedAt(OffsetDateTime reportedAt) {
        this.reportedAt = reportedAt;
    }

    public BreakdownPriority getPriority() {
        return priority;
    }

    public void setPriority(BreakdownPriority priority) {
        this.priority = priority;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BreakdownStatus getStatus() {
        return status;
    }

    public void setStatus(BreakdownStatus status) {
        this.status = status;
    }

    public SlaPolicy getSlaPolicy() {
        return slaPolicy;
    }

    public void setSlaPolicy(SlaPolicy slaPolicy) {
        this.slaPolicy = slaPolicy;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }
}

