package com.example.backend.AssetManagamentService.controller;

import com.example.backend.AssetManagamentService.dto.duemaintenance.DueMaintenanceResponse;
import com.example.backend.AssetManagamentService.service.DueMaintenanceService;
import com.example.backend.ExecutionService.dto.PreventiveBookingResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/due-maintenance")
public class DueMaintenanceController {

    private final DueMaintenanceService dueMaintenanceService;

    public DueMaintenanceController(
            DueMaintenanceService dueMaintenanceService
    ) {
        this.dueMaintenanceService = dueMaintenanceService;
    }

    /*
     * Returns every asset-maintenance-plan combination whose status is:
     *
     * DUE_SOON
     * OVERDUE
     *
     * Assets with status OK are not returned.
     */
    @GetMapping
    public ResponseEntity<List<DueMaintenanceResponse>>
    getDueMaintenanceAssets() {

        List<DueMaintenanceResponse> response =
                dueMaintenanceService
                        .getDueMaintenanceAssets();

        return ResponseEntity.ok(response);
    }

    /*
     * Finds the earliest feasible slot using FeasibleSlotEngine,
     * creates a PREVENTIVE Booking, and automatically creates its
     * SCHEDULED WorkOrder in the same transaction.
     */
    @PostMapping(
            "/assets/{assetId}/maintenance-plans/"
                    + "{maintenancePlanId}/bookings"
    )
    public ResponseEntity<PreventiveBookingResponse>
    createPreventiveBooking(
            @PathVariable Long assetId,
            @PathVariable Long maintenancePlanId
    ) {

        PreventiveBookingResponse response =
                dueMaintenanceService
                        .createPreventiveBooking(
                                assetId,
                                maintenancePlanId
                        );

        URI location =
                ServletUriComponentsBuilder
                        .fromCurrentContextPath()
                        .path("/api/bookings/{bookingId}")
                        .buildAndExpand(
                                response.getBookingId()
                        )
                        .toUri();

        return ResponseEntity
                .created(location)
                .body(response);
    }
}