package com.example.backend.ExecutionService.controller;

import com.example.backend.AssetManagamentService.exception.ResourceNotFoundException;
import com.example.backend.ExecutionService.dto.booking.BookingCancellationResponse;
import com.example.backend.ExecutionService.dto.booking.BookingResponse;
import com.example.backend.ExecutionService.dto.booking.CancelBookingRequest;
import com.example.backend.ExecutionService.entity.Booking;
import com.example.backend.ExecutionService.mapper.BookingMapper;
import com.example.backend.ExecutionService.repository.BookingRepository;
import com.example.backend.ExecutionService.service.CancelBookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final CancelBookingService cancelBookingService;
    private final BookingRepository bookingRepo;
    private final BookingMapper bookingMapper;

    public BookingController(
            CancelBookingService cancelBookingService,
            BookingRepository bookingRepo,
            BookingMapper bookingMapper
    ) {
        this.cancelBookingService =
                cancelBookingService;
        this.bookingRepo = bookingRepo;
        this.bookingMapper = bookingMapper;
    }

    @GetMapping
    public ResponseEntity<List<BookingResponse>> getAllBookings() {

        List<BookingResponse> response =
                bookingRepo.findAll()
                        .stream()
                        .map(bookingMapper::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingResponse> getBooking(
            @PathVariable Long bookingId
    ) {

        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Booking not found with id: " + bookingId));

        return ResponseEntity.ok(
                bookingMapper.toResponse(booking));
    }

    @PostMapping("/{bookingId}/cancel")
    public BookingCancellationResponse cancelBooking(
            @PathVariable Long bookingId,
            @RequestBody CancelBookingRequest request
    ) {
        return cancelBookingService.cancelBooking(
                bookingId,
                request
        );
    }
}