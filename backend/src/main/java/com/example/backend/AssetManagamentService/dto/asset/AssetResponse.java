package com.example.backend.AssetManagamentService.dto.asset;

import com.example.backend.AssetManagamentService.status.AssetStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AssetResponse {

    private Long id;

    private String vin;

    private Long assetClassId;

    private String assetClassCode;

    private Long homeDepotId;

    private LocalDate acquisitionDate;

    private BigDecimal acquisitionOdometerKm;

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

    public Long getAssetClassId() {
        return assetClassId;
    }

    public void setAssetClassId(Long assetClassId) {
        this.assetClassId = assetClassId;
    }

    public String getAssetClassCode() {
        return assetClassCode;
    }

    public void setAssetClassCode(String assetClassCode) {
        this.assetClassCode = assetClassCode;
    }

    public Long getHomeDepotId() {
        return homeDepotId;
    }

    public void setHomeDepotId(Long homeDepotId) {
        this.homeDepotId = homeDepotId;
    }

    public LocalDate getAcquisitionDate() {
        return acquisitionDate;
    }

    public void setAcquisitionDate(LocalDate acquisitionDate) {
        this.acquisitionDate = acquisitionDate;
    }

    public BigDecimal getAcquisitionOdometerKm() {
        return acquisitionOdometerKm;
    }

    public void setAcquisitionOdometerKm(BigDecimal acquisitionOdometerKm) {
        this.acquisitionOdometerKm = acquisitionOdometerKm;
    }

    public AssetStatus getStatus() {
        return status;
    }

    public void setStatus(AssetStatus status) {
        this.status = status;
    }
}