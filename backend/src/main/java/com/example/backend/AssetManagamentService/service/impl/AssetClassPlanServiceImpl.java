package com.example.backend.AssetManagamentService.service.impl;

import com.example.backend.AssetManagamentService.dto.maintenanceplan.MaintenancePlanResponse;
import com.example.backend.AssetManagamentService.entity.AssetClass;
import com.example.backend.AssetManagamentService.entity.AssetClassPlan;
import com.example.backend.AssetManagamentService.entity.MaintenancePlan;
import com.example.backend.AssetManagamentService.exception.DuplicateResourceException;
import com.example.backend.AssetManagamentService.exception.ResourceNotFoundException;
import com.example.backend.AssetManagamentService.mapper.AssetManagementMapper;
import com.example.backend.AssetManagamentService.repository.AssetClassPlanRepository;
import com.example.backend.AssetManagamentService.repository.AssetClassRepository;
import com.example.backend.AssetManagamentService.repository.MaintenancePlanRepository;
import com.example.backend.AssetManagamentService.service.AssetClassPlanService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        mappingRepository.save(mapping);
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