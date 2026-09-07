package com.example.backend.InventoryService.entity;

import com.example.backend.CapacityAndSchedulingService.entity.Workshop;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "inventory_movement")
public class InventoryMovement {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "part_id",
            nullable = false
    )
    private Part part;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "workshop_id",
            nullable = false
    )
    private Workshop workshop;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "movement_type",
            nullable = false,
            length = 30
    )
    private InventoryMovementType movementType;

    @Column(
            name = "signed_quantity",
            nullable = false,
            precision = 12,
            scale = 3
    )
    private BigDecimal signedQuantity;

    @Column(
            name = "unit_cost",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal unitCost;

    @Column(
            name = "transfer_reference",
            length = 100
    )
    private String transferReference;

    @Column(
            length = 1000
    )
    private String reason;

    @Column(
            name = "recorded_by",
            nullable = false,
            length = 100
    )
    private String recordedBy;

    @Column(
            name = "occurred_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime occurredAt;

    // getters setters


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Part getPart() {
        return part;
    }

    public void setPart(Part part) {
        this.part = part;
    }

    public Workshop getWorkshop() {
        return workshop;
    }

    public void setWorkshop(Workshop workshop) {
        this.workshop = workshop;
    }

    public InventoryMovementType getMovementType() {
        return movementType;
    }

    public void setMovementType(InventoryMovementType movementType) {
        this.movementType = movementType;
    }

    public BigDecimal getSignedQuantity() {
        return signedQuantity;
    }

    public void setSignedQuantity(BigDecimal signedQuantity) {
        this.signedQuantity = signedQuantity;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public String getTransferReference() {
        return transferReference;
    }

    public void setTransferReference(String transferReference) {
        this.transferReference = transferReference;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(String recordedBy) {
        this.recordedBy = recordedBy;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(OffsetDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }
}