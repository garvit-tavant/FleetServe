package com.example.backend.InventoryService.service;

import com.example.backend.InventoryService.dtos.partreorderlevel.CreateOrUpdatePartReorderLevelRequest;
import com.example.backend.InventoryService.dtos.partreorderlevel.PartReorderLevelResponse;

import java.util.List;

public interface PartReorderLevelService {

    PartReorderLevelResponse createReorderLevel(
            CreateOrUpdatePartReorderLevelRequest request
    );

    PartReorderLevelResponse updateReorderLevel(
            Long id,
            CreateOrUpdatePartReorderLevelRequest request,
            Long version
    );

    PartReorderLevelResponse getById(
            Long id
    );

    List<PartReorderLevelResponse> getByPart(
            Long partId
    );

    List<PartReorderLevelResponse> getByWorkshop(
            Long workshopId
    );

    void deleteReorderLevel(
            Long id,
            Long version
    );
}