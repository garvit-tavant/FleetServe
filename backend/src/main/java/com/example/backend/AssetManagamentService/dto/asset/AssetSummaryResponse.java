package com.example.backend.AssetManagamentService.dto.asset;

import com.example.backend.AssetManagamentService.status.AssetStatus;

public class AssetSummaryResponse {

    private Long id;

    private String vin;

    private String assetClassCode;

    private AssetStatus status;

    // getters setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public AssetStatus getStatus() {
        return status;
    }

    public void setStatus(AssetStatus status) {
        this.status = status;
    }

    public String getAssetClassCode() {
        return assetClassCode;
    }

    public void setAssetClassCode(String assetClassCode) {
        this.assetClassCode = assetClassCode;
    }
}