package com.example.backend.InventoryService.dtos.inventorymovement;

import java.math.BigDecimal;

public class IssuePartRequest {

    private Long partId;

    private Long workshopId;

    private BigDecimal quantity;

    public Long getPartId() {
        return partId;
    }

    public void setPartId(Long partId) {
        this.partId = partId;
    }

    public Long getWorkshopId() {
        return workshopId;
    }

    public void setWorkshopId(Long workshopId) {
        this.workshopId = workshopId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
}