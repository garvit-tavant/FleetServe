package com.example.backend.CapacityAndSchedulingService.controller;

import com.example.backend.CapacityAndSchedulingService.dto.bay.BayResponse;
import com.example.backend.CapacityAndSchedulingService.dto.bay.CreateBayRequest;
import com.example.backend.CapacityAndSchedulingService.service.BayService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class BayController {

    private final BayService bayService;

    public BayController(
            BayService bayService
    ) {
        this.bayService = bayService;
    }

    @PostMapping("/api/workshops/{workshopId}/bays")
    public ResponseEntity<BayResponse> createBay(
            @PathVariable Long workshopId,
            @Valid
            @RequestBody
            CreateBayRequest request
    ) {

        BayResponse response =
                bayService.createBay(
                        workshopId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/api/workshops/{workshopId}/bays")
    public ResponseEntity<List<BayResponse>>
    getBaysByWorkshop(
            @PathVariable Long workshopId
    ) {

        return ResponseEntity.ok(
                bayService.getBaysByWorkshop(
                        workshopId
                )
        );
    }

    @GetMapping("/api/bays/{bayId}")
    public ResponseEntity<BayResponse>
    getBay(
            @PathVariable Long bayId
    ) {

        BayResponse response =
                bayService.getBay(bayId);

        return ResponseEntity.ok(
                response
        );
    }

    @PostMapping("/api/bays/{bayId}/activate")
    public ResponseEntity<Void>
    activateBay(
            @PathVariable Long bayId
    ) {

        bayService.activateBay(bayId);

        return ResponseEntity
                .noContent()
                .build();
    }

    @PostMapping("/api/bays/{bayId}/deactivate")
    public ResponseEntity<Void>
    deactivateBay(
            @PathVariable Long bayId
    ) {

        bayService.deactivateBay(bayId);

        return ResponseEntity
                .noContent()
                .build();
    }
}