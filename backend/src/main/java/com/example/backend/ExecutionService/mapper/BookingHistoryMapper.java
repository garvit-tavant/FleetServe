package com.example.backend.ExecutionService.mapper;

import org.springframework.stereotype.Component;

import com.example.backend.ExecutionService.dto.bookinghistory.BookingHistoryResponse;
import com.example.backend.ExecutionService.entity.BookingHistory;

@Component
public class BookingHistoryMapper {

    public BookingHistoryResponse toResponse(
            BookingHistory history) {

        BookingHistoryResponse response =
                new BookingHistoryResponse();

        response.setId(
                history.getId());

        response.setBookingId(
                history.getBooking().getId());

        response.setAction(
                history.getAction());

        response.setActorId(
                history.getActor().getId());

        response.setReason(
                history.getReason());

        response.setOccurredAt(
                history.getOccurredAt());

        return response;
    }
}