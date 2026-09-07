package com.example.backend.InventoryService.service;


import com.example.backend.InventoryService.dtos.part.CreatePartRequest;
import com.example.backend.InventoryService.dtos.part.PartResponse;
import com.example.backend.InventoryService.dtos.part.UpdatePartRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PartService {

    PartResponse createPart(
            CreatePartRequest request
    );

    PartResponse updatePart(
            Long partId,
            UpdatePartRequest request
    );

    PartResponse getPartById(
            Long partId
    );

    Page<PartResponse> getAllParts(
            Pageable pageable
    );

    void deactivatePart(
            Long partId,
            Long version
    );
}