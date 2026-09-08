package com.example.backend.ExecutionService.controller;

import java.net.URI;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.ExecutionService.dto.booking.BookBreakdownSlotRequest;
import com.example.backend.ExecutionService.dto.booking.BookingResponse;
import com.example.backend.ExecutionService.service.SlotBookingService;

/**
 * Books corrective work for a breakdown raised by a depot supervisor.
 */
@Validated
@RestController
@RequestMapping("/api/breakdowns")
public class BreakdownBookingController {

    private final SlotBookingService slotBookingService;

    public BreakdownBookingController(SlotBookingService slotBookingService) {
        this.slotBookingService = slotBookingService;
    }

    /**
     * POST /api/breakdowns/{breakdownRequestId}/booking
     *
     * <p>Holds the earliest feasible slot for a breakdown and moves it to
     * BOOKED. The required skill, required capability and estimated duration
     * come from the breakdown record, and the workshop must belong to the depot
     * that reported it.
     *
     * <p>201 with the booking when a slot is held. 422 when no feasible slot
     * exists in the next {@value SlotBookingService#SEARCH_HORIZON_DAYS} days,
     * when the breakdown is past triage, or when the workshop serves a different
     * depot. 409 if it is already booked, or if the bay or technician was taken
     * concurrently.
     */
    @PostMapping("/{breakdownRequestId}/booking")
    @PreAuthorize("hasAnyRole('DEPOT_SUPERVISOR','SERVICE_COORDINATOR',"
            + "'WORKSHOP_MANAGER','FLEET_ADMINISTRATOR')")
    public ResponseEntity<BookingResponse> bookBreakdownSlot(
            @Positive(message = "breakdownRequestId must be positive")
            @PathVariable Long breakdownRequestId,
            @Valid @RequestBody BookBreakdownSlotRequest request) {

        BookingResponse response =
                slotBookingService.bookBreakdownSlot(breakdownRequestId, request);

        return ResponseEntity
                .created(URI.create("/api/bookings/" + response.bookingId()))
                .body(response);
    }
}
