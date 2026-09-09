package com.example.backend.SLA.service;

import java.time.LocalDate;

import com.example.backend.SLA.entity.SlaPolicy;
import com.example.backend.SLA.dto.BreakdownPriority;

public interface SlaPolicyService {

    void createSlaPolicy(SlaPolicy policy);
    SlaPolicy getSlaPolicy(Long id);
    SlaPolicy getEffectivePolicy(BreakdownPriority priority, LocalDate reportedDate);

}
