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

import jakarta.transaction.Transactional;

@Repository
public interface SlaCheckpointRepository extends JpaRepository<SlaCheckpoint , Long> {


    @Query("select c.responseBreach from SlaCheckpoint c where c.breakdownRequestId = :breakdownId")
    Boolean responseBreachfindbyID(@Param("breakdownId") long breakdownID);

    @Query("select c.resolutionBreach from SlaCheckpoint c where c.breakdownRequestId = :breakdownId")
    Boolean resolutionBreachfindbyID(@Param("breakdownId") long breadkdownID);

    @Query("select c.respondedAt from SlaCheckpoint c where c.breakdownRequestId = :breakdownId")
    OffsetDateTime respondedatbyID(@Param("breakdownId") Long breakdownID);

    @Query("select c.resolvedAt from SlaCheckpoint c where c.breakdownRequestId = :breakdownId")
    OffsetDateTime resolutionByID(@Param("breakdownId") Long breakdownID);

    @Modifying
    @Transactional
    @Query ("update SlaCheckpoint c set c.responseBreach = true where c.breakdownRequestId = :breakdownId")
    void updateResponseBreach(@Param("breakdownId") Long breakdownId);

    @Modifying
    @Transactional
    @Query ("update SlaCheckpoint c set c.resolutionBreach = true where c.breakdownRequestId = :breakdownId")
    void updateResolutionBreach(@Param("breakdownId") Long breakdownId);

    //we want the priority wise compliance report like which request have breached from the one which has not been completed!!
    @Query("""
        SELECT sc.breakdownRequest.slaPolicy.priority AS priority,
        COUNT(sc.breakdownRequest.id) AS evaluatedCases,
        (COUNT(sc.breakdownRequest.id) - SUM(CASE WHEN sc.responseBreach = true THEN 1 ELSE 0 END)) AS compliantCases,
        ((COUNT(sc.breakdownRequest.id) - SUM(CASE WHEN sc.responseBreach = true THEN 1 ELSE 0 END)) * 100.0 / COUNT(sc.breakdownRequest.id)) AS compliancePercent
        FROM SlaCheckpoint sc
        WHERE sc.breakdownRequest.status = 'COMPLETED'
        GROUP BY sc.breakdownRequest.slaPolicy.priority
        ORDER BY sc.breakdownRequest.slaPolicy.priority
        """)
    List<SlaComplianceReport> findSlaComplianceMetrics();






}