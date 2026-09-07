package com.example.backend.InventoryService.dtos.inventorymovement;

import java.math.BigDecimal;

public class TransferStockRequest {

    private Long partId;

    private Long fromWorkshopId;

    private Long toWorkshopId;

    private BigDecimal quantity;

    private String reason;

// getters

    public Long getPartId() {
        return partId;
    }

    public void setPartId(Long partId) {
        this.partId = partId;
    }

    public Long getFromWorkshopId() {
        return fromWorkshopId;
    }

    public void setFromWorkshopId(Long fromWorkshopId) {
        this.fromWorkshopId = fromWorkshopId;
    }

    public Long getToWorkshopId() {
        return toWorkshopId;
    }

    public void setToWorkshopId(Long toWorkshopId) {
        this.toWorkshopId = toWorkshopId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}