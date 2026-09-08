package com.example.backend.SLA.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.example.backend.SLA.dto.SlaEvaluationRow;
import com.example.backend.SLA.entity.BreakdownRequest;
import com.example.backend.SLA.status.BreakdownStatus;

@Repository
public interface BreakdownRequestRepository extends JpaRepository<BreakdownRequest, Long> {

    /**
     * Breakdowns whose clocks are still running.
     *
     * <p>Every join is a LEFT JOIN on purpose. A breakdown that has been reported
     * but not yet booked has no booking and therefore no workshop, and that is
     * precisely the population the response clock exists to police; an inner
     * join would silently drop it. The checkpoint row may also not exist yet.
     *
     * <p>The status filter uses the vocabulary the schema actually permits
     * (REPORTED, TRIAGED, BOOKED, IN_PROGRESS, RESOLVED, CANCELLED).
     * <p>The status filter is passed as a parameter rather than written as a
     * literal, because {@code status} is an {@code @Enumerated} field and a bare
     * string literal would not compare against it.
     */
    @Query("""
            select new com.example.backend.SLA.dto.SlaEvaluationRow(
                b.id,
                b.priority,
                b.reportedAt,
                w.id,
                p.responseTargetMinutes,
                p.resolutionTargetMinutes,
                p.calendarBasis,
                c.respondedAt,
                c.resolvedAt,
                c.responseBreach,
                c.resolutionBreach)
            from BreakdownRequest b
            join b.slaPolicy p
            left join b.booking bk
            left join bk.workshop w
            left join SlaCheckpoint c on c.breakdownRequestId = b.id
            where b.status not in :terminalStatuses
            """)
    List<SlaEvaluationRow> findOpenForEvaluation(
            @Param("terminalStatuses") Collection<BreakdownStatus> terminalStatuses);

    default List<SlaEvaluationRow> findOpenForEvaluation() {
        return findOpenForEvaluation(BreakdownStatus.terminalStatuses());
    }

    /** Single-breakdown variant of {@link #findOpenForEvaluation()}. */
    @Query("""
            select new com.example.backend.SLA.dto.SlaEvaluationRow(
                b.id,
                b.priority,
                b.reportedAt,
                w.id,
                p.responseTargetMinutes,
                p.resolutionTargetMinutes,
                p.calendarBasis,
                c.respondedAt,
                c.resolvedAt,
                c.responseBreach,
                c.resolutionBreach)
            from BreakdownRequest b
            join b.slaPolicy p
            left join b.booking bk
            left join bk.workshop w
            left join SlaCheckpoint c on c.breakdownRequestId = b.id
            where b.id = :breakdownId
            """)
    Optional<SlaEvaluationRow> findForEvaluation(@Param("breakdownId") long breakdownId);

    /** Breakdowns in a given state, most recently reported first. */
    Page<BreakdownRequest> findByStatusOrderByReportedAtDesc(
            BreakdownStatus status, Pageable pageable);

    Page<BreakdownRequest> findAllByOrderByReportedAtDesc(Pageable pageable);
}
