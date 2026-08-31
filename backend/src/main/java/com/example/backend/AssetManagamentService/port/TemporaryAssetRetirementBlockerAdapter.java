package com.example.backend.AssetManagamentService.port;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class TemporaryAssetRetirementBlockerAdapter
        implements AssetRetirementBlockerPort {

    @Override
    public List<String> findOpenWorkOrderReferences(Long assetId) {
        return Collections.emptyList();
    }

    @Override
    public List<String> findFutureBookingReferences(Long assetId) {
        return Collections.emptyList();
    }
}