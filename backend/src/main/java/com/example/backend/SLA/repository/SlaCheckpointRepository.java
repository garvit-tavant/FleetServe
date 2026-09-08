package com.example.backend.SLA.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

import com.example.backend.SLA.dto.SlaComplianceReport;
import com.example.backend.SLA.entity.SlaCheckpoint;
import com.example.backend.SLA.status.BreakdownStatus;

@Repository
public interface SlaCheckpointRepository extends JpaRepository<SlaCheckpoint, Long> {

    @Modifying
    @Query("""
            update SlaCheckpoint c
               set c.responseBreach = :breached
             where c.breakdownRequestId = :breakdownId
               and (c.responseBreach is null or c.responseBreach <> :breached)
            """)
    int updateResponseBreach(
            @Param("breakdownId") Long breakdownId,
            @Param("breached") boolean breached);

    @Modifying
    @Query("""
            update SlaCheckpoint c
               set c.resolutionBreach = :breached
             where c.breakdownRequestId = :breakdownId
               and (c.resolutionBreach is null or c.resolutionBreach <> :breached)
            """)
    int updateResolutionBreach(
            @Param("breakdownId") Long breakdownId,
            @Param("breached") boolean breached);

    @Modifying
    @Query("""
            update SlaCheckpoint c
               set c.respondedAt = :respondedAt
             where c.breakdownRequestId = :breakdownId
               and c.respondedAt is null
            """)
    int recordFirstResponse(
            @Param("breakdownId") Long breakdownId,
            @Param("respondedAt") OffsetDateTime respondedAt);

    /**
     * Service-level compliance percentage by priority (US-4.2).
     *
     * <p>Aggregated in SQL rather than by looping in Java, which the
     * specification treats as an automatic fail for this story. A case counts as
     * compliant only when neither clock breached.
     *
     * <p>Scoped to RESOLVED, the terminal success state the schema actually
     * defines. Cancelled breakdowns are excluded because they were never worked.
     */
    @Query("""
            select new com.example.backend.SLA.dto.SlaComplianceReport(
                b.priority,
                count(c.breakdownRequestId),
                sum(case when coalesce(c.responseBreach, false) = false
                          and coalesce(c.resolutionBreach, false) = false
                         then 1L else 0L end),
                (sum(case when coalesce(c.responseBreach, false) = false
                           and coalesce(c.resolutionBreach, false) = false
                          then 1L else 0L end) * 100.0)
                    / count(c.breakdownRequestId))
            from SlaCheckpoint c
            join c.breakdownRequest b
            where b.status = :resolvedStatus
            group by b.priority
            order by b.priority
            """)
    List<SlaComplianceReport> findSlaComplianceMetrics(
            @Param("resolvedStatus") BreakdownStatus resolvedStatus);

    default List<SlaComplianceReport> findSlaComplianceMetrics() {
        return findSlaComplianceMetrics(BreakdownStatus.RESOLVED);
    }
}
