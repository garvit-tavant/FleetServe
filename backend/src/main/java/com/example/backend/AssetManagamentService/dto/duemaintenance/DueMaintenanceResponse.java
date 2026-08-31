package com.example.backend.AssetManagamentService.dto.duemaintenance;

import com.example.backend.AssetManagamentService.status.DueStatus;

import java.time.LocalDate;
import java.math.BigDecimal;

public class DueMaintenanceResponse {

    private Long assetId;

    private String vin;

    private String assetClassCode;

    private String maintenancePlanCode;

    private LocalDate nextDueDate;

    private BigDecimal nextDueKm;

    private DueStatus dueStatus;

    // getters setters

    public Long getAssetId() {
        return assetId;
    }

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public String getAssetClassCode() {
        return assetClassCode;
    }

    public void setAssetClassCode(String assetClassCode) {
        this.assetClassCode = assetClassCode;
    }

    public String getMaintenancePlanCode() {
        return maintenancePlanCode;
    }

    public void setMaintenancePlanCode(String maintenancePlanCode) {
        this.maintenancePlanCode = maintenancePlanCode;
    }

    public LocalDate getNextDueDate() {
        return nextDueDate;
    }

    public void setNextDueDate(LocalDate nextDueDate) {
        this.nextDueDate = nextDueDate;
    }

    public BigDecimal getNextDueKm() {
        return nextDueKm;
    }

    public void setNextDueKm(BigDecimal nextDueKm) {
        this.nextDueKm = nextDueKm;
    }

    public DueStatus getDueStatus() {
        return dueStatus;
    }

    public void setDueStatus(DueStatus dueStatus) {
        this.dueStatus = dueStatus;
    }
}