package com.example.backend.ExecutionService.mapper;

import org.springframework.stereotype.Component;

import com.example.backend.ExecutionService.dto.booking.BookingResponse;
import com.example.backend.ExecutionService.entity.Booking;
import com.example.backend.ExecutionService.entity.WorkOrder;

@Component
public class BookingMapper {

    public BookingResponse toResponse(Booking booking) {

        BookingResponse response = new BookingResponse();

        response.setId(booking.getId());
        response.setAssetId(
                booking.getAsset() != null
                        ? booking.getAsset().getId()
                        : null);
        response.setWorkshopId(
                booking.getWorkshop() != null
                        ? booking.getWorkshop().getId()
                        : null);
        response.setBayId(
                booking.getBay() != null
                        ? booking.getBay().getId()
                        : null);
        response.setTechnicianId(
                booking.getTechnician() != null
                        ? booking.getTechnician().getId()
                        : null);
        response.setStartAt(booking.getStartAt());
        response.setEndAt(booking.getEndAt());
        response.setKind(
                booking.getKind() != null
                        ? booking.getKind().name()
                        : null);
        response.setMaintenancePlanId(
                booking.getMaintenancePlan() != null
                        ? booking.getMaintenancePlan().getId()
                        : null);
        response.setBreakdownRequestId(
                booking.getBreakdownRequest() != null
                        ? booking.getBreakdownRequest().getId()
                        : null);
        response.setStatus(
                booking.getStatus() != null
                        ? booking.getStatus().name()
                        : null);

        WorkOrder workOrder = booking.getWorkOrder();

        if (workOrder != null) {
            response.setWorkOrderId(workOrder.getId());
            response.setWorkOrderNumber(workOrder.getWorkOrderNumber());
            response.setWorkOrderStatus(
                    workOrder.getStatus() != null
                            ? workOrder.getStatus().name()
                            : null);
        }

        return response;
    }
}
