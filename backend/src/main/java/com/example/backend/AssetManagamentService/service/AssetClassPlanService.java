package com.example.backend.AssetManagamentService.service;

import com.example.backend.AssetManagamentService.dto.maintenanceplan.MaintenancePlanResponse;

import java.util.List;

public interface AssetClassPlanService {

    void attachPlanToAssetClass(
            Long assetClassId,
            Long maintenancePlanId
    );

    void removePlanFromAssetClass(
            Long assetClassId,
            Long maintenancePlanId
    );

    List<MaintenancePlanResponse> getPlansForAssetClass(
            Long assetClassId
    );
}
