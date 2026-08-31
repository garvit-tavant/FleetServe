package com.example.backend.AssetManagamentService.controller;

import com.example.backend.AssetManagamentService.dto.odometer.AddOdometerReadingRequest;
import com.example.backend.AssetManagamentService.dto.odometer.OdometerReadingResponse;
import com.example.backend.AssetManagamentService.service.OdometerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/assets/{assetId}/odometer-readings")
public class OdometerController {

    private final OdometerService odometerService;

    public OdometerController(OdometerService odometerService) {
        this.odometerService = odometerService;
    }

    /**
     * POST /api/assets/{assetId}/odometer-readings
     *
     * Adds a new odometer reading.
     */
    @PostMapping
    public ResponseEntity<OdometerReadingResponse> addOdometerReading(
            @PathVariable Long assetId,
            @Valid @RequestBody AddOdometerReadingRequest request
    ) {

        OdometerReadingResponse response =
                odometerService.addReading(assetId, request);

        URI location = URI.create(
                "/api/assets/"
                        + assetId
                        + "/odometer-readings/"
                        + response.getId()
        );

        return ResponseEntity
                .created(location)
                .body(response);
    }

    /**
     * GET /api/assets/{assetId}/odometer-readings
     *
     * Returns all odometer readings for the asset
     * ordered by latest first.
     */
    @GetMapping
    public ResponseEntity<List<OdometerReadingResponse>>
    getOdometerHistory(
            @PathVariable Long assetId
    ) {

        List<OdometerReadingResponse> response =
                odometerService.getHistory(assetId);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/assets/{assetId}/odometer-readings/latest
     *
     * Returns the latest odometer reading.
     */
    @GetMapping("/latest")
    public ResponseEntity<OdometerReadingResponse>
    getLatestOdometerReading(
            @PathVariable Long assetId
    ) {

        OdometerReadingResponse response =
                odometerService.getLatestReading(assetId);

        return ResponseEntity.ok(response);
    }
}