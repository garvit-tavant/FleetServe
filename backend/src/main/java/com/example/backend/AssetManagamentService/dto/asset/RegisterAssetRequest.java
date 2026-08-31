package com.example.backend.AssetManagamentService.dto.asset;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RegisterAssetRequest {

    @NotBlank(message = "VIN or serial number is required")
    @Size(
    max = 100,
    message = "VIN or serial number cannot exceed 100 characters")
    private String vin;

    @NotNull(message = "Asset class ID is required")
    private Long assetClassId;
    @NotNull(message = "Home depot ID is required")
    private Long homeDepotId;
    @NotNull(message = "Acquisition date is required")
    @PastOrPresent(
    message = "Acquisition date cannot be in the future")
    private LocalDate acquisitionDate;
    @NotNull(message = "Acquisition odometer is required")
    @DecimalMin(
    value = "0.000",
    inclusive = true,
    message = "Acquisition odometer cannot be negative"
            )
    private BigDecimal acquisitionOdometerKm;


    // getters setters

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
}