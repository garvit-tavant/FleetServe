package com.example.backend.InventoryService.service;

import com.example.backend.InventoryService.dtos.partstock.PartStockResponse;

import java.util.List;

public interface PartStockService {

    PartStockResponse getStock(
            Long partId,
            Long workshopId
    );

    List<PartStockResponse>
    getStockForWorkshop(
            Long workshopId
    );

    List<PartStockResponse>
    getStockForPart(
            Long partId
    );

    List<PartStockResponse>
    getReorderAlerts(
            Long workshopId
    );
}