package com.example.backend.ExecutionService.service;

import com.example.backend.ExecutionService.dto.booking.BookingCancellationResponse;
import com.example.backend.ExecutionService.dto.booking.CancelBookingRequest;

public interface CancelBookingService {

    BookingCancellationResponse cancelBooking(
            Long bookingId,
            CancelBookingRequest request);
}