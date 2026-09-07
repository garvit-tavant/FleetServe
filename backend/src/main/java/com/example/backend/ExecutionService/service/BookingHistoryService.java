package com.example.backend.ExecutionService.service;

import com.example.backend.ExecutionService.entity.BookingHistory;
import java.util.List;

public interface BookingHistoryService {
    BookingHistory save(BookingHistory bookingHistory);
    List<BookingHistory> findByBookingId(Long bookingId);
}
