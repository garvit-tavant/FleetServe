package com.example.backend.AssetManagamentService.service;

import com.example.backend.AssetManagamentService.dto.assetclass.AssetClassResponse;
import com.example.backend.AssetManagamentService.dto.assetclass.CreateAssetClassRequest;

import java.util.List;

public interface AssetClassService {

    AssetClassResponse createAssetClass(
            CreateAssetClassRequest request
    );

    AssetClassResponse getAssetClass(
            Long assetClassId
    );

    List<AssetClassResponse> getAllAssetClasses();
}
