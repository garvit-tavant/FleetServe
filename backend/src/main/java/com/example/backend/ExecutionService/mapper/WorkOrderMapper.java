package com.example.backend.ExecutionService.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.example.backend.ExecutionService.dto.workorder.WorkOrderDetailsResponse;
import com.example.backend.ExecutionService.dto.workorder.WorkOrderResponse;
import com.example.backend.ExecutionService.entity.WorkOrder;

@Component
public class WorkOrderMapper {

    private final WorkOrderLabourMapper labourMapper;
    private final WorkOrderPartMapper partMapper;

    public WorkOrderMapper(
            WorkOrderLabourMapper labourMapper,
            WorkOrderPartMapper partMapper) {

        this.labourMapper = labourMapper;
        this.partMapper = partMapper;
    }

    public WorkOrderResponse toResponse(
            WorkOrder workOrder) {

        WorkOrderResponse response =
                new WorkOrderResponse();

        response.setId(
                workOrder.getId());

        response.setWorkOrderNumber(
                workOrder.getWorkOrderNumber());

        response.setBookingId(
                workOrder.getBooking().getId());

        response.setStatus(
                workOrder.getStatus().name());

        response.setStartedAt(
                workOrder.getStartedAt());

        response.setCompletedAt(
                workOrder.getCompletedAt());

        response.setOdometerAtService(
                workOrder.getOdometerAtService());

        response.setTotalCost(
                workOrder.getTotalCost());

        return response;
    }

    public WorkOrderDetailsResponse toDetailsResponse(
            WorkOrder workOrder) {

        WorkOrderDetailsResponse response =
                new WorkOrderDetailsResponse();

        response.setId(
                workOrder.getId());

        response.setWorkOrderNumber(
                workOrder.getWorkOrderNumber());

        response.setBookingId(
                workOrder.getBooking().getId());

        response.setStatus(
                workOrder.getStatus().name());

        response.setStartedAt(
                workOrder.getStartedAt());

        response.setCompletedAt(
                workOrder.getCompletedAt());

        response.setOdometerAtService(
                workOrder.getOdometerAtService());

        response.setTotalCost(
                workOrder.getTotalCost());

        response.setLabourEntries(
                workOrder.getLabourEntries()
                        .stream()
                        .map(labourMapper::toResponse)
                        .toList());

        response.setPartsUsed(
                workOrder.getPartEntries()
                        .stream()
                        .map(partMapper::toResponse)
                        .toList());

        return response;
    }
}