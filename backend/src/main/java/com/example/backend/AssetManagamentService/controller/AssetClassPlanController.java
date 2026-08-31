package com.example.backend.AssetManagamentService.controller;

import com.example.backend.AssetManagamentService.dto.maintenanceplan.MaintenancePlanResponse;
import com.example.backend.AssetManagamentService.service.AssetClassPlanService;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@Validated
@RestController
@RequestMapping("/api/asset-classes")
public class AssetClassPlanController {

    private final AssetClassPlanService assetClassPlanService;

    public AssetClassPlanController(
            AssetClassPlanService assetClassPlanService
    ) {
        this.assetClassPlanService = assetClassPlanService;
    }

    /**
     * POST /api/asset-classes/{assetClassId}/plans/{planId}
     *
     * Maps a maintenance plan to an asset class.
     *
     * No request body is required because both IDs
     * are available in the URL.
     */
    @PostMapping("/{assetClassId}/plans/{planId}")
    public ResponseEntity<Void> attachPlanToAssetClass(
            @Positive(message="asset class Id must be positive")
            @PathVariable Long assetClassId,
            @Positive(message = "Plan ID must be positive")
            @PathVariable Long planId
    ) {

        assetClassPlanService.attachPlanToAssetClass(
                assetClassId,
                planId
        );

        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /api/asset-classes/{assetClassId}/plans/{planId}
     *
     * Removes a plan mapping from an asset class.
     *
     * This deletes only the asset_class_plan mapping,
     * not the asset class or maintenance plan.
     */
    @DeleteMapping("/{assetClassId}/plans/{planId}")
    public ResponseEntity<Void> removePlanFromAssetClass(
            @PathVariable Long assetClassId,
            @PathVariable Long planId
    ) {

        assetClassPlanService.removePlanFromAssetClass(
                assetClassId,
                planId
        );

        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/asset-classes/{assetClassId}/plans
     *
     * Returns all maintenance plans attached
     * to the specified asset class.
     */
    @GetMapping("/{assetClassId}/plans")
    public ResponseEntity<List<MaintenancePlanResponse>>
    getPlansForAssetClass(
            @PathVariable Long assetClassId
    ) {

        List<MaintenancePlanResponse> response =
                assetClassPlanService
                        .getPlansForAssetClass(assetClassId);

        return ResponseEntity.ok(response);
    }
}