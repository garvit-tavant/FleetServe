package com.example.backend.ExecutionService.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "work_order")
public class WorkOrder {
    /*
     id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    work_order_number   VARCHAR(50)   NOT NULL,
    booking_id          BIGINT        NOT NULL,
    asset_id            BIGINT        NOT NULL,
    status              VARCHAR(30)   NOT NULL DEFAULT 'SCHEDULED',
    started_at          TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ,
    odometer_at_service NUMERIC(12,3),
    total_cost          NUMERIC(12,2) NOT NULL DEFAULT 0,
    idempotency_key     VARCHAR(100)  NOT NULL,
    version             BIGINT        NOT NULL DEFAULT 0,
    */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "work_order_number", nullable = false)
    private String workOrderNumber;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "started_at", columnDefinition = "TIME WITH TIME ZONE")
    private Instant startedAt;

    @Column(name = "completed_at", columnDefinition = "TIME WITH TIME ZONE")
    private Instant completedAt;

    @Column(name = "odometer_at_service", precision = 12, scale = 3)
    private BigDecimal odometerAtService;

    @Column(name = "total_cost", nullable = false , precision = 12, scale = 2)
    private BigDecimal totalCost;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "version", nullable = false)
    private Long version;

    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<WorkOrderLabour> labourEntries = new ArrayList<>();

    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<WorkOrderPart> partEntries = new ArrayList<>();

    //getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getWorkOrderNumber() {
        return workOrderNumber;
    }

    public void setWorkOrderNumber(String workOrderNumber) {
        this.workOrderNumber = workOrderNumber;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Long getAssetId() {
        return assetId;
    }

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public BigDecimal getOdometerAtService() {
        return odometerAtService;
    }

    public void setOdometerAtService(BigDecimal odometerAtService) {
        this.odometerAtService = odometerAtService;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

}
