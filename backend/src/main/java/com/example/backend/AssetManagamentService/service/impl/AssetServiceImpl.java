package com.example.backend.AssetManagamentService.service.impl;

import com.example.backend.AssetManagamentService.dto.asset.AssetResponse;
import com.example.backend.AssetManagamentService.dto.asset.AssetSummaryResponse;
import com.example.backend.AssetManagamentService.dto.asset.RegisterAssetRequest;
import com.example.backend.AssetManagamentService.dto.asset.ReinstateAssetRequest;
import com.example.backend.AssetManagamentService.dto.asset.RetireAssetRequest;
import com.example.backend.AssetManagamentService.entity.Asset;
import com.example.backend.AssetManagamentService.entity.AssetClass;
import com.example.backend.AssetManagamentService.entity.OdometerReading;
import com.example.backend.AssetManagamentService.exception.AssetRetirementBlockedException;
import com.example.backend.AssetManagamentService.exception.BusinessValidationException;
import com.example.backend.AssetManagamentService.exception.DuplicateResourceException;
import com.example.backend.AssetManagamentService.exception.ResourceNotFoundException;
import com.example.backend.AssetManagamentService.mapper.AssetManagementMapper;
import com.example.backend.AssetManagamentService.port.AssetRetirementBlockerPort;
import com.example.backend.AssetManagamentService.port.CurrentUserProvider;
import com.example.backend.AssetManagamentService.repository.AssetClassRepository;
import com.example.backend.AssetManagamentService.repository.AssetRepository;
import com.example.backend.AssetManagamentService.repository.OdometerReadingRepository;
import com.example.backend.AssetManagamentService.service.AssetService;
import com.example.backend.AssetManagamentService.source.OdometerSource;
import com.example.backend.AssetManagamentService.status.AssetStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AssetServiceImpl implements AssetService {

    private final AssetRepository assetRepository;
    private final AssetClassRepository assetClassRepository;
    private final OdometerReadingRepository odometerRepository;
    private final AssetRetirementBlockerPort retirementBlockerPort;
    private final CurrentUserProvider currentUserProvider;
    private final AssetManagementMapper mapper;

    public AssetServiceImpl(
            AssetRepository assetRepository,
            AssetClassRepository assetClassRepository,
            OdometerReadingRepository odometerRepository,
            AssetRetirementBlockerPort retirementBlockerPort,
            CurrentUserProvider currentUserProvider,
            AssetManagementMapper mapper
    ) {
        this.assetRepository = assetRepository;
        this.assetClassRepository = assetClassRepository;
        this.odometerRepository = odometerRepository;
        this.retirementBlockerPort = retirementBlockerPort;
        this.currentUserProvider = currentUserProvider;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public AssetResponse registerAsset(
            RegisterAssetRequest request
    ) {
        validateRegisterRequest(request);

        String normalizedVin =
                request.getVin().trim().toUpperCase();

        if (assetRepository.existsByVin(normalizedVin)) {
            throw new DuplicateResourceException(
                    "Asset with VIN or serial number "
                            + normalizedVin
                            + " already exists"
            );
        }

        AssetClass assetClass = assetClassRepository
                .findById(request.getAssetClassId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Asset class not found with id "
                                        + request.getAssetClassId()
                        )
                );

        Asset asset = new Asset();

        asset.setVin(normalizedVin);
        asset.setAssetClass(assetClass);
        asset.setHomeDepotId(request.getHomeDepotId());
        asset.setAcquisitionDate(
                request.getAcquisitionDate()
        );
        asset.setAcquisitionOdometerKm(
                request.getAcquisitionOdometerKm()
        );
        asset.setStatus(AssetStatus.ACTIVE);

        Asset savedAsset = assetRepository.save(asset);

        OdometerReading initialReading =
                new OdometerReading();

        initialReading.setAsset(savedAsset);
        initialReading.setReadingKm(
                request.getAcquisitionOdometerKm()
        );
        initialReading.setReadAt(
                request.getAcquisitionDate()
                        .atStartOfDay()
                        .atOffset(ZoneOffset.UTC)
        );
        initialReading.setSource(OdometerSource.IMPORT);
        initialReading.setRecordedById(
                currentUserProvider.getCurrentUserId()
        );

        odometerRepository.save(initialReading);

        return mapper.toAssetResponse(savedAsset);
    }

    @Override
    public AssetResponse getAsset(Long assetId) {
        Asset asset = findAssetOrThrow(assetId);
        return mapper.toAssetResponse(asset);
    }

    @Override
    public List<AssetSummaryResponse> getAllAssets() {
        return assetRepository.findAll()
                .stream()
                .map(mapper::toAssetSummaryResponse)
                .toList();
    }

    @Override
    @Transactional
    public void retireAsset(
            Long assetId,
            RetireAssetRequest request
    ) {
        validateReason(
                request == null ? null : request.getReason(),
                "Retirement reason is required"
        );

        Asset asset = findAssetOrThrow(assetId);

        if (asset.getStatus() == AssetStatus.RETIRED) {
            throw new BusinessValidationException(
                    "Asset is already retired"
            );
        }

        List<String> blockers = new ArrayList<>();

        blockers.addAll(
                retirementBlockerPort
                        .findOpenWorkOrderReferences(assetId)
        );

        blockers.addAll(
                retirementBlockerPort
                        .findFutureBookingReferences(assetId)
        );

        if (!blockers.isEmpty()) {
            throw new AssetRetirementBlockedException(
                    "Asset cannot be retired because blocking "
                            + "work orders or future bookings exist",
                    blockers
            );
        }

        asset.setStatus(AssetStatus.RETIRED);
        assetRepository.save(asset);

        /*
         * Required next:
         * write a proper asset audit record containing:
         * assetId, RETIRE event, actor, reason and timestamp.
         *
         * Your current schema does not contain an asset_audit table,
         * so this side effect cannot yet be persisted correctly.
         */
    }

    @Override
    @Transactional
    public void reinstateAsset(
            Long assetId,
            ReinstateAssetRequest request
    ) {
        validateReason(
                request == null ? null : request.getReason(),
                "Reinstatement reason is required"
        );

        Asset asset = findAssetOrThrow(assetId);

        if (asset.getStatus() != AssetStatus.RETIRED) {
            throw new BusinessValidationException(
                    "Only a retired asset can be reinstated"
            );
        }

        asset.setStatus(AssetStatus.ACTIVE);
        assetRepository.save(asset);

        /*
         * Required next:
         * write an audited REINSTATE event.
         */
    }

    private Asset findAssetOrThrow(Long assetId) {
        return assetRepository.findById(assetId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Asset not found with id "
                                        + assetId
                        )
                );
    }

    private void validateRegisterRequest(
            RegisterAssetRequest request
    ) {
        if (request == null) {
            throw new BusinessValidationException(
                    "Asset registration request is required"
            );
        }

        if (request.getVin() == null
                || request.getVin().isBlank()) {
            throw new BusinessValidationException(
                    "VIN or serial number is required"
            );
        }

        if (request.getAssetClassId() == null) {
            throw new BusinessValidationException(
                    "Asset class is required"
            );
        }

        if (request.getHomeDepotId() == null) {
            throw new BusinessValidationException(
                    "Home depot is required"
            );
        }

        if (request.getAcquisitionDate() == null) {
            throw new BusinessValidationException(
                    "Acquisition date is required"
            );
        }

        if (request.getAcquisitionDate()
                .isAfter(LocalDate.now())) {
            throw new BusinessValidationException(
                    "Acquisition date cannot be in the future"
            );
        }

        BigDecimal odometer =
                request.getAcquisitionOdometerKm();

        if (odometer == null) {
            throw new BusinessValidationException(
                    "Acquisition odometer is required"
            );
        }

        if (odometer.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessValidationException(
                    "Acquisition odometer cannot be negative"
            );
        }
    }

    private void validateReason(
            String reason,
            String errorMessage
    ) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessValidationException(errorMessage);
        }
    }
}