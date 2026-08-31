package com.example.backend.AssetManagamentService.repository;

import com.example.backend.AssetManagamentService.entity.MaintenancePlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MaintenancePlanRepository
        extends JpaRepository<MaintenancePlan, Long> {

    Optional<MaintenancePlan> findByCode(String code);

    boolean existsByCode(String code);
}