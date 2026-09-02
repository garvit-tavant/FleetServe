package com.example.backend.CapacityAndSchedulingService.repository;

import com.example.backend.CapacityAndSchedulingService.entity.Depot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepotRepository
        extends JpaRepository<Depot, Long> {

    Optional<Depot> findByCode(String code);

    boolean existsByCode(String code);
}