package com.example.backend.ExecutionService.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend.ExecutionService.entity.BookingHistory;

@Repository
public interface BookingHistoryRepository
        extends JpaRepository<BookingHistory, Long> {

    List<BookingHistory> findByBooking_IdOrderByOccurredAtDesc(
            Long bookingId
    );
}