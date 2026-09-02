package com.example.backend.CapacityAndSchedulingService.controller;

import com.example.backend.CapacityAndSchedulingService.dto.skill.CreateSkillRequest;
import com.example.backend.CapacityAndSchedulingService.dto.skill.SkillResponse;
import com.example.backend.CapacityAndSchedulingService.service.SkillService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
public class SkillController {

    private final SkillService skillService;

    public SkillController(
            SkillService skillService
    ) {
        this.skillService = skillService;
    }

    @PostMapping
    public ResponseEntity<SkillResponse> createSkill(
            @Valid
            @RequestBody
            CreateSkillRequest request
    ) {

        SkillResponse response =
                skillService.createSkill(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{skillCode}")
    public ResponseEntity<SkillResponse> getSkill(
            @PathVariable String skillCode
    ) {

        SkillResponse response =
                skillService.getSkill(skillCode);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<SkillResponse>>
    getAllSkills() {

        return ResponseEntity.ok(
                skillService.getAllSkills()
        );
    }
}