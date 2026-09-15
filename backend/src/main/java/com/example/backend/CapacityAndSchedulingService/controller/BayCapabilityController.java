package com.example.backend.CapacityAndSchedulingService.controller;

import com.example.backend.CapacityAndSchedulingService.dto.bayCapability.BayCapabilityResponse;
import com.example.backend.CapacityAndSchedulingService.service.BayCapabilityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class BayCapabilityController {

    private final BayCapabilityService bayCapabilityService;

    public BayCapabilityController(
            BayCapabilityService bayCapabilityService
    ) {
        this.bayCapabilityService = bayCapabilityService;
    }

    @PostMapping(
            "/api/bays/{bayId}/capabilities/{capabilityCode}"
    )
    public ResponseEntity<Void> assignCapabilityToBay(
            @PathVariable Long bayId,
            @PathVariable String capabilityCode
    ) {

        bayCapabilityService.assignCapabilityToBay(
                bayId,
                capabilityCode
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .build();
    }

    @GetMapping(
            "/api/bays/{bayId}/capabilities"
    )
    public ResponseEntity<List<BayCapabilityResponse>>
    getCapabilitiesForBay(
            @PathVariable Long bayId
    ) {

        return ResponseEntity.ok(
                bayCapabilityService.getCapabilitiesForBay(
                        bayId
                )
        );
    }

    @DeleteMapping(
            "/api/bays/{bayId}/capabilities/{capabilityCode}"
    )
    public ResponseEntity<Void> removeCapabilityFromBay(
            @PathVariable Long bayId,
            @PathVariable String capabilityCode
    ) {

        bayCapabilityService.removeCapabilityFromBay(
                bayId,
                capabilityCode
        );

        return ResponseEntity
                .noContent()
                .build();
    }
}