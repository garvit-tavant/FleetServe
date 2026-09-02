package com.example.backend.CapacityAndSchedulingService.service;

import com.example.backend.CapacityAndSchedulingService.dto.bay.BayResponse;
import com.example.backend.CapacityAndSchedulingService.dto.bay.CreateBayRequest;

import java.util.List;

public interface BayService {

    BayResponse createBay(
            Long workshopId,
            CreateBayRequest request
    );

    BayResponse getBay(
            Long bayId
    );

    List<BayResponse> getBaysByWorkshop(
            Long workshopId
    );

    void activateBay(
            Long bayId
    );

    void deactivateBay(
            Long bayId
    );
}