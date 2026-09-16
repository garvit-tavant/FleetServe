package com.example.backend.SLA.mapper;

import org.springframework.stereotype.Component;

import com.example.backend.SLA.dto.BreakdownRequestResponse;
import com.example.backend.SLA.entity.BreakdownRequest;

@Component
public class BreakdownRequestMapper {

    public BreakdownRequestResponse toResponse(
            BreakdownRequest breakdownRequest) {

        if (breakdownRequest == null) {
            return null;
        }

        BreakdownRequestResponse response =
                new BreakdownRequestResponse();

        response.setId(breakdownRequest.getId());

        if (breakdownRequest.getAsset() != null) {
            response.setAssetId(
                    breakdownRequest.getAsset().getId());

            response.setAssetCode(
                    breakdownRequest.getAsset().getVin());
        }

        if (breakdownRequest.getDepot() != null) {
            response.setDepotId(
                    breakdownRequest.getDepot().getId());
        }

        response.setPriority(
                breakdownRequest.getPriority());

        response.setDescription(
                breakdownRequest.getDescription());

        response.setStatus(
                breakdownRequest.getStatus());

        if (breakdownRequest.getReportedBy() != null) {
            response.setReportedById(
                    breakdownRequest.getReportedBy().getId());
        }

        response.setReportedAt(
                breakdownRequest.getReportedAt());

        if (breakdownRequest.getSlaPolicy() != null) {
            response.setSlaPolicyId(
                    breakdownRequest.getSlaPolicy().getId());
        }

        response.setBookingId(
                breakdownRequest.getBooking() != null
                        ? breakdownRequest.getBooking().getId()
                        : null);

        return response;
    }
}
