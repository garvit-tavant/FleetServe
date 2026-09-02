package com.example.backend.CapacityAndSchedulingService.controller;

import com.example.backend.CapacityAndSchedulingService.dto.technician.CreateTechnicianRequest;
import com.example.backend.CapacityAndSchedulingService.dto.technician.TechnicianResponse;
import com.example.backend.CapacityAndSchedulingService.service.TechnicianService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/technicians")
public class TechnicianController {

    private final TechnicianService technicianService;

    public TechnicianController(
            TechnicianService technicianService
    ) {
        this.technicianService = technicianService;
    }

    @PostMapping
    public ResponseEntity<TechnicianResponse>
    createTechnician(
            @Valid
            @RequestBody
            CreateTechnicianRequest request
    ) {

        TechnicianResponse response =
                technicianService.createTechnician(
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{technicianId}")
    public ResponseEntity<TechnicianResponse>
    getTechnician(
            @PathVariable Long technicianId
    ) {

        TechnicianResponse response =
                technicianService.getTechnician(
                        technicianId
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<TechnicianResponse>>
    getAllTechnicians() {

        return ResponseEntity.ok(
                technicianService.getAllTechnicians()
        );
    }

    @PostMapping("/{technicianId}/activate")
    public ResponseEntity<Void>
    activateTechnician(
            @PathVariable Long technicianId
    ) {

        technicianService.activateTechnician(
                technicianId
        );

        return ResponseEntity
                .noContent()
                .build();
    }

    @PostMapping("/{technicianId}/deactivate")
    public ResponseEntity<Void>
    deactivateTechnician(
            @PathVariable Long technicianId
    ) {

        technicianService.deactivateTechnician(
                technicianId
        );

        return ResponseEntity
                .noContent()
                .build();
    }

    @GetMapping("/api/workshops/{workshopId}/technicians")
    public ResponseEntity<List<TechnicianResponse>>
    getTechniciansByWorkshop(
            @PathVariable Long workshopId
    ) {

        return ResponseEntity.ok(
                technicianService.getTechniciansByWorkshop(
                        workshopId
                ));
    }
}