package com.example.backend.AssetManagamentService.repository;

import com.example.backend.AssetManagamentService.entity.Asset;
import com.example.backend.AssetManagamentService.status.AssetStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long> {

    Optional<Asset> findByVin(String vin);

    boolean existsByVin(String vin);

    List<Asset> findByStatus(AssetStatus status);

    List<Asset> findByAssetClass_Id(Long assetClassId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
       SELECT a
       FROM Asset a
       WHERE a.id = :assetId
       """)
    Optional<Asset> findAssetForOdometerUpdate(
            @Param("assetId") Long assetId
    );
}