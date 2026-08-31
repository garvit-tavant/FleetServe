package com.example.backend.AssetManagamentService.port;

import java.util.List;

public interface AssetRetirementBlockerPort {

    List<String> findOpenWorkOrderReferences(Long assetId);

    List<String> findFutureBookingReferences(Long assetId);
}