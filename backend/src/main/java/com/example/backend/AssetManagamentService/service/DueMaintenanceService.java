package com.example.backend.AssetManagamentService.service;


import com.example.backend.AssetManagamentService.dto.duemaintenance.DueMaintenanceResponse;
import com.example.backend.ExecutionService.dto.PreventiveBookingResponse;

import java.util.List;

public interface DueMaintenanceService {

    List<DueMaintenanceResponse> getDueMaintenanceAssets();

    PreventiveBookingResponse createPreventiveBooking(
            Long assetId,
            Long maintenancePlanId
    );
}