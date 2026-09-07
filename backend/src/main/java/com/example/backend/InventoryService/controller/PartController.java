package com.example.backend.InventoryService.controller;

import com.example.backend.InventoryService.dtos.part.CreatePartRequest;
import com.example.backend.InventoryService.dtos.part.PartResponse;
import com.example.backend.InventoryService.dtos.part.UpdatePartRequest;
import com.example.backend.InventoryService.service.PartService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/parts")
public class PartController {

    private final PartService partService;

    public PartController(
            PartService partService
    ) {
        this.partService = partService;
    }

    @PostMapping
    public PartResponse createPart(
            @RequestBody CreatePartRequest request
    ) {
        return partService.createPart(request);
    }

    @PutMapping("/{partId}")
    public PartResponse updatePart(
            @PathVariable Long partId,
            @RequestBody UpdatePartRequest request
    ) {
        return partService.updatePart(
                partId,
                request
        );
    }

    @GetMapping("/{partId}")
    public PartResponse getPartById(
            @PathVariable Long partId
    ) {
        return partService.getPartById(partId);
    }

    @GetMapping
    public Page<PartResponse> getAllParts(
            Pageable pageable
    ) {
        return partService.getAllParts(pageable);
    }

    @DeleteMapping("/{partId}")
    public void deactivatePart(
            @PathVariable Long partId,
            @RequestParam Long version
    ) {
        partService.deactivatePart(
                partId,
                version
        );
    }
}