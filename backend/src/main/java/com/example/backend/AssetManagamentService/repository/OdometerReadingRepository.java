package com.example.backend.AssetManagamentService.repository;

import com.example.backend.AssetManagamentService.entity.OdometerReading;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OdometerReadingRepository
        extends JpaRepository<OdometerReading, Long> {

    List<OdometerReading> findByAsset_IdOrderByReadAtDesc(
            Long assetId
    );

    Optional<OdometerReading>
    findFirstByAsset_IdOrderByReadAtDesc(
            Long assetId
    );
}