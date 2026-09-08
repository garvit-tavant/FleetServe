package com.example.backend.SLA.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.backend.SLA.entity.SlaPolicy;
import com.example.backend.SLA.status.BreakdownPriority;

public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, Long> {

    /**
     * The policy version in force for a priority on a given date.
     *
     * <p>Targets are effective-dated, so raising a breakdown pins the version
     * current at intake. Changing a target later inserts a new row and leaves
     * history alone, which is why this takes the latest effective_from that has
     * started rather than assuming one row per priority.
     */
    @Query("""
            select p
            from SlaPolicy p
            where p.priority = :priority
              and p.effectiveFrom <= :onDate
              and (p.effectiveTo is null or p.effectiveTo >= :onDate)
            order by p.effectiveFrom desc
            """)
    Optional<SlaPolicy> findEffectivePolicy(
            @Param("priority") BreakdownPriority priority,
            @Param("onDate") LocalDate onDate,
            Limit limit);

    default Optional<SlaPolicy> findEffectivePolicy(
            BreakdownPriority priority, LocalDate onDate) {
        return findEffectivePolicy(priority, onDate, Limit.of(1));
    }
}
