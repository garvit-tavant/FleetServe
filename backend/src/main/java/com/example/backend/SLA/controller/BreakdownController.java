package com.example.backend.SLA.controller;

import java.net.URI;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.AssetManagamentService.port.CurrentUserProvider;
import com.example.backend.SLA.dto.breakdown.BreakdownResponse;
import com.example.backend.SLA.dto.breakdown.RaiseBreakdownRequest;
import com.example.backend.SLA.service.BreakdownIntakeService;
import com.example.backend.SLA.status.BreakdownStatus;

/**
 * Breakdown intake (US-4.1).
 *
 * <p>Shares the {@code /api/breakdowns} base path with
 * {@code BreakdownBookingController}: raising and listing a breakdown is
 * service-level work, while booking one into a slot is execution work, so the
 * two live in their own modules.
 */
@Validated
@RestController
@RequestMapping("/api/breakdowns")
public class BreakdownController {

    private final BreakdownIntakeService breakdownIntakeService;
    private final CurrentUserProvider currentUserProvider;

    public BreakdownController(
            BreakdownIntakeService breakdownIntakeService,
            CurrentUserProvider currentUserProvider) {
        this.breakdownIntakeService = breakdownIntakeService;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * POST /api/breakdowns
     *
     * <p>Raises a breakdown as REPORTED, pins the service-level policy in force
     * for its priority, and opens its service-level checkpoint. The reporter is
     * the authenticated user; the depot is taken from the asset.
     *
     * <p>201 with the breakdown. 422 when the asset is retired or no policy is in
     * force for the priority. 404 when the asset, skill or capability is unknown.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('DEPOT_SUPERVISOR','SERVICE_COORDINATOR',"
            + "'WORKSHOP_MANAGER','FLEET_ADMINISTRATOR')")
    public ResponseEntity<BreakdownResponse> raiseBreakdown(
            @Valid @RequestBody RaiseBreakdownRequest request) {

        BreakdownResponse response = breakdownIntakeService.raiseBreakdown(
                request, currentUserProvider.getCurrentUserId());

        return ResponseEntity
                .created(URI.create("/api/breakdowns/" + response.id()))
                .body(response);
    }

    /**
     * GET /api/breakdowns
     *
     * <p>Breakdowns awaiting attention, most recently reported first. Defaults to
     * REPORTED, which is the triage queue. Pass {@code status=} with no value to
     * see every state.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('DEPOT_SUPERVISOR','SERVICE_COORDINATOR',"
            + "'WORKSHOP_MANAGER','FLEET_ADMINISTRATOR','OPERATIONS_MANAGER')")
    public ResponseEntity<Page<BreakdownResponse>> listBreakdowns(
            @RequestParam(required = false, defaultValue = "REPORTED") BreakdownStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<BreakdownResponse> result = breakdownIntakeService.listBreakdowns(
                status, PageRequest.of(page, size));

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/breakdowns/{breakdownRequestId}
     */
    @GetMapping("/{breakdownRequestId}")
    @PreAuthorize("hasAnyRole('DEPOT_SUPERVISOR','SERVICE_COORDINATOR',"
            + "'WORKSHOP_MANAGER','FLEET_ADMINISTRATOR','OPERATIONS_MANAGER')")
    public ResponseEntity<BreakdownResponse> getBreakdown(
            @Positive(message = "breakdownRequestId must be positive")
            @PathVariable Long breakdownRequestId) {

        return ResponseEntity.ok(breakdownIntakeService.getBreakdown(breakdownRequestId));
    }
}
