package com.example.backend.InventoryService.controller;

import com.example.backend.InventoryService.dtos.partstock.PartStockResponse;
import com.example.backend.InventoryService.service.PartStockService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stock")
public class PartStockController {

    private final PartStockService partStockService;

    public PartStockController(
            PartStockService partStockService
    ) {
        this.partStockService = partStockService;
    }

    @GetMapping
    public PartStockResponse getStock(
            @RequestParam Long partId,
            @RequestParam Long workshopId
    ) {
        return partStockService.getStock(
                partId,
                workshopId
        );
    }

    @GetMapping("/workshop/{workshopId}")
    public List<PartStockResponse> getStockForWorkshop(
            @PathVariable Long workshopId
    ) {
        return partStockService.getStockForWorkshop(
                workshopId
        );
    }

    @GetMapping("/part/{partId}")
    public List<PartStockResponse> getStockForPart(
            @PathVariable Long partId
    ) {
        return partStockService.getStockForPart(
                partId
        );
    }

    @GetMapping("/alerts/{workshopId}")
    public List<PartStockResponse> getReorderAlerts(
            @PathVariable Long workshopId
    ) {
        return partStockService.getReorderAlerts(
                workshopId
        );
    }
}