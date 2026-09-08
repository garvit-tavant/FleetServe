package com.example.backend.AssetManagamentService.service.impl;

import com.example.backend.AssetManagamentService.dto.assetclass.DueSoonWindowRequest;
import com.example.backend.AssetManagamentService.dto.maintenanceplan.MaintenancePlanResponse;
import com.example.backend.AssetManagamentService.entity.AssetClass;
import com.example.backend.AssetManagamentService.entity.AssetClassPlan;
import com.example.backend.AssetManagamentService.entity.MaintenancePlan;
import com.example.backend.AssetManagamentService.exception.BusinessValidationException;
import com.example.backend.AssetManagamentService.exception.DuplicateResourceException;
import com.example.backend.AssetManagamentService.exception.ResourceNotFoundException;
import com.example.backend.AssetManagamentService.mapper.AssetManagementMapper;
import com.example.backend.AssetManagamentService.repository.AssetClassPlanRepository;
import com.example.backend.AssetManagamentService.repository.AssetClassRepository;
import com.example.backend.AssetManagamentService.repository.MaintenancePlanRepository;
import com.example.backend.AssetManagamentService.service.AssetClassPlanService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AssetClassPlanServiceImpl
        implements AssetClassPlanService {

    private final AssetClassPlanRepository mappingRepository;
    private final AssetClassRepository assetClassRepository;
    private final MaintenancePlanRepository planRepository;
    private final AssetManagementMapper mapper;

    public AssetClassPlanServiceImpl(
            AssetClassPlanRepository mappingRepository,
            AssetClassRepository assetClassRepository,
            MaintenancePlanRepository planRepository,
            AssetManagementMapper mapper
    ) {
        this.mappingRepository = mappingRepository;
        this.assetClassRepository = assetClassRepository;
        this.planRepository = planRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void attachPlanToAssetClass(
            Long assetClassId,
            Long maintenancePlanId
    ) {
        attachPlanToAssetClass(assetClassId, maintenancePlanId, null);
    }

    @Override
    @Transactional
    public void attachPlanToAssetClass(
            Long assetClassId,
            Long maintenancePlanId,
            DueSoonWindowRequest window
    ) {
        if (mappingRepository
                .existsByAssetClass_IdAndMaintenancePlan_Id(
                        assetClassId,
                        maintenancePlanId
                )) {
            throw new DuplicateResourceException(
                    "Maintenance plan "
                            + maintenancePlanId
                            + " is already attached to asset class "
                            + assetClassId
            );
        }

        AssetClass assetClass = assetClassRepository
                .findById(assetClassId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Asset class not found with id "
                                        + assetClassId
                        )
                );

        MaintenancePlan plan = planRepository
                .findById(maintenancePlanId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Maintenance plan not found with id "
                                        + maintenancePlanId
                        )
                );

        AssetClassPlan mapping = new AssetClassPlan();
        mapping.setAssetClass(assetClass);
        mapping.setMaintenancePlan(plan);
        applyWindow(mapping, plan, window);

        mappingRepository.save(mapping);
    }

    @Override
    @Transactional
    public void updateDueSoonWindow(
            Long assetClassId,
            Long maintenancePlanId,
            DueSoonWindowRequest window
    ) {
        AssetClassPlan mapping = mappingRepository
                .findByAssetClass_IdAndMaintenancePlan_Id(
                        assetClassId,
                        maintenancePlanId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No mapping exists between asset class "
                                        + assetClassId
                                        + " and maintenance plan "
                                        + maintenancePlanId
                        )
                );

        applyWindow(mapping, mapping.getMaintenancePlan(), window);
        mappingRepository.save(mapping);
    }

    /**
     * A warning window only means something on an axis the plan measures, and it
     * must be shorter than the interval or the asset is permanently amber.
     *
     * <p>A database trigger enforces the same rule as the real guarantee; this
     * check exists so the caller gets a business error rather than a raw
     * constraint violation.
     */
    private void applyWindow(
            AssetClassPlan mapping,
            MaintenancePlan plan,
            DueSoonWindowRequest window
    ) {
        if (window == null) {
            mapping.setDueSoonDistanceKm(null);
            mapping.setDueSoonDays(null);
            return;
        }

        BigDecimal distance = window.dueSoonDistanceKm();
        if (distance != null) {
            if (plan.getDistanceIntervalKm() == null) {
                throw new BusinessValidationException(
                        "Plan " + plan.getCode()
                                + " has no distance interval, so a distance warning window cannot apply"
                );
            }
            if (distance.compareTo(BigDecimal.ZERO) <= 0
                    || distance.compareTo(plan.getDistanceIntervalKm()) >= 0) {
                throw new BusinessValidationException(
                        "Distance warning window must be greater than zero and shorter than the "
                                + "plan interval of " + plan.getDistanceIntervalKm() + " km"
                );
            }
        }

        Integer days = window.dueSoonDays();
        if (days != null) {
            if (plan.getTimeIntervalDays() == null) {
                throw new BusinessValidationException(
                        "Plan " + plan.getCode()
                                + " has no time interval, so a day warning window cannot apply"
                );
            }
            if (days <= 0 || days >= plan.getTimeIntervalDays()) {
                throw new BusinessValidationException(
                        "Day warning window must be greater than zero and shorter than the "
                                + "plan interval of " + plan.getTimeIntervalDays() + " days"
                );
            }
        }

        mapping.setDueSoonDistanceKm(distance);
        mapping.setDueSoonDays(days);
    }

    @Override
    @Transactional
    public void removePlanFromAssetClass(
            Long assetClassId,
            Long maintenancePlanId
    ) {
        AssetClassPlan mapping = mappingRepository
                .findByAssetClass_IdAndMaintenancePlan_Id(
                        assetClassId,
                        maintenancePlanId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No mapping exists between asset class "
                                        + assetClassId
                                        + " and maintenance plan "
                                        + maintenancePlanId
                        )
                );

        mappingRepository.delete(mapping);
    }

    @Override
    public List<MaintenancePlanResponse> getPlansForAssetClass(
            Long assetClassId
    ) {

        AssetClass assetClass = assetClassRepository
                .findById(assetClassId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Asset class not found with id "
                                        + assetClassId
                        )
                );

        return mappingRepository
                .findByAssetClass_Id(assetClass.getId())
                .stream()
                .map(AssetClassPlan::getMaintenancePlan)
                .map(mapper::toMaintenancePlanResponse)
                .toList();
    }
}