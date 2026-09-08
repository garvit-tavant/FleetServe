package com.example.backend.AssetManagamentService.controller;

import com.example.backend.AssetManagamentService.dto.duemaintenance.DueMaintenanceFilter;
import com.example.backend.AssetManagamentService.dto.duemaintenance.DueMaintenanceResponse;
import com.example.backend.AssetManagamentService.service.DueMaintenanceService;
import com.example.backend.AssetManagamentService.status.DueStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/maintenance/due")
public class DueMaintenanceController {

    private final DueMaintenanceService dueMaintenanceService;

    public DueMaintenanceController(DueMaintenanceService dueMaintenanceService) {
        this.dueMaintenanceService = dueMaintenanceService;
    }

    /**
     * GET /api/maintenance/due
     *
     * <p>Preventive maintenance worklist. An asset appears once per maintenance
     * plan attached to its class, classified OVERDUE, DUE_SOON or OK by whichever
     * of the distance and time thresholds is reached first.
     *
     * <p>The DUE_SOON windows come from each plan's own
     * {@code due_soon_distance_km} and {@code due_soon_days}. The two override
     * parameters exist for what-if planning and are not the source of the
     * defaults.
     *
     * <p>Ordered most urgent first. Page size is capped server-side.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('FLEET_ADMINISTRATOR','MAINTENANCE_PLANNER',"
            + "'WORKSHOP_MANAGER','SERVICE_COORDINATOR')")
    public ResponseEntity<Page<DueMaintenanceResponse>> getDueMaintenance(
            @RequestParam(required = false) Long depotId,
            @RequestParam(required = false) String assetClassCode,
            @RequestParam(required = false) DueStatus status,
            @RequestParam(defaultValue = "true") boolean dueOnly,
            @RequestParam(required = false) BigDecimal distanceSoonThresholdKm,
            @RequestParam(required = false) Integer timeSoonThresholdDays,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        DueMaintenanceFilter filter = new DueMaintenanceFilter(
                distanceSoonThresholdKm,
                timeSoonThresholdDays,
                depotId,
                assetClassCode,
                status,
                dueOnly
        );

        Page<DueMaintenanceResponse> result =
                dueMaintenanceService.getDueMaintenance(
                        filter, PageRequest.of(page, size));

        return ResponseEntity.ok(result);
    }
}
