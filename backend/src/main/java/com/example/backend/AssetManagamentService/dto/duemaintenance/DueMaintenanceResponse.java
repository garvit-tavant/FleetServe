package com.example.backend.AssetManagamentService.dto.duemaintenance;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DueMaintenanceResponse {

    private Long assetId;

    private String vin;

    private Long maintenancePlanId;

    private String maintenancePlanCode;

    private BigDecimal currentOdometerKm;

    private BigDecimal nextDueKm;

    private LocalDate nextDueDate;

    private String dueStatus;

    public DueMaintenanceResponse() {
    }

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

    public Long getMaintenancePlanId() {
        return maintenancePlanId;
    }

    public void setMaintenancePlanId(Long maintenancePlanId) {
        this.maintenancePlanId = maintenancePlanId;
    }

    public String getMaintenancePlanCode() {
        return maintenancePlanCode;
    }

    public void setMaintenancePlanCode(String maintenancePlanCode) {
        this.maintenancePlanCode = maintenancePlanCode;
    }

    public BigDecimal getCurrentOdometerKm() {
        return currentOdometerKm;
    }

    public void setCurrentOdometerKm(BigDecimal currentOdometerKm) {
        this.currentOdometerKm = currentOdometerKm;
    }

    public BigDecimal getNextDueKm() {
        return nextDueKm;
    }

    public void setNextDueKm(BigDecimal nextDueKm) {
        this.nextDueKm = nextDueKm;
    }

    public LocalDate getNextDueDate() {
        return nextDueDate;
    }

    public void setNextDueDate(LocalDate nextDueDate) {
        this.nextDueDate = nextDueDate;
    }

    public String getDueStatus() {
        return dueStatus;
    }

    public void setDueStatus(String dueStatus) {
        this.dueStatus = dueStatus;
    }
}