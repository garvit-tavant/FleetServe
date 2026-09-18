package com.example.backend.SLA.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.ExecutionService.repository.WorkOrderRepository;

/**
 * Extracted from {@link SlaService#updateSlaStatus()} to fix a Spring
 * self-invocation bug: {@code SlaService} is {@code @Transactional(readOnly = true)}
 * at the class level, and {@code SlaCompliance()} used to call
 * {@code updateSlaStatus()} directly (i.e. {@code this.updateSlaStatus()}).
 * Because that call bypasses the Spring AOP proxy entirely (self-invocation),
 * a {@code @Transactional(propagation = REQUIRES_NEW)} annotation placed
 * directly on {@code updateSlaStatus()} was silently ignored, so the method's
 * {@code @Modifying} UPDATE queries (via {@link SlaCalculator#isResponseBreach}
 * / {@link SlaCalculator#isResolutionBreach}) still ran inside the inherited
 * read-only transaction and PostgreSQL rejected them with:
 * "ERROR: cannot execute UPDATE in a read-only transaction".
 *
 * <p>Moving this logic into its own bean means callers invoke it through the
 * Spring proxy (external call), so {@code @Transactional(propagation = REQUIRES_NEW)}
 * actually takes effect and runs in its own writable transaction.
 */
@Service
public class SlaStatusUpdater {

    private final SlaCalculator slaCalculator;
    private final WorkOrderRepository workOrderRepository;

    public SlaStatusUpdater(SlaCalculator slaCalculator, WorkOrderRepository workOrderRepository) {
        this.slaCalculator = slaCalculator;
        this.workOrderRepository = workOrderRepository;
    }

    /**
     * Updates all sla_checkpoints for bookings that currently have an active
     * WorkOrder (SCHEDULED / IN_PROGRESS / AWAITING_PARTS).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateSlaStatus() {
        List<Long> activeBookingIds = workOrderRepository.findActiveBookingIds();
        for (Long bookingId : activeBookingIds) {
            if (bookingId == null) continue;
            slaCalculator.isResponseBreach(bookingId);
            slaCalculator.isResolutionBreach(bookingId);
        }
    }
}
