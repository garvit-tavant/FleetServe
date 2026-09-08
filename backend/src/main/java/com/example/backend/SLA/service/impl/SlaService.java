package com.example.backend.SLA.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.SLA.dto.SlaComplianceReport;
import com.example.backend.ExecutionService.repository.WorkOrderRepository;
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
    private final WorkOrderRepository workOrderRepository;
    private final SlaCheckpointRepository slaCheckpointRepository;

    public SlaService(SlaCalculator slaCalculator, WorkOrderRepository workOrderRepository, SlaCheckpointRepository slaCheckpointRepository) {
        this.slaCalculator = slaCalculator;
        this.workOrderRepository = workOrderRepository;
        this.slaCheckpointRepository = slaCheckpointRepository;
    }

    /**
     * US-4.1 "soon to breach" view: priority plus both clocks' remaining time
     * and OK/AT_RISK/BREACHED status, in one call.
     *
     * it updates all the sla_checkpoints where breakdown_request has not been RESOLVED OR CANCELED
     */
    public void updateSlaStatus() {
        // Only evaluate SLA clocks for bookings that currently have an
        // active WorkOrder (SCHEDULED / IN_PROGRESS / AWAITING_PARTS). This
        // avoids wasting work on resolved/cancelled bookings and matches the
        // user's requirement.
    java.util.List<Long> activeBookingIds = workOrderRepository.findActiveBookingIds();
        for (Long bookingId : activeBookingIds) {
            if (bookingId == null) continue;
            slaCalculator.isResponseBreach(bookingId);
            slaCalculator.isResolutionBreach(bookingId);
        }

    }

    public List<SlaComplianceReport> SlaCompliance() {
        updateSlaStatus();
        return slaCheckpointRepository.findSlaComplianceMetrics();
    }

    // time is calculated from the work order timestamps 
    public Double MeanTimeToRepair(){
        return null;
    }

}


