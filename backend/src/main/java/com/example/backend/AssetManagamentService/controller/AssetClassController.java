package com.example.backend.AssetManagamentService.controller;

import com.example.backend.AssetManagamentService.dto.assetclass.AssetClassResponse;
import com.example.backend.AssetManagamentService.dto.assetclass.CreateAssetClassRequest;
import com.example.backend.AssetManagamentService.service.AssetClassService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/asset-classes")
public class AssetClassController {

    private final AssetClassService assetClassService;

    public AssetClassController(
            AssetClassService assetClassService
    ) {
        this.assetClassService = assetClassService;
    }

    /*
     * POST /api/asset-classes
     *
     * Creates a new asset class such as:
     * PASSENGER_CAR, TRUCK, BUS.
     */
    @PostMapping
    public ResponseEntity<AssetClassResponse> createAssetClass(
            @Valid @RequestBody
            CreateAssetClassRequest request
    ) {
        AssetClassResponse response =
                assetClassService.createAssetClass(request);

        URI location = URI.create(
                "/api/asset-classes/" + response.getId()
        );

        return ResponseEntity
                .created(location)
                .body(response);
    }

    /*
     * GET /api/asset-classes/{assetClassId}
     */
    @GetMapping("/{assetClassId}")
    public ResponseEntity<AssetClassResponse> getAssetClass(
            @PathVariable Long assetClassId
    ) {
        AssetClassResponse response =
                assetClassService.getAssetClass(assetClassId);

        return ResponseEntity.ok(response);
    }

    /*
     * GET /api/asset-classes
     */
    @GetMapping
    public ResponseEntity<List<AssetClassResponse>>
    getAllAssetClasses() {

     List<AssetClassResponse> response =
        assetClassService.getAllAssetClasses();

        return ResponseEntity.ok(response);
    }
}