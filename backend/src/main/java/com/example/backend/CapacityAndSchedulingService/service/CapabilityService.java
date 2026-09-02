package com.example.backend.CapacityAndSchedulingService.service;

import com.example.backend.CapacityAndSchedulingService.dto.capability.CapabilityResponse;
import com.example.backend.CapacityAndSchedulingService.dto.capability.CreateCapabilityRequest;

import java.util.List;

public interface CapabilityService {

    CapabilityResponse createCapability(
            CreateCapabilityRequest request
    );

    CapabilityResponse getCapability(
            String capabilityCode
    );

    List<CapabilityResponse> getAllCapabilities();
}