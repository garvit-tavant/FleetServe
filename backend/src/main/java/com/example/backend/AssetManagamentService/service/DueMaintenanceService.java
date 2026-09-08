package com.example.backend.AssetManagamentService.service;

import com.example.backend.AssetManagamentService.dto.duemaintenance.DueMaintenanceFilter;
import com.example.backend.AssetManagamentService.dto.duemaintenance.DueMaintenanceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DueMaintenanceService {

    /** Largest page the server will return, per the pagination rule. */
    int MAX_PAGE_SIZE = 100;

    /**
     * Preventive maintenance due list (US-1.3), evaluated against the injected
     * clock and each plan's stored DUE_SOON thresholds.
     */
    Page<DueMaintenanceResponse> getDueMaintenance(
            DueMaintenanceFilter filter,
            Pageable pageable
    );
}
