package com.example.backend.ExecutionService.mapper;

import com.example.backend.ExecutionService.dto.booking.CorrectiveBookingResponse;
import com.example.backend.ExecutionService.entity.Booking;
import com.example.backend.ExecutionService.entity.WorkOrder;
import org.springframework.data.domain.Range;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class CorrectiveBookingMapper {

    public CorrectiveBookingResponse toResponse(
            Booking booking,
            WorkOrder workOrder
    ) {

        CorrectiveBookingResponse response =
                new CorrectiveBookingResponse();

        response.setBookingId(
                booking.getId());

        response.setWorkOrderId(
                workOrder.getId());

        response.setWorkOrderNumber(
                workOrder.getWorkOrderNumber());

        response.setAssetId(
                booking.getAsset().getId());

        response.setWorkshopId(
                booking.getWorkshop().getId());

        response.setBayId(
                booking.getBay().getId());

        response.setTechnicianId(
                booking.getTechnician().getId());

        response.setBreakdownRequestId(
                booking.getBreakdownRequest().getId());

        OffsetDateTime start = booking.getStartAt();
        OffsetDateTime end = booking.getEndAt();
        response.setStart(
                start);

        response.setEnd(
                end);

        response.setBookingKind(
                booking.getKind().name());

        response.setBookingStatus(
                booking.getStatus().name());

        response.setWorkOrderStatus(
                workOrder.getStatus().name());

        return response;
    }
}
