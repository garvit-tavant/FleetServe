package com.example.backend.AssetManagamentService.repository;

import com.example.backend.AssetManagamentService.entity.AssetClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssetClassRepository
        extends JpaRepository<AssetClass, Long> {

    Optional<AssetClass> findByCode(String code);

    boolean existsByCode(String code);
}