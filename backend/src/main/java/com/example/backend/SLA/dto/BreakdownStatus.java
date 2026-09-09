package com.example.backend.SLA.dto;

/**
 * Mirrors the ck_breakdown_status CHECK constraint in
 * V5__sla_and_breakdown.sql.
 */
public enum BreakdownStatus {
    REPORTED,
    TRIAGED,
    BOOKED,
    IN_PROGRESS,
    RESOLVED,
    CANCELLED
}
