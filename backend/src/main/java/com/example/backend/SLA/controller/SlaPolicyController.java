package com.example.backend.SLA.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.backend.SLA.dto.CreateSlaPolicyRequest;
import com.example.backend.SLA.dto.SlaPolicyResponse;
import com.example.backend.SLA.service.SlaPolicyService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/sla-policies")
public class SlaPolicyController {

    private final SlaPolicyService slaPolicyService;

    public SlaPolicyController(
            SlaPolicyService slaPolicyService) {

        this.slaPolicyService = slaPolicyService;
    }

    @PostMapping
    public ResponseEntity<SlaPolicyResponse> createPolicy(
            @Valid @RequestBody CreateSlaPolicyRequest request) {

        SlaPolicyResponse response =
                slaPolicyService.createSlaPolicy(request);

        return ResponseEntity
                .created(
                        URI.create(
                                "/api/sla-policies/" + response.getId()))
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SlaPolicyResponse> getPolicy(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                slaPolicyService.getSlaPolicy(id));
    }

    @GetMapping
    public ResponseEntity<List<SlaPolicyResponse>> getAllPolicies() {

        return ResponseEntity.ok(
                slaPolicyService.getAllPolicies());
    }
}