package com.example.backend.CapacityAndSchedulingService.repository;

import com.example.backend.CapacityAndSchedulingService.entity.Bay;
import com.example.backend.CapacityAndSchedulingService.entity.Workshop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BayRepository
        extends JpaRepository<Bay, Long> {

    List<Bay> findByWorkshop(Workshop workshop);

    List<Bay> findByWorkshop_Id(Long workshopId);

    List<Bay> findByWorkshop_IdAndIsActiveTrue(
            Long workshopId
    );

    Optional<Bay> findByBayCode(
            String bayCode
    );

    boolean existsByBayCode(
            String bayCode
    );
}