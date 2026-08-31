package com.example.backend.AssetManagamentService.repository;

import com.example.backend.AssetManagamentService.entity.AssetClassPlan;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface AssetClassPlanRepository
        extends JpaRepository<AssetClassPlan, Long> {

    List<AssetClassPlan> findByAssetClass_Id(Long assetClassId);

    List<AssetClassPlan> findByMaintenancePlan_Id(Long maintenancePlanId);

    Optional<AssetClassPlan>
    findByAssetClass_IdAndMaintenancePlan_Id(
            Long assetClassId,
            Long maintenancePlanId
    );

    boolean existsByAssetClass_IdAndMaintenancePlan_Id(
            Long assetClassId,
            Long maintenancePlanId
    );
}