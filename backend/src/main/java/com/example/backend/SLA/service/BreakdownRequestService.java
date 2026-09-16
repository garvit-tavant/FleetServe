package com.example.backend.SLA.service;

import com.example.backend.SLA.dto.BreakdownRequestResponse;
import com.example.backend.SLA.dto.BreakdownStatus;
import com.example.backend.SLA.dto.CreateBreakdownRequestRequest;

public interface BreakdownRequestService {

    BreakdownRequestResponse createBreakdownRequest(
            CreateBreakdownRequestRequest request);

    BreakdownRequestResponse getBreakdownRequest(
            Long id);

    BreakdownRequestResponse updateBreakdownRequestStatus(
            Long id,
            BreakdownStatus targetStatus);
}
