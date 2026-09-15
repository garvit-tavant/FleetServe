package com.example.backend.AssetManagamentService.controller;

import com.example.backend.AssetManagamentService.dto.asset.AssetResponse;
import com.example.backend.AssetManagamentService.dto.asset.AssetSummaryResponse;
import com.example.backend.AssetManagamentService.dto.asset.RegisterAssetRequest;
import com.example.backend.AssetManagamentService.dto.asset.ReinstateAssetRequest;
import com.example.backend.AssetManagamentService.dto.asset.RetireAssetRequest;
import com.example.backend.AssetManagamentService.service.AssetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    /*
     * POST /api/assets
     *
     * Registers an asset and creates its initial odometer reading.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")  // only ADMIN can register assets
    public ResponseEntity<AssetResponse> registerAsset(
            @Valid @RequestBody RegisterAssetRequest request
    ) {
        AssetResponse response =
                assetService.registerAsset(request);

        URI location =
                URI.create("/api/assets/" + response.getId());

        return ResponseEntity
                .created(location)
                .body(response);
    }

    /*
     * GET /api/assets/{assetId}
     *
     * Returns complete asset details.
     */
    @GetMapping("/{assetId}")
    @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN')")  // both roles can read
    public ResponseEntity<AssetResponse> getAsset(
            @PathVariable Long assetId
    ) {
        AssetResponse response =
                assetService.getAsset(assetId);

        return ResponseEntity.ok(response);
    }

    /*
     * GET /api/assets
     *
     * Returns all asset summaries.
     *
     * This can later be changed to Page<AssetSummaryResponse>
     * when pagination is implemented.
     */
    @GetMapping
    public ResponseEntity<List<AssetSummaryResponse>>
    getAllAssets() {

        List<AssetSummaryResponse> response =
                assetService.getAllAssets();

        return ResponseEntity.ok(response);
    }

    /*
     * POST /api/assets/{assetId}/retire
     *
     * Retires an asset only when no open work order or
     * future active booking exists.
     */
    @PostMapping("/{assetId}/retire")
    public ResponseEntity<Void> retireAsset(
            @PathVariable Long assetId,
            @Valid @RequestBody RetireAssetRequest request
    ) {
        assetService.retireAsset(assetId, request);

        return ResponseEntity.noContent().build();
    }

    /*
     * POST /api/assets/{assetId}/reinstate
     *
     * Reinstates a previously retired asset.
     */
    @PostMapping("/{assetId}/reinstate")
    public ResponseEntity<Void> reinstateAsset(
            @PathVariable Long assetId,
            @Valid @RequestBody ReinstateAssetRequest request
    ) {
        assetService.reinstateAsset(assetId, request);

        return ResponseEntity.noContent().build();
    }
}