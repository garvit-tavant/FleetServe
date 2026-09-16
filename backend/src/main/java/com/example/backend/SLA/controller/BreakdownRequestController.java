package com.example.backend.SLA.controller;

import com.example.backend.ExecutionService.dto.booking.CorrectiveBookingResponse;
import com.example.backend.ExecutionService.dto.booking.CreateCorrectiveBookingRequest;
import com.example.backend.ExecutionService.service.CorrectiveBookingService;
import com.example.backend.SLA.dto.BreakdownRequestResponse;
import com.example.backend.SLA.dto.BreakdownStatus;
import com.example.backend.SLA.dto.CreateBreakdownRequestRequest;
import com.example.backend.SLA.service.BreakdownRequestService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/breakdown-requests")
public class BreakdownRequestController {

    private final BreakdownRequestService breakdownRequestService;
    private final CorrectiveBookingService correctiveBookingService;

    public BreakdownRequestController(
            BreakdownRequestService breakdownRequestService, CorrectiveBookingService correctiveBookingService) {
        this.breakdownRequestService = breakdownRequestService;
        this.correctiveBookingService = correctiveBookingService;
    }

    /**
     * Raise a new breakdown request.
     */
    @PostMapping
    public ResponseEntity<BreakdownRequestResponse>
    createBreakdownRequest(
            @Valid
            @RequestBody
            CreateBreakdownRequestRequest request) {

        BreakdownRequestResponse response =
                breakdownRequestService
                        .createBreakdownRequest(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Get breakdown by id.
     */
    @GetMapping("/{id}")
    public ResponseEntity<BreakdownRequestResponse>
    getBreakdownRequest(
            @PathVariable Long id) {

        BreakdownRequestResponse response =
                breakdownRequestService
                        .getBreakdownRequest(id);

        return ResponseEntity.ok(response);
    }

    /**
     * Update breakdown status.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<BreakdownRequestResponse>
    updateBreakdownStatus(
            @PathVariable Long id,

            @RequestParam BreakdownStatus status) {

        BreakdownRequestResponse response =
                breakdownRequestService
                        .updateBreakdownRequestStatus(
                                id,
                                status);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{breakdownId}/booking")
    public CorrectiveBookingResponse createCorrectiveBooking(
            @PathVariable Long breakdownId,
            @Valid @RequestBody
            CreateCorrectiveBookingRequest request
    ) {
        return correctiveBookingService
                .createCorrectiveBooking(
                        breakdownId,
                        request
                );
    }
}