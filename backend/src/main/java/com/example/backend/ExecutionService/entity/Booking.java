package com.example.backend.ExecutionService.entity;
import org.springframework.data.domain.Range;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

import com.example.backend.AssetManagamentService.entity.Asset;
import com.example.backend.AssetManagamentService.entity.MaintenancePlan;
import com.example.backend.CapacityAndSchedulingService.entity.Bay;
import com.example.backend.CapacityAndSchedulingService.entity.Technician;
import com.example.backend.CapacityAndSchedulingService.entity.Workshop;
import com.example.backend.ExecutionService.status.BookingKind;
import com.example.backend.ExecutionService.status.BookingStatus;
import com.example.backend.SLA.entity.BreakdownRequest;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Range;

@Entity
@Table(name = "booking")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false)
    @JoinColumn(
            name = "asset_id",
            nullable = false)
    private Asset asset;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false)
    @JoinColumn(
            name = "workshop_id",
            nullable = false)
    private Workshop workshop;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false)
    @JoinColumn(
            name = "bay_id",
            nullable = false)
    private Bay bay;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false)
    @JoinColumn(
            name = "technician_id",
            nullable = false)
    private Technician technician;

    @Column(
            name = "start_at",
            nullable = false
    )
    private OffsetDateTime startAt;

    @Column(
            name = "end_at",
            nullable = false
    )
    private OffsetDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "kind",
            nullable = false,
            length = 20)
    private BookingKind kind;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maintenance_plan_id")
    private MaintenancePlan maintenancePlan;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "breakdown_request_id",
            unique = true)
    private BreakdownRequest breakdownRequest;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30)
    private BookingStatus status;

    @Version
    @Column(
            name = "version",
            nullable = false)
    private Long version;

    @OneToOne(
            mappedBy = "booking",
            fetch = FetchType.LAZY,
            optional = true)
    private WorkOrder workOrder;

    @OneToMany(
            mappedBy = "booking",
            fetch = FetchType.LAZY)
    private List<BookingHistory> history =
            new ArrayList<>();

    public Booking() {
    }

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

    public Workshop getWorkshop() {
        return workshop;
    }

    public void setWorkshop(Workshop workshop) {
        this.workshop = workshop;
    }

    public Bay getBay() {
        return bay;
    }

    public void setBay(Bay bay) {
        this.bay = bay;
    }

    public Technician getTechnician() {
        return technician;
    }

    public void setTechnician(Technician technician) {
        this.technician = technician;
    }

    public BookingKind getKind() {
        return kind;
    }

    public void setKind(BookingKind kind) {
        this.kind = kind;
    }

    public MaintenancePlan getMaintenancePlan() {
        return maintenancePlan;
    }

    public void setMaintenancePlan(
            MaintenancePlan maintenancePlan) {
        this.maintenancePlan = maintenancePlan;
    }

    public BreakdownRequest getBreakdownRequest() {
        return breakdownRequest;
    }

    public void setBreakdownRequest(
            BreakdownRequest breakdownRequest) {
        this.breakdownRequest = breakdownRequest;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(
            BookingStatus status) {
        this.status = status;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(
            Long version) {
        this.version = version;
    }

    public WorkOrder getWorkOrder() {
        return workOrder;
    }

    public void setWorkOrder(
            WorkOrder workOrder) {
        this.workOrder = workOrder;
    }

    public List<BookingHistory> getHistory() {
        return history;
    }

    public void setHistory(List<BookingHistory> history) {
        this.history = history;
    }

    public OffsetDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(OffsetDateTime startAt) {
        this.startAt = startAt;
    }

    public OffsetDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(OffsetDateTime endAt) {
        this.endAt = endAt;
    }
}