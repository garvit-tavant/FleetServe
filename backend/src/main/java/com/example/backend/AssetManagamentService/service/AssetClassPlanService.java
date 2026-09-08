package com.example.backend.AssetManagamentService.service;

import com.example.backend.AssetManagamentService.dto.assetclass.DueSoonWindowRequest;
import com.example.backend.AssetManagamentService.dto.maintenanceplan.MaintenancePlanResponse;

import java.util.List;

public interface AssetClassPlanService {

    void attachPlanToAssetClass(
            Long assetClassId,
            Long maintenancePlanId
    );

    /**
     * Attaches a plan and sets the DUE_SOON windows for the pairing in one step.
     * A null {@code window}, or null fields within it, means no amber warning on
     * that axis.
     */
    void attachPlanToAssetClass(
            Long assetClassId,
            Long maintenancePlanId,
            DueSoonWindowRequest window
    );

    /** Changes the DUE_SOON windows on an existing pairing. */
    void updateDueSoonWindow(
            Long assetClassId,
            Long maintenancePlanId,
            DueSoonWindowRequest window
    );

    void removePlanFromAssetClass(
            Long assetClassId,
            Long maintenancePlanId
    );

    List<MaintenancePlanResponse> getPlansForAssetClass(
            Long assetClassId
    );
}
