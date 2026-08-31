package com.example.backend.AssetManagamentService.service;

import com.example.backend.AssetManagamentService.dto.maintenanceplan.CreateMaintenancePlanRequest;
import com.example.backend.AssetManagamentService.dto.maintenanceplan.MaintenancePlanResponse;

import java.util.List;

public interface MaintenancePlanService {

    MaintenancePlanResponse createPlan(
            CreateMaintenancePlanRequest request
    );

    MaintenancePlanResponse getPlan(
            Long planId
    );

    List<MaintenancePlanResponse> getAllPlans();
}