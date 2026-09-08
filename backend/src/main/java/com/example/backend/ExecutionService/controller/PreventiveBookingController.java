package com.example.backend.ExecutionService.controller;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.ExecutionService.dto.booking.BookPreventiveSlotRequest;
import com.example.backend.ExecutionService.dto.booking.BookingResponse;
import com.example.backend.ExecutionService.service.SlotBookingService;

/**
 * Books preventive maintenance that the due list has flagged.
 */
@RestController
@RequestMapping("/api/bookings/preventive")
public class PreventiveBookingController {

    private final SlotBookingService slotBookingService;

    public PreventiveBookingController(SlotBookingService slotBookingService) {
        this.slotBookingService = slotBookingService;
    }

    /**
     * POST /api/bookings/preventive
     *
     * <p>Holds the earliest feasible slot for a maintenance plan that is due on
     * an asset. Duration, required skill and required capability come from the
     * plan, so scheduling follows policy rather than the caller.
     *
     * <p>201 with the booking when a slot is held. 422 when no feasible slot
     * exists in the next {@value SlotBookingService#SEARCH_HORIZON_DAYS} days.
     * 409 if the bay or technician was taken concurrently, which the database
     * exclusion constraints decide.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('MAINTENANCE_PLANNER','SERVICE_COORDINATOR',"
            + "'WORKSHOP_MANAGER','FLEET_ADMINISTRATOR')")
    public ResponseEntity<BookingResponse> bookPreventiveSlot(
            @Valid @RequestBody BookPreventiveSlotRequest request) {

        BookingResponse response = slotBookingService.bookPreventiveSlot(request);

        return ResponseEntity
                .created(URI.create("/api/bookings/" + response.bookingId()))
                .body(response);
    }
}
