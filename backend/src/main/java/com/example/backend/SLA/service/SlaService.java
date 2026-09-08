package com.example.backend.SLA.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.SLA.dto.SlaClockSnapshot;
import com.example.backend.SLA.dto.SlaComplianceReport;
import com.example.backend.SLA.repository.SlaCheckpointRepository;

/**
 * Service-level facade for controllers and scheduled jobs.
 */
@Service
@Transactional(readOnly = true)
public class SlaService {

    private final SlaClockService slaClockService;
    private final SlaCheckpointRepository slaCheckpointRepository;

    public SlaService(
            SlaClockService slaClockService,
            SlaCheckpointRepository slaCheckpointRepository) {
        this.slaClockService = slaClockService;
        this.slaCheckpointRepository = slaCheckpointRepository;
    }

    /** Both clocks for one breakdown, without writing anything. */
    public SlaClockSnapshot clockStatus(long breakdownId) {
        return slaClockService.evaluate(breakdownId);
    }

    /**
     * The "soon to breach" list for US-4.1, ordered most urgent first.
     *
     * <p>Writes breach flags, so it deliberately overrides the read-only default
     * of this class.
     */
    @Transactional
    public List<SlaClockSnapshot> refreshAndListOpenClocks() {
        List<SlaClockSnapshot> snapshots = slaClockService.refreshOpenBreakdowns();
        return snapshots.stream()
                .sorted((left, right) -> Long.compare(
                        Math.min(left.responseRemainingMinutes(), left.resolutionRemainingMinutes()),
                        Math.min(right.responseRemainingMinutes(), right.resolutionRemainingMinutes())))
                .toList();
    }

    /** Compliance percentage by priority (US-4.2), aggregated in SQL. */
    public List<SlaComplianceReport> complianceByPriority() {
        return slaCheckpointRepository.findSlaComplianceMetrics();
    }
}
