package com.example.backend.AssetManagamentService.service.impl;

import com.example.backend.AssetManagamentService.dto.maintenanceplan.CreateMaintenancePlanRequest;
import com.example.backend.AssetManagamentService.dto.maintenanceplan.MaintenancePlanResponse;
import com.example.backend.AssetManagamentService.entity.MaintenancePlan;
import com.example.backend.AssetManagamentService.exception.BusinessValidationException;
import com.example.backend.AssetManagamentService.exception.DuplicateResourceException;
import com.example.backend.AssetManagamentService.exception.ResourceNotFoundException;
import com.example.backend.AssetManagamentService.mapper.AssetManagementMapper;
import com.example.backend.AssetManagamentService.repository.MaintenancePlanRepository;
import com.example.backend.AssetManagamentService.service.MaintenancePlanService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class MaintenancePlanServiceImpl
        implements MaintenancePlanService {

    private final MaintenancePlanRepository planRepository;
    private final AssetManagementMapper mapper;

    public MaintenancePlanServiceImpl(
            MaintenancePlanRepository planRepository,
            AssetManagementMapper mapper
    ) {
        this.planRepository = planRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public MaintenancePlanResponse createPlan(
            CreateMaintenancePlanRequest request
    ) {
        validateRequest(request);

        String normalizedCode =
                request.getCode().trim().toUpperCase();

        if (planRepository.existsByCode(normalizedCode)) {
            throw new DuplicateResourceException(
                    "Maintenance plan with code "
                            + normalizedCode
                            + " already exists"
            );
        }

        MaintenancePlan plan = new MaintenancePlan();

        plan.setCode(normalizedCode);
        plan.setDistanceIntervalKm(
                request.getDistanceIntervalKm()
        );
        plan.setTimeIntervalDays(
                request.getTimeIntervalDays()
        );
        plan.setEstimatedDurationMinutes(
                request.getEstimatedDurationMinutes()
        );
        plan.setRequiredSkillCode(
                normalizeNullableCode(
                        request.getRequiredSkillCode()
                )
        );
        plan.setRequiredCapabilityCode(
                normalizeNullableCode(
                        request.getRequiredCapabilityCode()
                )
        );

        MaintenancePlan saved = planRepository.save(plan);

        return mapper.toMaintenancePlanResponse(saved);
    }

    @Override
    public MaintenancePlanResponse getPlan(Long planId) {
        MaintenancePlan plan = planRepository.findById(planId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Maintenance plan not found with id "
                                        + planId
                        )
                );

        return mapper.toMaintenancePlanResponse(plan);
    }

    @Override
    public List<MaintenancePlanResponse> getAllPlans() {
        return planRepository.findAll()
                .stream()
                .map(mapper::toMaintenancePlanResponse)
                .toList();
    }

    private void validateRequest(
            CreateMaintenancePlanRequest request
    ) {
        if (request == null) {
            throw new BusinessValidationException(
                    "Maintenance-plan request is required"
            );
        }

        if (request.getCode() == null
                || request.getCode().isBlank()) {
            throw new BusinessValidationException(
                    "Maintenance-plan code is required"
            );
        }

        BigDecimal distance =
                request.getDistanceIntervalKm();

        Integer days =
                request.getTimeIntervalDays();

        if (distance == null && days == null) {
            throw new BusinessValidationException(
                    "At least one maintenance interval is required"
            );
        }

        if (distance != null
                && distance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException(
                    "Distance interval must be greater than zero"
            );
        }

        if (days != null && days <= 0) {
            throw new BusinessValidationException(
                    "Time interval must be greater than zero"
            );
        }

        if (request.getEstimatedDurationMinutes() == null
                || request.getEstimatedDurationMinutes() <= 0) {
            throw new BusinessValidationException(
                    "Estimated duration must be greater than zero"
            );
        }
    }

    private String normalizeNullableCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }

        return code.trim().toUpperCase();
    }
}