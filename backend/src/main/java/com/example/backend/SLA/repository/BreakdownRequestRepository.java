package com.example.backend.SLA.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

import com.example.backend.SLA.dto.BreakdownPriority;
import com.example.backend.SLA.entity.BreakdownRequest;
import com.example.backend.SLA.entity.SlaPolicy;

@Repository
public interface BreakdownRequestRepository extends JpaRepository<BreakdownRequest,Long>{

  

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

    // Used by WorkOrderServiceImpl to resolve which breakdown_request (and
    // therefore which sla_checkpoint) a work order's booking belongs to, since
    // a WorkOrder only stores bookingId. Returns null for preventive bookings
    // (no linked breakdown request).
    @Query("select b.id from BreakdownRequest b where b.booking.id = :bookingId")
    Long findIdByBookingId(@Param("bookingId") Long bookingId);


    @Query("SELECT br.slaPolicy FROM BreakdownRequest br WHERE br.id = :id")
    SlaPolicy pinnedSlaPolicyById(@Param("id") Long id);

    @Query("SELECT br.priority FROM BreakdownRequest br WHERE br.id = :id")
    BreakdownPriority priorityById(@Param("id") Long id);

   
}
