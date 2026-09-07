package com.example.backend.SLA.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

import com.example.backend.SLA.entity.AwaitingRaised;

@Repository
public interface AwaitingRaisedRepository extends JpaRepository<AwaitingRaised,Long>{


    @Query("select a from AwaitingRaised a where a.breakdownRequest.id = :breakdownId")
    List<AwaitingRaised> getBreakdownRequestId(@Param("breakdownId") long breadkdownID);
}

