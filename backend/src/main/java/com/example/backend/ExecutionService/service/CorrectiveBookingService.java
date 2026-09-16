package com.example.backend.ExecutionService.service;

import com.example.backend.ExecutionService.dto.booking.CorrectiveBookingResponse;
import com.example.backend.ExecutionService.dto.booking.CreateCorrectiveBookingRequest;

public interface CorrectiveBookingService {

    CorrectiveBookingResponse createCorrectiveBooking(
            Long breakdownRequestId,
            CreateCorrectiveBookingRequest request
    );
}