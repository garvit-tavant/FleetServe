package com.example.backend.CapacityAndSchedulingService.controller;

import com.example.backend.CapacityAndSchedulingService.dto.depot.CreateDepotRequest;
import com.example.backend.CapacityAndSchedulingService.dto.depot.DepotResponse;
import com.example.backend.CapacityAndSchedulingService.service.DepotService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/depots")
public class DepotController {

    private final DepotService depotService;

    public DepotController(
            DepotService depotService
    ) {
        this.depotService = depotService;
    }

    @PostMapping
    public ResponseEntity<DepotResponse> createDepot(
            @Valid
            @RequestBody
            CreateDepotRequest request
    ) {

        DepotResponse response =
                depotService.createDepot(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{depotId}")
    public ResponseEntity<DepotResponse> getDepot(
            @PathVariable Long depotId
    ) {

        DepotResponse response =
                depotService.getDepot(depotId);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<DepotResponse>> getAllDepots() {

        return ResponseEntity.ok(
                depotService.getAllDepots()
        );
    }

    @PostMapping("/{depotId}/activate")
    public ResponseEntity<Void> activateDepot(
            @PathVariable Long depotId
    ) {

        depotService.activateDepot(depotId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{depotId}/deactivate")
    public ResponseEntity<Void> deactivateDepot(
            @PathVariable Long depotId
    ) {

        depotService.deactivateDepot(depotId);

        return ResponseEntity.noContent().build();
    }
}
