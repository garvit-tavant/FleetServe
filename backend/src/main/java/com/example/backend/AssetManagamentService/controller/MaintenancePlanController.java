package com.example.backend.AssetManagamentService.controller;

import com.example.backend.AssetManagamentService.dto.maintenanceplan.CreateMaintenancePlanRequest;
import com.example.backend.AssetManagamentService.dto.maintenanceplan.MaintenancePlanResponse;
import com.example.backend.AssetManagamentService.service.MaintenancePlanService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/maintenance-plans")
public class MaintenancePlanController {

    private final MaintenancePlanService maintenancePlanService;

    public MaintenancePlanController(
            MaintenancePlanService maintenancePlanService
    ) {
        this.maintenancePlanService =
                maintenancePlanService;
    }

    /**
     * POST /api/maintenance-plans
     *
     * Creates a maintenance plan containing:
     * - distance interval
     * - time interval
     * - estimated duration
     * - required skill
     * - required capability
     */
    @PostMapping
    public ResponseEntity<MaintenancePlanResponse>
    createMaintenancePlan(
            @Valid
            @RequestBody
            CreateMaintenancePlanRequest request
    ) {

        MaintenancePlanResponse response =
                maintenancePlanService.createPlan(request);

        URI location = URI.create(
                "/api/maintenance-plans/" + response.getId()
        );

        return ResponseEntity
                .created(location)
                .body(response);
    }

    /**
     * GET /api/maintenance-plans/{planId}
     */
    @GetMapping("/{planId}")
    public ResponseEntity<MaintenancePlanResponse>
    getMaintenancePlan(
            @PathVariable Long planId
    ) {

        MaintenancePlanResponse response =
                maintenancePlanService.getPlan(planId);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/maintenance-plans
     */
    @GetMapping
    public ResponseEntity<List<MaintenancePlanResponse>>
    getAllMaintenancePlans() {

        List<MaintenancePlanResponse> response =
                maintenancePlanService.getAllPlans();

        return ResponseEntity.ok(response);
    }
}