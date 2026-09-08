package com.example.backend.AssetManagamentService.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "asset_class_plan")
public class AssetClassPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "asset_class_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_asset_class_plan_class"
            )
    )
    private AssetClass assetClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "maintenance_plan_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_asset_class_plan_plan"
            )
    )
    private MaintenancePlan maintenancePlan;

    /*
     * DUE_SOON warning windows for this class-and-plan pairing.
     *
     * They live here rather than on the plan because one plan can apply to
     * several classes: the same 10 000 km service may warrant a narrow warning
     * on a van and a wide one on a heavy vehicle that is harder to schedule.
     *
     * Null means no amber warning on that axis. A database trigger keeps each
     * window shorter than the interval it qualifies.
     */
    @Column(name = "due_soon_distance_km", precision = 12, scale = 3)
    private BigDecimal dueSoonDistanceKm;

    @Column(name = "due_soon_days")
    private Integer dueSoonDays;

    public AssetClassPlan() {
    }

    public BigDecimal getDueSoonDistanceKm() {
        return dueSoonDistanceKm;
    }

    public void setDueSoonDistanceKm(BigDecimal dueSoonDistanceKm) {
        this.dueSoonDistanceKm = dueSoonDistanceKm;
    }

    public Integer getDueSoonDays() {
        return dueSoonDays;
    }

    public void setDueSoonDays(Integer dueSoonDays) {
        this.dueSoonDays = dueSoonDays;
    }

    public Long getId() {
        return id;
    }

    public AssetClass getAssetClass() {
        return assetClass;
    }

    public MaintenancePlan getMaintenancePlan() {
        return maintenancePlan;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setAssetClass(AssetClass assetClass) {
        this.assetClass = assetClass;
    }

    public void setMaintenancePlan(MaintenancePlan maintenancePlan) {
        this.maintenancePlan = maintenancePlan;
    }
}