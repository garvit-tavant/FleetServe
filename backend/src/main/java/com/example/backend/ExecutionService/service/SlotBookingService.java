package com.example.backend.ExecutionService.service;

import com.example.backend.ExecutionService.dto.booking.BookBreakdownSlotRequest;
import com.example.backend.ExecutionService.dto.booking.BookPreventiveSlotRequest;
import com.example.backend.ExecutionService.dto.booking.BookingResponse;

public interface SlotBookingService {

    /**
     * How far ahead the engine looks. If nothing is feasible inside this window
     * the caller is told the slot was not booked, rather than the search running
     * on indefinitely.
     */
    int SEARCH_HORIZON_DAYS = 14;

    /** Holds the earliest feasible slot for a maintenance plan due on an asset. */
    BookingResponse bookPreventiveSlot(BookPreventiveSlotRequest request);

    /** Holds the earliest feasible slot for a reported breakdown. */
    BookingResponse bookBreakdownSlot(long breakdownRequestId, BookBreakdownSlotRequest request);
}
