package com.example.backend.SLA.service.impl;

import com.example.backend.SLA.entity.SlaPolicy;
import com.example.backend.SLA.repository.SlaPolicyRepository;
import com.example.backend.SLA.service.SlaPolicyService;

public class SlaPolicyServiceImpl implements SlaPolicyService {

    private final SlaPolicyRepository slaPolicyRepository;

    public SlaPolicyServiceImpl(SlaPolicyRepository slaPolicyRepository) {
        this.slaPolicyRepository = slaPolicyRepository;
    }

    @Override
    public void createSlaPolicy(SlaPolicy policy) {
        slaPolicyRepository.save(policy);
        System.out.println("SLA Policy created: " + policy);
    }

    @Override
    public SlaPolicy getSlaPolicy(Long id) {
        return slaPolicyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SLA Policy not found"));
    }
    
}
