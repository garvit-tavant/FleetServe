package com.example.backend.SLA.service.impl;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.SLA.dto.BreakdownPriority;
import com.example.backend.SLA.dto.CreateSlaPolicyRequest;
import com.example.backend.SLA.dto.SlaPolicyResponse;
import com.example.backend.SLA.entity.SlaPolicy;
import com.example.backend.SLA.mapper.SlaPolicyMapper;
import com.example.backend.SLA.repository.SlaPolicyRepository;
import com.example.backend.SLA.service.SlaPolicyService;
import com.example.backend.common.exception.GlobalExceptionHandler.ConflictException;

@Service
@Transactional
public class SlaPolicyServiceImpl
        implements SlaPolicyService {

    private final SlaPolicyRepository slaPolicyRepository;

    private final SlaPolicyMapper slaPolicyMapper;

    public SlaPolicyServiceImpl(
            SlaPolicyRepository slaPolicyRepository,
            SlaPolicyMapper slaPolicyMapper) {

        this.slaPolicyRepository =
                slaPolicyRepository;

        this.slaPolicyMapper =
                slaPolicyMapper;
    }

    @Override
    public SlaPolicyResponse createSlaPolicy(
            CreateSlaPolicyRequest request) {

        SlaPolicy policy =
                slaPolicyMapper.toEntity(request);

        validatePolicy(policy);

        List<SlaPolicy> existingPolicies =
                slaPolicyRepository.findByPriority(
                        policy.getPriority());

        LocalDate newFrom =
                policy.getEffectiveFrom();

        LocalDate newTo =
                policy.getEffectiveTo() != null
                        ? policy.getEffectiveTo()
                        : LocalDate.of(
                        9999, 12, 31);

        boolean overlapExists =
                existingPolicies.stream()
                        .anyMatch(existing -> {

                            LocalDate existingFrom =
                                    existing.getEffectiveFrom();

                            LocalDate existingTo =
                                    existing.getEffectiveTo() != null
                                            ? existing.getEffectiveTo()
                                            : LocalDate.of(
                                            9999,
                                            12,
                                            31);

                            return !(newTo.isBefore(existingFrom)
                                    || existingTo.isBefore(newFrom));
                        });

        if (overlapExists) {
            throw new ConflictException(
                    "An overlapping SLA policy already exists for priority "
                            + policy.getPriority());
        }

        SlaPolicy savedPolicy =
                slaPolicyRepository.save(policy);

        return slaPolicyMapper.toResponse(
                savedPolicy);
    }

    @Override
    @Transactional(readOnly = true)
    public SlaPolicyResponse getSlaPolicy(
            Long id) {

        SlaPolicy policy =
                slaPolicyRepository.findById(id)
                        .orElseThrow(
                                () -> new ConflictException(
                                        "SLA Policy not found: "
                                                + id));

        return slaPolicyMapper.toResponse(
                policy);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SlaPolicyResponse> getAllPolicies() {

        return slaPolicyRepository
                .findAllByOrderByPriorityAscEffectiveFromDesc()
                .stream()
                .map(slaPolicyMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SlaPolicy getEffectivePolicy(
            BreakdownPriority priority,
            LocalDate reportedDate) {

        List<SlaPolicy> candidates =
                slaPolicyRepository.findEffectivePolicies(
                        priority,
                        reportedDate);

        if (candidates.isEmpty()) {

            throw new ConflictException(
                    "No SLA policy in effect for "
                            + priority
                            + " on "
                            + reportedDate);
        }

        return candidates.stream()
                .max(
                        Comparator.comparing(
                                SlaPolicy::getEffectiveFrom))
                .orElseThrow();
    }

    private void validatePolicy(
            SlaPolicy policy) {

        if (policy == null) {
            throw new IllegalArgumentException(
                    "Policy is required");
        }

        if (policy.getPriority() == null) {
            throw new IllegalArgumentException(
                    "Priority is required");
        }

        if (policy.getCalendarBasis() == null) {
            throw new IllegalArgumentException(
                    "Calendar basis is required");
        }

        if (policy.getEffectiveFrom() == null) {
            throw new IllegalArgumentException(
                    "Effective from date is required");
        }

        if (policy.getResponseTargetMinutes() == null
                || policy.getResponseTargetMinutes() <= 0) {

            throw new IllegalArgumentException(
                    "Response target must be greater than zero");
        }

        if (policy.getResolutionTargetMinutes() == null
                || policy.getResolutionTargetMinutes() <= 0) {

            throw new IllegalArgumentException(
                    "Resolution target must be greater than zero");
        }

        if (policy.getEffectiveTo() != null
                && policy.getEffectiveTo()
                .isBefore(
                        policy.getEffectiveFrom())) {

            throw new IllegalArgumentException(
                    "EffectiveTo cannot be before EffectiveFrom");
        }
    }
}