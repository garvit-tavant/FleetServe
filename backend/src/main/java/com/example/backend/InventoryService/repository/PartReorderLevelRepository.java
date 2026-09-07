package com.example.backend.InventoryService.repository;

import com.example.backend.InventoryService.entity.PartReorderLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PartReorderLevelRepository
        extends JpaRepository<PartReorderLevel, Long> {

    Optional<PartReorderLevel>
    findByPart_IdAndWorkshop_Id(
            Long partId,
            Long workshopId
    );

    boolean existsByPart_IdAndWorkshop_Id(
            Long partId,
            Long workshopId
    );

    List<PartReorderLevel>
    findByWorkshop_Id(
            Long workshopId
    );

    List<PartReorderLevel>
    findByPart_Id(
            Long partId
    );
}