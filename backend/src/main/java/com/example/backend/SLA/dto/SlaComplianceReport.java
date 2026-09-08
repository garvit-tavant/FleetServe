package com.example.backend.SLA.dto;

public class SlaComplianceReport {
    private com.example.backend.SLA.status.BreakdownPriority priority;
    private Long evaluatedCases;
    private Long compliantCases;
    private Double compliancePercent;

    public SlaComplianceReport(
            com.example.backend.SLA.status.BreakdownPriority priority,
            Long evaluatedCases,
            Long compliantCases,
            Double compliancePercent) {
        this.priority = priority;
        this.evaluatedCases = evaluatedCases;
        this.compliantCases = compliantCases;
        this.compliancePercent = compliancePercent;
    }

    // Getters and setters

    public com.example.backend.SLA.status.BreakdownPriority getPriority() {
        return priority;
    }

    public void setPriority(com.example.backend.SLA.status.BreakdownPriority priority) {
        this.priority = priority;
    }

    public Long getEvaluatedCases() {
        return evaluatedCases;
    }

    public void setEvaluatedCases(Long evaluatedCases) {
        this.evaluatedCases = evaluatedCases;
    }

    public Long getCompliantCases() {
        return compliantCases;
    }

    public void setCompliantCases(Long compliantCases) {
        this.compliantCases = compliantCases;
    }

    public Double getCompliancePercent() {
        return compliancePercent;
    }

    public void setCompliancePercent(Double compliancePercent) {
        this.compliancePercent = compliancePercent;
    }
}
