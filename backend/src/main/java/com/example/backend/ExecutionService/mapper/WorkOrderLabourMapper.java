package com.example.backend.ExecutionService.mapper;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.example.backend.ExecutionService.dto.workorderlabour.WorkOrderLabourResponse;
import com.example.backend.ExecutionService.entity.WorkOrderLabour;

@Component
public class WorkOrderLabourMapper {

    public WorkOrderLabourResponse toResponse(
            WorkOrderLabour labour) {

        WorkOrderLabourResponse response =
                new WorkOrderLabourResponse();

        response.setId(
                labour.getId());

        response.setTechnicianId(
                labour.getTechnician().getId());

        response.setTechnicianName(
                labour.getTechnician().getAppUser().getUsername());

        response.setHours(
                labour.getHours());

        response.setRateApplied(
                labour.getRateApplied());

        response.setLabourCost(
                labour.getHours()
                        .multiply(
                                labour.getRateApplied()));

        return response;
    }
}

