package com.example.backend.AssetManagamentService.mapper;

import com.example.backend.AssetManagamentService.dto.asset.AssetResponse;
import com.example.backend.AssetManagamentService.dto.asset.AssetSummaryResponse;
import com.example.backend.AssetManagamentService.dto.assetclass.AssetClassResponse;
import com.example.backend.AssetManagamentService.dto.maintenanceplan.MaintenancePlanResponse;
import com.example.backend.AssetManagamentService.dto.odometer.OdometerReadingResponse;
import com.example.backend.AssetManagamentService.entity.Asset;
import com.example.backend.AssetManagamentService.entity.AssetClass;
import com.example.backend.AssetManagamentService.entity.MaintenancePlan;
import com.example.backend.AssetManagamentService.entity.OdometerReading;
import org.springframework.stereotype.Component;

@Component
public class AssetManagementMapper {

    public AssetResponse toAssetResponse(Asset asset) {
        AssetResponse response = new AssetResponse();

        response.setId(asset.getId());
        response.setVin(asset.getVin());
        response.setAssetClassId(asset.getAssetClass().getId());
        response.setAssetClassCode(asset.getAssetClass().getCode());
        response.setHomeDepotId(asset.getHomeDepotId());
        response.setAcquisitionDate(asset.getAcquisitionDate());
        response.setAcquisitionOdometerKm(
                asset.getAcquisitionOdometerKm()
        );
        response.setStatus(asset.getStatus());

        return response;
    }

    public AssetSummaryResponse toAssetSummaryResponse(Asset asset) {
        AssetSummaryResponse response = new AssetSummaryResponse();

        response.setId(asset.getId());
        response.setVin(asset.getVin());
        response.setAssetClassCode(asset.getAssetClass().getCode());
        response.setStatus(asset.getStatus());

        return response;
    }

    public AssetClassResponse toAssetClassResponse(
            AssetClass assetClass
    ) {
        AssetClassResponse response = new AssetClassResponse();

        response.setId(assetClass.getId());
        response.setCode(assetClass.getCode());
        response.setDescription(assetClass.getDescription());

        return response;
    }

    public MaintenancePlanResponse toMaintenancePlanResponse(
            MaintenancePlan plan
    ) {
        MaintenancePlanResponse response =
                new MaintenancePlanResponse();

        response.setId(plan.getId());
        response.setCode(plan.getCode());
        response.setDistanceIntervalKm(
                plan.getDistanceIntervalKm()
        );
        response.setTimeIntervalDays(
                plan.getTimeIntervalDays()
        );
        response.setEstimatedDurationMinutes(
                plan.getEstimatedDurationMinutes()
        );
        response.setRequiredSkillCode(
                plan.getRequiredSkillCode()
        );
        response.setRequiredCapabilityCode(
                plan.getRequiredCapabilityCode()
        );

        return response;
    }

    public OdometerReadingResponse toOdometerResponse(
            OdometerReading reading
    ) {
        OdometerReadingResponse response =
                new OdometerReadingResponse();

        response.setId(reading.getId());
        response.setReadingKm(reading.getReadingKm());

        response.setReadAt(reading.getReadAt());

        response.setSource(reading.getSource());
        response.setRecordedById(reading.getRecordedById());

        return response;
    }
}