package com.example.backend.ExecutionService.controller;

import com.example.backend.ExecutionService.dto.booking.BookingCancellationResponse;
import com.example.backend.ExecutionService.dto.booking.CancelBookingRequest;
import com.example.backend.ExecutionService.entity.Booking;
import com.example.backend.ExecutionService.repository.BookingRepository;
import com.example.backend.ExecutionService.service.CancelBookingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final CancelBookingService cancelBookingService;
    private final BookingRepository bookingRepo;

    public BookingController(
            CancelBookingService cancelBookingService,
            BookingRepository bookingRepo
    ) {
        this.cancelBookingService =
                cancelBookingService;
        this.bookingRepo = bookingRepo;
    }

    @GetMapping
    public List<Booking> getBooking(){
        System.out.println("maa chudgayi");
        return bookingRepo.findAll();
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