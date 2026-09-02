package com.example.backend.CapacityAndSchedulingService.controller;

import com.example.backend.CapacityAndSchedulingService.dto.capability.CapabilityResponse;
import com.example.backend.CapacityAndSchedulingService.dto.capability.CreateCapabilityRequest;
import com.example.backend.CapacityAndSchedulingService.service.CapabilityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/capabilities")
public class CapabilityController {

    private final CapabilityService capabilityService;

    public CapabilityController(
            CapabilityService capabilityService
    ) {
        this.capabilityService = capabilityService;
    }

    @PostMapping
    public ResponseEntity<CapabilityResponse>
    createCapability(
            @Valid
            @RequestBody
            CreateCapabilityRequest request
    ) {

        CapabilityResponse response =
                capabilityService.createCapability(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<CapabilityResponse>>
    getAllCapabilities() {

        return ResponseEntity.ok(
                capabilityService.getAllCapabilities()
        );
    }

    @GetMapping("/{capabilityCode}")
    public ResponseEntity<CapabilityResponse>
    getCapability(
            @PathVariable
            String capabilityCode
    ) {

        CapabilityResponse response =
                capabilityService.getCapability(
                        capabilityCode
                );

        return ResponseEntity.ok(response);
    }
}