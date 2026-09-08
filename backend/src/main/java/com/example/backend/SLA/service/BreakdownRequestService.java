package com.example.backend.SLA.service;

import com.example.backend.SLA.entity.BreakdownRequest;

public interface BreakdownRequestService {

    void createBreakdownRequest(BreakdownRequest request);
    BreakdownRequest getBreakdownRequest(Long id);
    void updateBreakdownRequestStatus(Long id, BreakdownRequest request);
      
}
