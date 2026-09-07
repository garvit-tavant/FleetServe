package com.example.backend.SLA.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

import com.example.backend.SLA.entity.BreakdownRequest;

@Repository
public interface BreakdownRequestRepository extends JpaRepository<BreakdownRequest,Long>{

    // ans: none of the methods below matched a Spring Data derived-query keyword
    // (names like "responsetimebyid" don't resolve to any property path), so they
    // would all throw QueryCreationException at startup. Added explicit @Query for
    // each, matching what the pre-existing comment already specified.

    @Query("select b.slaPolicy.responseTargetMinutes from BreakdownRequest b where b.id = :breakdownId")
    long responsetimebyid(@Param("breakdownId") long breakdownID);

    @Query("select b.slaPolicy.resolutionTargetMinutes from BreakdownRequest b where b.id = :breakdownId")
    long resolutiontimebyid(@Param("breakdownId") long breakdownID);

    // A breakdown may not have a booking yet (booked only after triage), so this
    // can legitimately return null before then - callers must handle that.
    @Query("select b.booking.workshop.id from BreakdownRequest b where b.id = :breakdownId")
    Long workshopIDfromid(@Param("breakdownId") long breadkdownID);

    @Query("select b.reportedAt from BreakdownRequest b where b.id = :breakdownId")
    OffsetDateTime requestraisedtimebyid(@Param("breakdownId") long breakdownID);

    @Query("select b.priority from BreakdownRequest b where b.id = :breakdownId")
    String prioritybyid(@Param("breakdownId") long breakdownID);

    @Query("select b.id, b.booking.id from BreakdownRequest b where b.status not in ('COMPLETED', 'CANCELED')")
    List<Long[]> requestsnothandleded();
}
