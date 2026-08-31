package com.example.backend.AssetManagamentService.service.impl;

import com.example.backend.AssetManagamentService.dto.odometer.AddOdometerReadingRequest;
import com.example.backend.AssetManagamentService.dto.odometer.OdometerReadingResponse;
import com.example.backend.AssetManagamentService.entity.Asset;
import com.example.backend.AssetManagamentService.entity.OdometerReading;
import com.example.backend.AssetManagamentService.exception.BusinessValidationException;
import com.example.backend.AssetManagamentService.exception.ResourceNotFoundException;
import com.example.backend.AssetManagamentService.mapper.AssetManagementMapper;
import com.example.backend.AssetManagamentService.port.CurrentUserProvider;
import com.example.backend.AssetManagamentService.repository.AssetRepository;
import com.example.backend.AssetManagamentService.repository.OdometerReadingRepository;
import com.example.backend.AssetManagamentService.service.OdometerService;
import com.example.backend.AssetManagamentService.source.OdometerSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class OdometerServiceImpl implements OdometerService {

    private final AssetRepository assetRepository;
    private final OdometerReadingRepository readingRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AssetManagementMapper mapper;

    public OdometerServiceImpl(
            AssetRepository assetRepository,
            OdometerReadingRepository readingRepository,
            CurrentUserProvider currentUserProvider,
            AssetManagementMapper mapper
    ) {
        this.assetRepository = assetRepository;
        this.readingRepository = readingRepository;
        this.currentUserProvider = currentUserProvider;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public OdometerReadingResponse addReading(
            Long assetId,
            AddOdometerReadingRequest request
    ) {
        validateRequest(request);

        Asset asset = assetRepository
                .findAssetForOdometerUpdate(assetId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Asset not found with id " + assetId
                        )
                );

        BigDecimal currentReading = readingRepository
                .findFirstByAsset_IdOrderByReadAtDesc(assetId)
                .map(OdometerReading::getReadingKm)
                .orElse(asset.getAcquisitionOdometerKm());

        if (request.getReadingKm().compareTo(currentReading) < 0) {
            throw new BusinessValidationException(
                    "Odometer reading cannot be lower than "
                            + "the current value. Current value: "
                            + currentReading
            );
        }

        OdometerReading reading = new OdometerReading();

        reading.setAsset(asset);
        reading.setReadingKm(request.getReadingKm());
        reading.setReadAt(
                OffsetDateTime.now(ZoneOffset.UTC)
        );
        reading.setSource(OdometerSource.MANUAL);
        reading.setRecordedById(
                currentUserProvider.getCurrentUserId()
        );

        OdometerReading saved =
                readingRepository.save(reading);

        return mapper.toOdometerResponse(saved);
    }

    @Override
    public List<OdometerReadingResponse> getHistory(Long assetId) {
        getAssetOrThrow(assetId);

        return readingRepository
                .findByAsset_IdOrderByReadAtDesc(assetId)
                .stream()
                .map(mapper::toOdometerResponse)
                .toList();
    }

    @Override
    public OdometerReadingResponse getLatestReading(
            Long assetId
    ) {
        getAssetOrThrow(assetId);

        OdometerReading reading = readingRepository
                .findFirstByAsset_IdOrderByReadAtDesc(assetId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No odometer reading found for asset "
                                        + assetId
                        )
                );

        return mapper.toOdometerResponse(reading);
    }

    private Asset getAssetOrThrow(Long assetId) {
        return assetRepository.findById(assetId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Asset not found with id "
                                        + assetId
                        )
                );
    }

    private void validateRequest(
            AddOdometerReadingRequest request
    ) {
        if (request == null
                || request.getReadingKm() == null) {
            throw new BusinessValidationException(
                    "Odometer reading is required"
            );
        }

        if (request.getReadingKm()
                .compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessValidationException(
                    "Odometer reading cannot be negative"
            );
        }
    }
}