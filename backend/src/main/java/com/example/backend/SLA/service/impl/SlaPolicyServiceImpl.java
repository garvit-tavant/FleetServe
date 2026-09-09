package com.example.backend.SLA.service.impl;

import java.time.LocalDate;

import com.example.backend.SLA.entity.SlaPolicy;
import com.example.backend.SLA.repository.SlaPolicyRepository;
import com.example.backend.SLA.service.SlaPolicyService;
import com.example.backend.SLA.dto.BreakdownPriority;
import com.example.backend.common.exception.GlobalExceptionHandler.ConflictException;
import java.util.List;
import java.util.Comparator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service 
@Transactional 
public class SlaPolicyServiceImpl implements SlaPolicyService {

    private final SlaPolicyRepository slaPolicyRepository;

    public SlaPolicyServiceImpl(SlaPolicyRepository slaPolicyRepository) {
        this.slaPolicyRepository = slaPolicyRepository;
    }

    @Override
    @Transactional
    public void createSlaPolicy(SlaPolicy policy) {
        slaPolicyRepository.save(policy);
    }

    @Override
    public SlaPolicy getSlaPolicy(Long id) {
        return slaPolicyRepository.findById(id)
                .orElseThrow(() -> new ConflictException("SLA Policy not found: " + id));
    }

    @Override
    public SlaPolicy getEffectivePolicy(BreakdownPriority priority, LocalDate reportedDate) {
        List<SlaPolicy> candidates =
                slaPolicyRepository.findEffectivePolicies(priority, reportedDate);

        if (candidates.isEmpty()) {
            throw new ConflictException(
                    "No SLA policy in effect for " + priority + " on " + reportedDate);
        }
        // Deterministic tie-break: latest effectiveFrom wins. Overlapping windows
        // are a configuration defect that should be caught at save time (open-questions #E).
        return candidates.stream()
                .max(Comparator.comparing(SlaPolicy::getEffectiveFrom))
                .orElseThrow();
    }
}
