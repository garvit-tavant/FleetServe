package com.example.backend.SLA.repository;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import com.example.backend.SLA.dto.BreakdownPriority;
import com.example.backend.SLA.entity.SlaPolicy;

public interface SlaPolicyRepository extends JpaRepository<SlaPolicy,Long> {
    
     @Query("""
            SELECT p
            FROM SlaPolicy p
            WHERE p.priority = :priority
              AND p.effectiveFrom <= :reportedDate
              AND (p.effectiveTo IS NULL OR p.effectiveTo >= :reportedDate)
            """)
    List<SlaPolicy> findEffectivePolicies(@Param("priority") BreakdownPriority priority,
                                          @Param("reportedDate") LocalDate reportedDate);
}
