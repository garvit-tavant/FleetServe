package com.example.backend.AssetManagamentService.dto.assetclass;

public class AssetClassPlanRequest {

    private Long assetClassId;

    private Long maintenancePlanId;

    // getters setters

    public Long getAssetClassId() {
        return assetClassId;
    }

    public void setAssetClassId(Long assetClassId) {
        this.assetClassId = assetClassId;
    }

    public Long getMaintenancePlanId() {
        return maintenancePlanId;
    }

    public void setMaintenancePlanId(Long maintenancePlanId) {
        this.maintenancePlanId = maintenancePlanId;
    }
}