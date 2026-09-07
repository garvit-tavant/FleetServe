package com.example.backend.InventoryService.controller;

import com.example.backend.InventoryService.dtos.inventorymovement.*;
import com.example.backend.InventoryService.service.InventoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(
            InventoryService inventoryService
    ) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/receive")
    public InventoryMovementResponse receiveStock(
            @RequestBody ReceiveStockRequest request
    ) {
        return inventoryService.receiveStock(request);
    }

    @PostMapping("/issue")
    public InventoryMovementResponse issuePart(
            @RequestBody IssuePartRequest request
    ) {
        return inventoryService.issuePart(request);
    }

    @PostMapping("/return")
    public InventoryMovementResponse returnPart(
            @RequestBody ReturnPartRequest request
    ) {
        return inventoryService.returnPart(request);
    }

    @PostMapping("/transfer")
    public void transferStock(
            @RequestBody TransferStockRequest request
    ) {
        inventoryService.transferStock(request);
    }

    @PostMapping("/adjust")
    public InventoryMovementResponse adjustStock(
            @RequestBody AdjustStockRequest request
    ) {
        return inventoryService.adjustStock(request);
    }

    @GetMapping("/movements/part/{partId}")
    public Page<InventoryMovementResponse>
    getMovementHistoryForPart(
            @PathVariable Long partId,
            Pageable pageable
    ) {
        return inventoryService
                .getMovementHistoryForPart(
                        partId,
                        pageable
                );
    }

    @GetMapping("/movements/workshop/{workshopId}")
    public Page<InventoryMovementResponse>
    getMovementHistoryForWorkshop(
            @PathVariable Long workshopId,
            Pageable pageable
    ) {
        return inventoryService
                .getMovementHistoryForWorkshop(
                        workshopId,
                        pageable
                );
    }

    @GetMapping("/movements")
    public Page<InventoryMovementResponse>
    getMovementHistoryForPartAndWorkshop(
            @RequestParam Long partId,
            @RequestParam Long workshopId,
            Pageable pageable
    ) {
        return inventoryService
                .getMovementHistoryForPartAndWorkshop(
                        partId,
                        workshopId,
                        pageable
                );
    }
}