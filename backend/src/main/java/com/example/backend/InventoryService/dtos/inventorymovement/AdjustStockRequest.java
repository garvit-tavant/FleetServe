package com.example.backend.InventoryService.dtos.inventorymovement;

import java.math.BigDecimal;

public class AdjustStockRequest {

    private Long partId;

    private Long workshopId;

    private BigDecimal adjustmentQuantity;

    private String reason;

    // getters setters


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

    public BigDecimal getAdjustmentQuantity() {
        return adjustmentQuantity;
    }

    public void setAdjustmentQuantity(BigDecimal adjustmentQuantity) {
        this.adjustmentQuantity = adjustmentQuantity;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}