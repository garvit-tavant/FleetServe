package com.example.backend.SLA.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

import com.example.backend.SLA.entity.AwaitingRaised;

@Repository
public interface AwaitingRaisedRepository extends JpaRepository<AwaitingRaised, Long> {

    @Query("""
            select a
            from AwaitingRaised a
            where a.breakdownRequest.id = :breakdownId
            order by a.raisedAt asc
            """)
    List<AwaitingRaised> findByBreakdownRequestId(@Param("breakdownId") long breakdownId);

    /** Pause rows for several breakdowns at once, to keep the sweep off N+1 queries. */
    @Query("""
            select a
            from AwaitingRaised a
            where a.breakdownRequest.id in :breakdownIds
            order by a.raisedAt asc
            """)
    List<AwaitingRaised> findByBreakdownRequestIds(@Param("breakdownIds") List<Long> breakdownIds);
}
