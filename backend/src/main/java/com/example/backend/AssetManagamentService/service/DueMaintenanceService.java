package com.example.backend.AssetManagamentService.service;

import com.example.backend.AssetManagamentService.dto.duemaintenance.DueMaintenanceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface DueMaintenanceService {

    Page<DueMaintenanceResponse> getDueMaintenance(
            BigDecimal distanceSoonThreshold,
            Integer timeSoonThresholdDays,
            Pageable pageable
    );
}