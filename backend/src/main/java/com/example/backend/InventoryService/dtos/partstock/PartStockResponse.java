package com.example.backend.InventoryService.dtos.partstock;

import java.math.BigDecimal;

public class PartStockResponse {

    private Long partId;

    private String partNumber;

    private String partDescription;

    private Long workshopId;

    private String workshopCode;

    private BigDecimal onHand;

    private BigDecimal reorderLevel;

    private Boolean reorderRequired;

    // getters setters


    public Long getPartId() {
        return partId;
    }

    public void setPartId(Long partId) {
        this.partId = partId;
    }

    public String getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
    }

    public String getPartDescription() {
        return partDescription;
    }

    public void setPartDescription(String partDescription) {
        this.partDescription = partDescription;
    }

    public Long getWorkshopId() {
        return workshopId;
    }

    public void setWorkshopId(Long workshopId) {
        this.workshopId = workshopId;
    }

    public String getWorkshopCode() {
        return workshopCode;
    }

    public void setWorkshopCode(String workshopCode) {
        this.workshopCode = workshopCode;
    }

    public BigDecimal getOnHand() {
        return onHand;
    }

    public void setOnHand(BigDecimal onHand) {
        this.onHand = onHand;
    }

    public BigDecimal getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(BigDecimal reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public Boolean getReorderRequired() {
        return reorderRequired;
    }

    public void setReorderRequired(Boolean reorderRequired) {
        this.reorderRequired = reorderRequired;
    }
}