package com.example.backend.ExecutionService.repository;

import com.example.backend.ExecutionService.entity.BookingHistory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingHistoryRepository extends JpaRepository<BookingHistory, Long> {

}
