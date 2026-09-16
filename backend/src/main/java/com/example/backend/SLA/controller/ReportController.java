package com.example.backend.SLA.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

import com.example.backend.SLA.dto.MeanTimeToRepairReport;
import com.example.backend.SLA.dto.SlaComplianceReport;
import com.example.backend.SLA.service.impl.SlaService;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private SlaService slaService;

    public ReportController(SlaService slaService) {
        this.slaService = slaService;
    }

    @GetMapping("/mttr")
    public ResponseEntity<List<MeanTimeToRepairReport>> getMeanTimeToRepairReport() {
        List<MeanTimeToRepairReport> report = slaService.MeanTimeToRepair();
        return ResponseEntity.ok(report);
    }

    @GetMapping("/sla-compliance")
    public ResponseEntity<List<SlaComplianceReport>> getSlaComplianceReport() {
        List<SlaComplianceReport> report = slaService.SlaCompliance();
        return ResponseEntity.ok(report);
    }

}
