package com.example.backend.SLA.service;

import com.example.backend.SLA.entity.SlaPolicy;

public interface SlaPolicyService {

    void createSlaPolicy(SlaPolicy policy);

    SlaPolicy getSlaPolicy(Long id);

  
}
