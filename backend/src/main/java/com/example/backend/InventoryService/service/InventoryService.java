package com.example.backend.InventoryService.service;

import com.example.backend.InventoryService.dtos.inventorymovement.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InventoryService {

    InventoryMovementResponse receiveStock(
            ReceiveStockRequest request
    );

    InventoryMovementResponse issuePart(
            IssuePartRequest request
    );

    InventoryMovementResponse returnPart(
            ReturnPartRequest request
    );

    void transferStock(
            TransferStockRequest request
    );

    InventoryMovementResponse adjustStock(
            AdjustStockRequest request
    );

    Page<InventoryMovementResponse>
    getMovementHistoryForPart(
            Long partId,
            Pageable pageable
    );

    Page<InventoryMovementResponse>
    getMovementHistoryForWorkshop(
            Long workshopId,
            Pageable pageable
    );

    Page<InventoryMovementResponse>
    getMovementHistoryForPartAndWorkshop(
            Long partId,
            Long workshopId,
            Pageable pageable
    );
}