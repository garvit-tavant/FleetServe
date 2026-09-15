package com.example.backend.CapacityAndSchedulingService.service;

import com.example.backend.CapacityAndSchedulingService.dto.bayCapability.BayCapabilityResponse;

import java.util.List;

public interface BayCapabilityService {

    void assignCapabilityToBay(
            Long bayId,
            String capabilityCode
    );

    void removeCapabilityFromBay(
            Long bayId,
            String capabilityCode
    );

    List<BayCapabilityResponse> getCapabilitiesForBay(
            Long bayId
    );
}