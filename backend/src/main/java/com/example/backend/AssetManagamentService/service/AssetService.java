package com.example.backend.AssetManagamentService.service;

import com.example.backend.AssetManagamentService.dto.asset.*;

import java.util.List;

public interface AssetService {

    AssetResponse registerAsset(
            RegisterAssetRequest request
    );

    AssetResponse getAsset(
            Long assetId
    );

    List<AssetSummaryResponse> getAllAssets();

    void retireAsset(
            Long assetId,
            RetireAssetRequest request
    );

    void reinstateAsset(
            Long assetId,
            ReinstateAssetRequest request
    );
}
