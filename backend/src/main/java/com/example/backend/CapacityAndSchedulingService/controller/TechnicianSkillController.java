package com.example.backend.CapacityAndSchedulingService.controller;

import com.example.backend.CapacityAndSchedulingService.dto.technicianskill.AssignSkillRequest;
import com.example.backend.CapacityAndSchedulingService.dto.technicianskill.TechnicianSkillResponse;
import com.example.backend.CapacityAndSchedulingService.service.TechnicianSkillService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/technicians")
public class TechnicianSkillController {

    private final TechnicianSkillService technicianSkillService;

    public TechnicianSkillController(
            TechnicianSkillService technicianSkillService
    ) {
        this.technicianSkillService =
                technicianSkillService;
    }

    @PostMapping("/{technicianId}/skills")
    public ResponseEntity<Void>
    assignSkillToTechnician(
            @PathVariable Long technicianId,

            @Valid
            @RequestBody
            AssignSkillRequest request
    ) {

        technicianSkillService
                .assignSkillToTechnician(
                        technicianId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .build();
    }

    @GetMapping("/{technicianId}/skills")
    public ResponseEntity<
            List<TechnicianSkillResponse>>
    getSkillsForTechnician(
            @PathVariable Long technicianId
    ) {

        return ResponseEntity.ok(
                technicianSkillService
                        .getSkillsForTechnician(
                                technicianId
                        )
        );
    }

    @DeleteMapping(
            "/{technicianId}/skills/{skillCode}"
    )
    public ResponseEntity<Void>
    removeSkillFromTechnician(
            @PathVariable Long technicianId,

            @PathVariable String skillCode
    ) {

        technicianSkillService
                .removeSkillFromTechnician(
                        technicianId,
                        skillCode
                );

        return ResponseEntity
                .noContent()
                .build();
    }
}