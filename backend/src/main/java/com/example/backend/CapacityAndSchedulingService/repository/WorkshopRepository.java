package com.example.backend.CapacityAndSchedulingService.repository;

import com.example.backend.CapacityAndSchedulingService.entity.Depot;
import com.example.backend.CapacityAndSchedulingService.entity.Workshop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkshopRepository
        extends JpaRepository<Workshop, Long> {

    Optional<Workshop> findByCode(String code);

    boolean existsByCode(String code);

    List<Workshop> findByDepot(Depot depot);

    List<Workshop> findByIsActiveTrue();
}
