package com.example.backend.ExecutionService.dto.workorderpart;

import java.math.BigDecimal;

public class WorkOrderPartResponse {

    private Long id;

    private Long partId;

    private String partCode;

    private BigDecimal quantity;

    private BigDecimal unitCost;

    private BigDecimal lineCost;

    private Long movementId;

    // getters setters


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPartId() {
        return partId;
    }

    public void setPartId(Long partId) {
        this.partId = partId;
    }

    public String getPartCode() {
        return partCode;
    }

    public void setPartCode(String partCode) {
        this.partCode = partCode;
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

    public BigDecimal getLineCost() {
        return lineCost;
    }

    public void setLineCost(BigDecimal lineCost) {
        this.lineCost = lineCost;
    }

    public Long getMovementId() {
        return movementId;
    }

    public void setMovementId(Long movementId) {
        this.movementId = movementId;
    }
}