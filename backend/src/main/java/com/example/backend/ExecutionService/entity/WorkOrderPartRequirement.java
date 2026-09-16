package com.example.backend.ExecutionService.entity;

import com.example.backend.ExecutionService.status.WorkOrderPartRequirementStatus;
import com.example.backend.InventoryService.entity.Part;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "work_order_part_requirement"
)
public class WorkOrderPartRequirement {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "work_order_id",
            nullable = false
    )
    private WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "part_id",
            nullable = false
    )
    private Part part;

    @Column(
            name = "quantity_required",
            nullable = false,
            precision = 12,
            scale = 3
    )
    private BigDecimal quantityRequired;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private WorkOrderPartRequirementStatus status;

    @Column(
            length = 500
    )
    private String reason;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(
            name = "resolved_at"
    )
    private OffsetDateTime resolvedAt;

    @Version
    @Column(
            nullable = false
    )
    private Long version;

    public Long getId() {
        return id;
    }

    public void setId(
            Long id
    ) {
        this.id = id;
    }

    public WorkOrder getWorkOrder() {
        return workOrder;
    }

    public void setWorkOrder(
            WorkOrder workOrder
    ) {
        this.workOrder = workOrder;
    }

    public Part getPart() {
        return part;
    }

    public void setPart(
            Part part
    ) {
        this.part = part;
    }

    public BigDecimal getQuantityRequired() {
        return quantityRequired;
    }

    public void setQuantityRequired(
            BigDecimal quantityRequired
    ) {
        this.quantityRequired = quantityRequired;
    }

    public WorkOrderPartRequirementStatus getStatus() {
        return status;
    }

    public void setStatus(
            WorkOrderPartRequirementStatus status
    ) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(
            String reason
    ) {
        this.reason = reason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(
            OffsetDateTime createdAt
    ) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(
            OffsetDateTime resolvedAt
    ) {
        this.resolvedAt = resolvedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(
            Long version
    ) {
        this.version = version;
    }
}