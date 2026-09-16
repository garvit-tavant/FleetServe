package com.example.backend.SLA.service;

import java.time.LocalDate;
import java.util.List;

import com.example.backend.SLA.dto.CreateSlaPolicyRequest;
import com.example.backend.SLA.dto.SlaPolicyResponse;
import com.example.backend.SLA.entity.SlaPolicy;
import com.example.backend.SLA.dto.BreakdownPriority;

public interface SlaPolicyService {

    SlaPolicyResponse createSlaPolicy(
            CreateSlaPolicyRequest request);

    SlaPolicyResponse getSlaPolicy(Long id);

    List<SlaPolicyResponse> getAllPolicies();

    SlaPolicy getEffectivePolicy(
            BreakdownPriority priority,
            LocalDate reportedDate);

}
