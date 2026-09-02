package com.example.backend.CapacityAndSchedulingService.repository;

import com.example.backend.CapacityAndSchedulingService.entity.Capability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CapabilityRepository
        extends JpaRepository<Capability, String> {

    Optional<Capability> findByName(String name);

    boolean existsByName(String name);
}