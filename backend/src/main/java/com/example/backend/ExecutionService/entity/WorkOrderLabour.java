package com.example.backend.ExecutionService.entity;

import java.math.BigDecimal;

import com.example.backend.CapacityAndSchedulingService.entity.Technician;

import jakarta.persistence.*;

@Entity
@Table(name = "work_order_labour")
public class WorkOrderLabour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false)
    @JoinColumn(
            name = "work_order_id",
            nullable = false)
    private WorkOrder workOrder;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false)
    @JoinColumn(
            name = "technician_id",
            nullable = false)
    private Technician technician;

    @Column(
            name = "hours",
            nullable = false,
            precision = 5,
            scale = 2)
    private BigDecimal hours;

    @Column(
            name = "rate_applied",
            nullable = false,
            precision = 12,
            scale = 2)
    private BigDecimal rateApplied;

    // getters setters


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public WorkOrder getWorkOrder() {
        return workOrder;
    }

    public void setWorkOrder(WorkOrder workOrder) {
        this.workOrder = workOrder;
    }

    public Technician getTechnician() {
        return technician;
    }

    public void setTechnician(Technician technician) {
        this.technician = technician;
    }

    public BigDecimal getHours() {
        return hours;
    }

    public void setHours(BigDecimal hours) {
        this.hours = hours;
    }

    public BigDecimal getRateApplied() {
        return rateApplied;
    }

    public void setRateApplied(BigDecimal rateApplied) {
        this.rateApplied = rateApplied;
    }
}