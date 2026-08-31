package com.example.backend.AssetManagamentService.service;

import com.example.backend.AssetManagamentService.dto.odometer.AddOdometerReadingRequest;
import com.example.backend.AssetManagamentService.dto.odometer.OdometerReadingResponse;

import java.util.List;

public interface OdometerService {

    OdometerReadingResponse addReading(
            Long assetId,
            AddOdometerReadingRequest request
    );

    List<OdometerReadingResponse> getHistory(
            Long assetId
    );

    OdometerReadingResponse getLatestReading(
            Long assetId
    );
}