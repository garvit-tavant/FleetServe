package com.example.backend.SLA.status;

/**
 * Breakdown urgency, as required by US-4.1.
 *
 * <p>The response and resolution targets for each priority are configurable
 * data held in {@code sla_policy}, not constants here. These names are the
 * values allowed by {@code ck_breakdown_priority}.
 */
public enum BreakdownPriority {

    /** Most urgent. */
    P1,

    P2,

    /** Least urgent. */
    P3
}
