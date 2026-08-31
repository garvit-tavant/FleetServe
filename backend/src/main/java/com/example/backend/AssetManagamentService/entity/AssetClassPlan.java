package com.example.backend.AssetManagamentService.entity;

import jakarta.persistence.*;

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

    public AssetClassPlan() {
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