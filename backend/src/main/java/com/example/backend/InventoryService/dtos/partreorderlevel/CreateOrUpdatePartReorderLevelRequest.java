package com.example.backend.InventoryService.dtos.partreorderlevel;

import java.math.BigDecimal;

public class CreateOrUpdatePartReorderLevelRequest {

    private Long partId;

    private Long workshopId;

    private BigDecimal reorderLevel;

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

    public BigDecimal getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(BigDecimal reorderLevel) {
        this.reorderLevel = reorderLevel;
    }
}