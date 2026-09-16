package com.example.backend.ExecutionService.entity;

import java.math.BigDecimal;

import com.example.backend.InventoryService.entity.InventoryMovement;
import com.example.backend.InventoryService.entity.Part;

import jakarta.persistence.*;

@Entity
@Table(name = "work_order_part")
public class WorkOrderPart {

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
            name = "part_id",
            nullable = false)
    private Part part;

    @Column(
            name = "quantity",
            nullable = false,
            precision = 12,
            scale = 3)
    private BigDecimal quantity;

    @Column(
            name = "unit_cost",
            nullable = false,
            precision = 12,
            scale = 2)
    private BigDecimal unitCost;

    @OneToOne(
            fetch = FetchType.LAZY,
            optional = false)
    @JoinColumn(
            name = "movement_id",
            nullable = false,
            unique = true)
    private InventoryMovement movement;

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

    public Part getPart() {
        return part;
    }

    public void setPart(Part part) {
        this.part = part;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public InventoryMovement getMovement() {
        return movement;
    }

    public void setMovement(InventoryMovement movement) {
        this.movement = movement;
    }
}