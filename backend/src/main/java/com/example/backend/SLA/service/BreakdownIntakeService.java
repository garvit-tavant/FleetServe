package com.example.backend.SLA.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.backend.SLA.dto.breakdown.BreakdownResponse;
import com.example.backend.SLA.dto.breakdown.RaiseBreakdownRequest;
import com.example.backend.SLA.status.BreakdownStatus;

/**
 * Breakdown intake (US-4.1): raising a breakdown and applying a service-level
 * target to it.
 */
public interface BreakdownIntakeService {

    /** Largest page the server will return, per the pagination rule. */
    int MAX_PAGE_SIZE = 100;

    /**
     * Raises a breakdown as REPORTED, pins the service-level policy in force for
     * its priority, and opens its service-level checkpoint.
     *
     * @param reportedById the authenticated user raising it
     */
    BreakdownResponse raiseBreakdown(RaiseBreakdownRequest request, Long reportedById);

    BreakdownResponse getBreakdown(long breakdownRequestId);

    /** Breakdowns in a given state, most recently reported first. */
    Page<BreakdownResponse> listBreakdowns(BreakdownStatus status, Pageable pageable);
}
