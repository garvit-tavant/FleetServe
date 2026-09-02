package com.example.backend.CapacityAndSchedulingService.service;

import com.example.backend.CapacityAndSchedulingService.dto.depot.CreateDepotRequest;
import com.example.backend.CapacityAndSchedulingService.dto.depot.DepotResponse;

import java.util.List;

public interface DepotService {

    DepotResponse createDepot(
            CreateDepotRequest request
    );

    DepotResponse getDepot(
            Long depotId
    );

    List<DepotResponse> getAllDepots();

    void activateDepot(
            Long depotId
    );

    void deactivateDepot(
            Long depotId
    );
}