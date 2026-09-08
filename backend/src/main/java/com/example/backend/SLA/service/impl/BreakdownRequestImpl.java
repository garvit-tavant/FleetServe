package com.example.backend.SLA.service.impl;

import com.example.backend.SLA.entity.BreakdownRequest;
import com.example.backend.SLA.repository.BreakdownRequestRepository;
import com.example.backend.SLA.service.BreakdownRequestService;

public class BreakdownRequestImpl implements BreakdownRequestService {

    private final BreakdownRequestRepository breakdownRequestRepository;

    public BreakdownRequestImpl(BreakdownRequestRepository breakdownRequestRepository) {
        this.breakdownRequestRepository = breakdownRequestRepository;
    }

    @Override
    public void createBreakdownRequest(BreakdownRequest request) {
        breakdownRequestRepository.save(request);
        System.out.println("Breakdown request created: " + request);
    }

    @Override
    public BreakdownRequest getBreakdownRequest(Long id) {
        return breakdownRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Breakdown request not found"));
    }

    @Override
    public void updateBreakdownRequestStatus(Long id, BreakdownRequest request) {
        BreakdownRequest existingRequest = breakdownRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Breakdown request not found"));
        existingRequest.setStatus(request.getStatus());
        breakdownRequestRepository.save(existingRequest);
        System.out.println("Breakdown request with ID " + id + " updated to: " + existingRequest);
    }

}
