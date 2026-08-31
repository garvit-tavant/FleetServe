package com.example.backend.AssetManagamentService.entity;

import com.example.backend.AssetManagamentService.status.AssetStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "asset")
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String vin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_class_id", nullable = false)
    private AssetClass assetClass;

    @Column(name = "home_depot_id", nullable = false)
    private Long homeDepotId;

    @Column(name = "acquisition_date", nullable = false)
    private LocalDate acquisitionDate;

    @Column(
            name = "acquisition_odometer_km",
            nullable = false,
            precision = 12,
            scale = 3
    )
    private BigDecimal acquisitionOdometerKm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AssetStatus status;

    @Version
    private Long version;

    public Asset() {
    }

    public Long getId() {
        return id;
    }

    public String getVin() {
        return vin;
    }

    public AssetClass getAssetClass() {
        return assetClass;
    }

    public Long getHomeDepotId() {
        return homeDepotId;
    }

    public LocalDate getAcquisitionDate() {
        return acquisitionDate;
    }

    public BigDecimal getAcquisitionOdometerKm() {
        return acquisitionOdometerKm;
    }

    public AssetStatus getStatus() {
        return status;
    }

    public Long getVersion() {
        return version;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public void setAssetClass(AssetClass assetClass) {
        this.assetClass = assetClass;
    }

    public void setHomeDepotId(Long homeDepotId) {
        this.homeDepotId = homeDepotId;
    }

    public void setAcquisitionDate(LocalDate acquisitionDate) {
        this.acquisitionDate = acquisitionDate;
    }

    public void setAcquisitionOdometerKm(BigDecimal acquisitionOdometerKm) {
        this.acquisitionOdometerKm = acquisitionOdometerKm;
    }

    public void setStatus(AssetStatus status) {
        this.status = status;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}