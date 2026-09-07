package com.example.backend.InventoryService.controller;

import com.example.backend.InventoryService.dtos.partreorderlevel.CreateOrUpdatePartReorderLevelRequest;
import com.example.backend.InventoryService.dtos.partreorderlevel.PartReorderLevelResponse;
import com.example.backend.InventoryService.service.PartReorderLevelService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reorder-levels")
public class PartReorderLevelController {

    private final PartReorderLevelService partReorderLevelService;

    public PartReorderLevelController(
            PartReorderLevelService partReorderLevelService
    ) {
        this.partReorderLevelService = partReorderLevelService;
    }

    @PostMapping
    public PartReorderLevelResponse createReorderLevel(
            @RequestBody CreateOrUpdatePartReorderLevelRequest request
    ) {
        return partReorderLevelService.createReorderLevel(
                request
        );
    }

    @PutMapping("/{id}")
    public PartReorderLevelResponse updateReorderLevel(
            @PathVariable Long id,
            @RequestBody CreateOrUpdatePartReorderLevelRequest request,
            @RequestParam Long version
    ) {
        return partReorderLevelService.updateReorderLevel(
                id,
                request,
                version
        );
    }

    @GetMapping("/{id}")
    public PartReorderLevelResponse getById(
            @PathVariable Long id
    ) {
        return partReorderLevelService.getById(id);
    }

    @GetMapping("/part/{partId}")
    public List<PartReorderLevelResponse> getByPart(
            @PathVariable Long partId
    ) {
        return partReorderLevelService.getByPart(
                partId
        );
    }

    @GetMapping("/workshop/{workshopId}")
    public List<PartReorderLevelResponse> getByWorkshop(
            @PathVariable Long workshopId
    ) {
        return partReorderLevelService.getByWorkshop(
                workshopId
        );
    }

    @DeleteMapping("/{id}")
    public void deleteReorderLevel(
            @PathVariable Long id,
            @RequestParam Long version
    ) {
        partReorderLevelService.deleteReorderLevel(
                id,
                version
        );
    }
}