package com.example.backend.SLA.dto;

public class SlaComplianceReport {
    private String priority;
    private Long evaluatedCases;
    private Long compliantCases;
    private Double compliancePercent;

    public SlaComplianceReport(String priority, Long evaluatedCases, Long compliantCases, Double compliancePercent) {
        this.priority = priority;
        this.evaluatedCases = evaluatedCases;
        this.compliantCases = compliantCases;
        this.compliancePercent = compliancePercent;
    }

    // Getters and setters

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
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
