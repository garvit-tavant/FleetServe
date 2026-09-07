package com.example.backend.ExecutionService.service.impl;

import com.example.backend.ExecutionService.entity.BookingHistory;
import com.example.backend.ExecutionService.repository.BookingHistoryRepository;
import com.example.backend.ExecutionService.service.BookingHistoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class BookingHistoryServiceImpl implements BookingHistoryService {
    private final BookingHistoryRepository repository;
    public BookingHistoryServiceImpl(BookingHistoryRepository repository) {
        this.repository = repository;
    }
    @Override
    @Transactional
    public BookingHistory save(BookingHistory bookingHistory) {
        return repository.save(bookingHistory);
    }
    @Override
    public List<BookingHistory> findByBookingId(Long bookingId) {
        return repository.findAll(); // TODO: implement filter by bookingId
    }
}
