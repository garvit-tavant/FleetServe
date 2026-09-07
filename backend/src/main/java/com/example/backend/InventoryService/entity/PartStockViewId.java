package com.example.backend.InventoryService.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class PartStockViewId
        implements Serializable {

    @Column(name = "part_id")
    private Long partId;

    @Column(name = "workshop_id")
    private Long workshopId;

    // equals hashCode


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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        PartStockViewId that = (PartStockViewId) o;
        return Objects.equals(partId, that.partId) && Objects.equals(workshopId, that.workshopId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(partId, workshopId);
    }
}