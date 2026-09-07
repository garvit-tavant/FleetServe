package com.example.backend.SLA.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.SLA.dto.SlaComplianceReport;
import com.example.backend.SLA.repository.BreakdownRequestRepository;
import com.example.backend.SLA.repository.SlaCheckpointRepository;

/**
 * SLA service facade providing high-level SLA queries for controllers and other services.
 *
 * Per project convention (Controller -> Service interface -> ServiceImpl), this should ideally
 * be split into an SlaService interface + SlaServiceImpl in a service/impl package,
 * matching AssetManagementService. Left as a single class for now since it's still minimal.
 *
 * @Service registers this as a Spring component; @Transactional(readOnly = true) applies
 * per project convention. Write methods should override with @Transactional.
 */
@Service
@Transactional(readOnly = true)
public class SlaService {
    private final SlaCalculator slaCalculator;
    private final BreakdownRequestRepository breakdownRequestRepository;
    private final SlaCheckpointRepository slaCheckpointRepository;

    public SlaService(SlaCalculator slaCalculator, BreakdownRequestRepository breakdownRequestRepository, SlaCheckpointRepository slaCheckpointRepository) {
        this.slaCalculator = slaCalculator;
        this.breakdownRequestRepository = breakdownRequestRepository;
        this.slaCheckpointRepository = slaCheckpointRepository;
    }

    /**
     * US-4.1 "soon to breach" view: priority plus both clocks' remaining time
     * and OK/AT_RISK/BREACHED status, in one call.
     *
     * it updates all the sla_checkpoints where breakdown_request has not been RESOLVED OR CANCELED
     */
    public void updateSlaStatus() {
        // id[0] => breakdownRequestId
        // id[1] => bookingId
        List<Long[]> IDs = breakdownRequestRepository.requestsnothandleded();
        for (Long[] id : IDs) {
           slaCalculator.isResponseBreach(id[0]);
           slaCalculator.isResolutionBreach(id[0], id[1]);    
        }

        
    }

    public List<SlaComplianceReport> SlaCompliance() {
        return slaCheckpointRepository.findSlaComplianceMetrics();
    }


}


