package com.example.backend.SLA.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.AssetManagamentService.entity.Asset;
import com.example.backend.AssetManagamentService.exception.BusinessValidationException;
import com.example.backend.AssetManagamentService.exception.ResourceNotFoundException;
import com.example.backend.AssetManagamentService.repository.AssetRepository;
import com.example.backend.AssetManagamentService.status.AssetStatus;
import com.example.backend.CapacityAndSchedulingService.entity.Capability;
import com.example.backend.CapacityAndSchedulingService.entity.Skill;
import com.example.backend.CapacityAndSchedulingService.repository.CapabilityRepository;
import com.example.backend.CapacityAndSchedulingService.repository.SkillRepository;
import com.example.backend.SLA.dto.breakdown.BreakdownResponse;
import com.example.backend.SLA.dto.breakdown.RaiseBreakdownRequest;
import com.example.backend.SLA.entity.BreakdownRequest;
import com.example.backend.SLA.entity.SlaCheckpoint;
import com.example.backend.SLA.entity.SlaPolicy;
import com.example.backend.SLA.repository.BreakdownRequestRepository;
import com.example.backend.SLA.repository.SlaCheckpointRepository;
import com.example.backend.SLA.repository.SlaPolicyRepository;
import com.example.backend.SLA.service.BreakdownIntakeService;
import com.example.backend.SLA.status.BreakdownStatus;
import com.example.backend.SecurityService.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class BreakdownIntakeServiceImpl implements BreakdownIntakeService {

    /** Matches ck_breakdown_estimated_duration and ck_booking_max_duration. */
    private static final long MAX_JOB_MINUTES = 1440;

    private final Clock clock;
    private final BreakdownRequestRepository breakdownRequestRepository;
    private final SlaPolicyRepository slaPolicyRepository;
    private final SlaCheckpointRepository slaCheckpointRepository;
    private final AssetRepository assetRepository;
    private final SkillRepository skillRepository;
    private final CapabilityRepository capabilityRepository;
    private final UserRepository userRepository;

    public BreakdownIntakeServiceImpl(
            Clock clock,
            BreakdownRequestRepository breakdownRequestRepository,
            SlaPolicyRepository slaPolicyRepository,
            SlaCheckpointRepository slaCheckpointRepository,
            AssetRepository assetRepository,
            SkillRepository skillRepository,
            CapabilityRepository capabilityRepository,
            UserRepository userRepository) {
        this.clock = clock;
        this.breakdownRequestRepository = breakdownRequestRepository;
        this.slaPolicyRepository = slaPolicyRepository;
        this.slaCheckpointRepository = slaCheckpointRepository;
        this.assetRepository = assetRepository;
        this.skillRepository = skillRepository;
        this.capabilityRepository = capabilityRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public BreakdownResponse raiseBreakdown(RaiseBreakdownRequest request, Long reportedById) {
        if (reportedById == null) {
            throw new BusinessValidationException(
                    "A breakdown must record who reported it");
        }
        if (!userRepository.existsById(reportedById)) {
            throw new ResourceNotFoundException("User not found with id " + reportedById);
        }

        Asset asset = assetRepository.findById(request.assetId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Asset not found with id " + request.assetId()));

        if (asset.getStatus() == AssetStatus.RETIRED) {
            throw new BusinessValidationException(
                    "Asset " + asset.getVin() + " is retired and cannot have a breakdown raised");
        }

        LocalDate today = LocalDate.now(clock);

        // The policy version is pinned now, so revising a target later cannot
        // retrospectively rewrite what this breakdown was promised.
        SlaPolicy policy = slaPolicyRepository
                .findEffectivePolicy(request.priority(), today)
                .orElseThrow(() -> new BusinessValidationException(
                        "No service-level policy is in force for priority "
                                + request.priority() + " on " + today));

        BreakdownRequest breakdown = new BreakdownRequest();
        breakdown.setAssetId(asset.getId());
        // The depot comes from the asset, so a breakdown cannot be filed against
        // a depot that has nothing to do with the vehicle.
        breakdown.setDepotId(asset.getHomeDepot().getId());
        breakdown.setReportedById(reportedById);
        breakdown.setReportedAt(OffsetDateTime.now(clock));
        breakdown.setPriority(request.priority());
        breakdown.setDescription(request.description().trim());
        breakdown.setStatus(BreakdownStatus.REPORTED);
        breakdown.setSlaPolicy(policy);

        applyTriageFields(breakdown, request);

        BreakdownRequest saved = breakdownRequestRepository.save(breakdown);

        // Every service-level write is an UPDATE against this row. Without it the
        // clocks would silently update nothing and no breach would ever register.
        SlaCheckpoint checkpoint = new SlaCheckpoint();
        checkpoint.setBreakdownRequest(saved);
        slaCheckpointRepository.save(checkpoint);

        return toResponse(saved, asset.getVin());
    }

    private void applyTriageFields(BreakdownRequest breakdown, RaiseBreakdownRequest request) {
        Skill skill = null;
        if (request.requiredSkillCode() != null) {
            skill = skillRepository.findById(request.requiredSkillCode())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Skill not found with code " + request.requiredSkillCode()));
            breakdown.setRequiredSkillCode(skill.getSkillCode());
        }

        if (request.requiredCapabilityCode() != null) {
            Capability capability = capabilityRepository
                    .findById(request.requiredCapabilityCode())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Capability not found with code " + request.requiredCapabilityCode()));
            breakdown.setRequiredCapabilityCode(capability.getCapabilityCode());
        }

        Integer duration = request.estimatedDurationMinutes();
        if (duration == null && skill != null) {
            // Fall back to the skill's standard time, pinned from here on.
            Long standard = skill.getTime();
            if (standard != null && standard > 0 && standard <= MAX_JOB_MINUTES) {
                duration = standard.intValue();
            }
        }
        breakdown.setEstimatedDurationMinutes(duration);
    }

    @Override
    public BreakdownResponse getBreakdown(long breakdownRequestId) {
        BreakdownRequest breakdown = breakdownRequestRepository.findById(breakdownRequestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Breakdown request not found with id " + breakdownRequestId));
        return toResponse(breakdown, null);
    }

    @Override
    public Page<BreakdownResponse> listBreakdowns(BreakdownStatus status, Pageable pageable) {
        Pageable effective = PageRequest.of(
                pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), MAX_PAGE_SIZE));

        Page<BreakdownRequest> page = status == null
                ? breakdownRequestRepository.findAllByOrderByReportedAtDesc(effective)
                : breakdownRequestRepository
                        .findByStatusOrderByReportedAtDesc(status, effective);

        return page.map(breakdown -> toResponse(breakdown, null));
    }

    private BreakdownResponse toResponse(BreakdownRequest breakdown, String assetVin) {
        SlaPolicy policy = breakdown.getSlaPolicy();

        return new BreakdownResponse(
                breakdown.getId(),
                breakdown.getAssetId(),
                assetVin,
                breakdown.getDepotId(),
                breakdown.getReportedById(),
                breakdown.getReportedAt(),
                breakdown.getPriority(),
                breakdown.getDescription(),
                breakdown.getStatus(),
                policy == null ? null : policy.getId(),
                policy == null ? null : policy.getResponseTargetMinutes(),
                policy == null ? null : policy.getResolutionTargetMinutes(),
                breakdown.getRequiredSkillCode(),
                breakdown.getRequiredCapabilityCode(),
                breakdown.getEstimatedDurationMinutes());
    }
}
