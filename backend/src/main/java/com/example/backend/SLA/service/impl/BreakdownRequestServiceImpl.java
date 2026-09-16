package com.example.backend.SLA.service.impl;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.AssetManagamentService.entity.Asset;
import com.example.backend.AssetManagamentService.repository.AssetRepository;
import com.example.backend.CapacityAndSchedulingService.entity.Depot;
import com.example.backend.SLA.dto.BreakdownRequestResponse;
import com.example.backend.SLA.dto.BreakdownStatus;
import com.example.backend.SLA.dto.CreateBreakdownRequestRequest;
import com.example.backend.SLA.entity.BreakdownRequest;
import com.example.backend.SLA.entity.SlaPolicy;
import com.example.backend.SLA.mapper.BreakdownRequestMapper;
import com.example.backend.SLA.repository.BreakdownRequestRepository;
import com.example.backend.SLA.service.BreakdownRequestService;
import com.example.backend.SLA.service.SlaPolicyService;
import com.example.backend.SecurityService.entity.AppUser;
import com.example.backend.SecurityService.repository.UserRepository;
import com.example.backend.common.exception.GlobalExceptionHandler.ConflictException;

import jakarta.persistence.EntityNotFoundException;

/**
 * Handles breakdown intake, SLA-policy pinning, retrieval, and legal
 * breakdown status transitions.
 */
@Service
@Transactional(readOnly = true)
public class BreakdownRequestServiceImpl
        implements BreakdownRequestService {

    private static final int MAX_DESCRIPTION_LENGTH = 2000;

    private static final Map<
            BreakdownStatus,
            Set<BreakdownStatus>> ALLOWED_TRANSITIONS =
            createAllowedTransitions();

    private final BreakdownRequestRepository breakdownRequestRepository;
    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    private final SlaPolicyService slaPolicyService;
    private final BreakdownRequestMapper breakdownRequestMapper;
    private final Clock clock;

    public BreakdownRequestServiceImpl(
            BreakdownRequestRepository breakdownRequestRepository,
            AssetRepository assetRepository,
            UserRepository userRepository,
            SlaPolicyService slaPolicyService,
            BreakdownRequestMapper breakdownRequestMapper,
            Clock clock) {

        this.breakdownRequestRepository =
                breakdownRequestRepository;

        this.assetRepository =
                assetRepository;

        this.userRepository =
                userRepository;

        this.slaPolicyService =
                slaPolicyService;

        this.breakdownRequestMapper =
                breakdownRequestMapper;

        this.clock =
                clock;
    }

    @Override
    @Transactional
    public BreakdownRequestResponse createBreakdownRequest(
            CreateBreakdownRequestRequest request) {

        validateCreateRequest(request);

        Asset asset = assetRepository.findById(
                        request.getAssetId())
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Asset not found: "
                                        + request.getAssetId()));

        validateAssetForBreakdown(asset);

        Depot depot = asset.getHomeDepot();

        validateDepotForBreakdown(
                depot,
                asset.getId());

        AppUser reportedBy =
                getAuthenticatedActiveUser();

        OffsetDateTime reportedAt =
                OffsetDateTime.now(clock);

        SlaPolicy effectivePolicy =
                slaPolicyService.getEffectivePolicy(
                        request.getPriority(),
                        reportedAt.toLocalDate());

        if (effectivePolicy == null) {
            throw new ConflictException(
                    "No effective SLA policy was found for priority "
                            + request.getPriority()
                            + " on "
                            + reportedAt.toLocalDate());
        }

        BreakdownRequest breakdownRequest =
                new BreakdownRequest();

        breakdownRequest.setAsset(asset);
        breakdownRequest.setDepot(depot);
        breakdownRequest.setReportedBy(reportedBy);
        breakdownRequest.setReportedAt(reportedAt);
        breakdownRequest.setPriority(
                request.getPriority());
        breakdownRequest.setDescription(
                request.getDescription().trim());
        breakdownRequest.setStatus(
                BreakdownStatus.REPORTED);
        breakdownRequest.setSlaPolicy(
                effectivePolicy);

        BreakdownRequest savedBreakdown =
                breakdownRequestRepository.save(
                        breakdownRequest);

        return breakdownRequestMapper.toResponse(
                savedBreakdown);
    }

    @Override
    public BreakdownRequestResponse getBreakdownRequest(
            Long id) {

        validateBreakdownId(id);

        BreakdownRequest breakdownRequest =
                findBreakdownRequest(id);

        return breakdownRequestMapper.toResponse(
                breakdownRequest);
    }

    @Override
    @Transactional
    public BreakdownRequestResponse updateBreakdownRequestStatus(
            Long id,
            BreakdownStatus targetStatus) {

        validateBreakdownId(id);

        if (targetStatus == null) {
            throw new IllegalArgumentException(
                    "Target breakdown status is required");
        }

        BreakdownRequest breakdownRequest =
                findBreakdownRequest(id);

        BreakdownStatus currentStatus =
                breakdownRequest.getStatus();

        validateStatusTransition(
                currentStatus,
                targetStatus);

        validateTransitionSpecificRules(
                breakdownRequest,
                targetStatus);

        breakdownRequest.setStatus(
                targetStatus);

        BreakdownRequest savedBreakdown =
                breakdownRequestRepository.save(
                        breakdownRequest);

        return breakdownRequestMapper.toResponse(
                savedBreakdown);
    }

    private BreakdownRequest findBreakdownRequest(
            Long breakdownRequestId) {

        return breakdownRequestRepository
                .findById(breakdownRequestId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Breakdown request not found: "
                                        + breakdownRequestId));
    }

    private AppUser getAuthenticatedActiveUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication
                instanceof AnonymousAuthenticationToken) {

            throw new IllegalStateException(
                    "An authenticated user is required "
                            + "to create a breakdown request");
        }

        String username =
                authentication.getName();

        if (username == null
                || username.isBlank()) {

            throw new IllegalStateException(
                    "Authenticated username is unavailable");
        }

        AppUser appUser =
                userRepository.findByUsername(username)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Authenticated user was not found: "
                                                + username));

        if (!Boolean.TRUE.equals(
                appUser.getIsActive())) {

            throw new ConflictException(
                    "Inactive users cannot create "
                            + "breakdown requests");
        }

        return appUser;
    }

    private void validateCreateRequest(
            CreateBreakdownRequestRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Breakdown request is required");
        }

        if (request.getAssetId() == null) {
            throw new IllegalArgumentException(
                    "Asset ID is required");
        }

        if (request.getAssetId() <= 0L) {
            throw new IllegalArgumentException(
                    "Asset ID must be greater than zero");
        }

        if (request.getPriority() == null) {
            throw new IllegalArgumentException(
                    "Breakdown priority is required");
        }

        if (request.getDescription() == null
                || request.getDescription().isBlank()) {

            throw new IllegalArgumentException(
                    "Breakdown description is required");
        }

        String trimmedDescription =
                request.getDescription().trim();

        if (trimmedDescription.length()
                > MAX_DESCRIPTION_LENGTH) {

            throw new IllegalArgumentException(
                    "Breakdown description cannot exceed "
                            + MAX_DESCRIPTION_LENGTH
                            + " characters");
        }
    }

    private void validateAssetForBreakdown(
            Asset asset) {

        if (asset.getStatus() == null) {
            throw new ConflictException(
                    "Asset "
                            + asset.getId()
                            + " has no status");
        }

        /*
         * The exact constants in AssetStatus were not provided.
         * This check avoids creating a breakdown for a retired asset
         * without introducing a dependency on an unconfirmed constant.
         */
        if ("RETIRED".equals(
                asset.getStatus().name())) {

            throw new ConflictException(
                    "A breakdown request cannot be created "
                            + "for retired asset "
                            + asset.getId());
        }
    }

    private void validateDepotForBreakdown(
            Depot depot,
            Long assetId) {

        if (depot == null) {
            throw new ConflictException(
                    "Asset "
                            + assetId
                            + " is not assigned to a home depot");
        }

        if (!Boolean.TRUE.equals(
                depot.getActive())) {

            throw new ConflictException(
                    "The home depot for asset "
                            + assetId
                            + " is inactive");
        }
    }

    private void validateBreakdownId(
            Long breakdownRequestId) {

        if (breakdownRequestId == null) {
            throw new IllegalArgumentException(
                    "Breakdown request ID is required");
        }

        if (breakdownRequestId <= 0L) {
            throw new IllegalArgumentException(
                    "Breakdown request ID must be greater than zero");
        }
    }

    private void validateStatusTransition(
            BreakdownStatus currentStatus,
            BreakdownStatus targetStatus) {

        if (currentStatus == null) {
            throw new ConflictException(
                    "Breakdown request has no current status");
        }

        if (currentStatus == targetStatus) {
            throw new ConflictException(
                    "Breakdown request is already in status "
                            + targetStatus);
        }

        Set<BreakdownStatus> allowedTargets =
                ALLOWED_TRANSITIONS.getOrDefault(
                        currentStatus,
                        Collections.emptySet());

        if (!allowedTargets.contains(targetStatus)) {
            throw new ConflictException(
                    "Illegal breakdown status transition from "
                            + currentStatus
                            + " to "
                            + targetStatus);
        }
    }

    private void validateTransitionSpecificRules(
            BreakdownRequest breakdownRequest,
            BreakdownStatus targetStatus) {

        if (targetStatus == BreakdownStatus.BOOKED
                && breakdownRequest.getBooking() == null) {

            throw new ConflictException(
                    "Breakdown request cannot move to BOOKED "
                            + "without a resulting booking");
        }

        if (targetStatus == BreakdownStatus.RESOLVED
                && breakdownRequest.getBooking() == null) {

            throw new ConflictException(
                    "Breakdown request cannot move to RESOLVED "
                            + "without a resulting booking");
        }
    }

    private static Map<
            BreakdownStatus,
            Set<BreakdownStatus>> createAllowedTransitions() {

        Map<BreakdownStatus, Set<BreakdownStatus>> transitions =
                new EnumMap<>(BreakdownStatus.class);

        transitions.put(
                BreakdownStatus.REPORTED,
                Set.of(
                        BreakdownStatus.BOOKED,
                        BreakdownStatus.CANCELLED));

        transitions.put(
                BreakdownStatus.BOOKED,
                Set.of(
                        BreakdownStatus.RESOLVED,
                        BreakdownStatus.CANCELLED));

        transitions.put(
                BreakdownStatus.RESOLVED,
                Collections.emptySet());

        transitions.put(
                BreakdownStatus.CANCELLED,
                Collections.emptySet());

        return Collections.unmodifiableMap(
                transitions);
    }
}