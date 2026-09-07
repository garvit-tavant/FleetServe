package com.example.backend.InventoryService.repository;

import com.example.backend.InventoryService.entity.Part;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PartRepository
        extends JpaRepository<Part, Long> {

    Optional<Part> findByPartNumber(
            String partNumber
    );

    boolean existsByPartNumber(
            String partNumber
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM Part p
            WHERE p.id = :partId
            """)
    Optional<Part> findByIdForInventoryUpdate(
            @Param("partId") Long partId
    );
}