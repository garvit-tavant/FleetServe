package com.example.backend.CapacityAndSchedulingService.controller;

import com.example.backend.CapacityAndSchedulingService.dto.technician.TechnicianResponse;
import com.example.backend.CapacityAndSchedulingService.dto.workshop.CreateWorkshopRequest;
import com.example.backend.CapacityAndSchedulingService.dto.workshop.WorkshopResponse;
import com.example.backend.CapacityAndSchedulingService.service.TechnicianService;
import com.example.backend.CapacityAndSchedulingService.service.WorkshopService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workshops")
public class WorkshopController {

    private final WorkshopService workshopService;
    private final TechnicianService technicianService;

    public WorkshopController(
            WorkshopService workshopService,
            TechnicianService technicianService
    ) {
        this.workshopService = workshopService;
        this.technicianService = technicianService;
    }

    @GetMapping("/{workshopId}/technicians")
    public ResponseEntity<List<TechnicianResponse>>
    getTechniciansByWorkshop(
            @PathVariable Long workshopId
    ) {

        return ResponseEntity.ok(
                technicianService.getTechniciansByWorkshop(
                        workshopId
                ));
    }

    @PostMapping
    public ResponseEntity<WorkshopResponse> createWorkshop(
            @Valid
            @RequestBody
            CreateWorkshopRequest request
    ) {

        WorkshopResponse response =
                workshopService.createWorkshop(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<WorkshopResponse>>
    getAllWorkshops() {

        return ResponseEntity.ok(
                workshopService.getAllWorkshops()
        );
    }

    @GetMapping("/{workshopId}")
    public ResponseEntity<WorkshopResponse>
    getWorkshop(
            @PathVariable Long workshopId
    ) {

        WorkshopResponse response =
                workshopService.getWorkshop(workshopId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{workshopId}/activate")
    public ResponseEntity<Void>
    activateWorkshop(
            @PathVariable Long workshopId
    ) {

        workshopService.activateWorkshop(workshopId);

        return ResponseEntity.noContent()
                .build();
    }

    @PostMapping("/{workshopId}/deactivate")
    public ResponseEntity<Void>
    deactivateWorkshop(
            @PathVariable Long workshopId
    ) {

        workshopService.deactivateWorkshop(workshopId);

        return ResponseEntity.noContent()
                .build();
    }
}