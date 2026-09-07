package com.example.backend.SLA.dto;

/**
 * Where a single SLA clock (response or resolution) currently stands.
 *
 * OK        - plenty of working time left before the target.
 * AT_RISK   - inside the warning window (see SlaCalculator#AT_RISK_THRESHOLD_MINUTES)
 *             but not yet over target. This is what US-4.1's "soon to breach" view needs.
 * BREACHED  - target already exceeded.
 */
public enum SlaClockStatus {
    OK,
    AT_RISK,
    BREACHED
}
