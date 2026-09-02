package com.example.backend.CapacityAndSchedulingService.repository;

import com.example.backend.CapacityAndSchedulingService.entity.BayCapability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BayCapabilityRepository
        extends JpaRepository<BayCapability, Long> {

    List<BayCapability> findByBay_Id(
            Long bayId
    );

    List<BayCapability> findByCapability_CapabilityCode(
            String capabilityCode
    );

    Optional<BayCapability>
    findByBay_IdAndCapability_CapabilityCode(
            Long bayId,
            String capabilityCode
    );

    boolean existsByBay_IdAndCapability_CapabilityCode(
            Long bayId,
            String capabilityCode
    );
}