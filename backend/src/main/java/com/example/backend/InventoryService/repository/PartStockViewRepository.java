package com.example.backend.InventoryService.repository;

import com.example.backend.InventoryService.entity.PartStockView;
import com.example.backend.InventoryService.entity.PartStockViewId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PartStockViewRepository
        extends JpaRepository<
        PartStockView,
        PartStockViewId> {

    Optional<PartStockView>
    findByIdPartIdAndIdWorkshopId(
            Long partId,
            Long workshopId
    );

    List<PartStockView>
    findByIdWorkshopId(
            Long workshopId
    );

    List<PartStockView>
    findByIdPartId(
            Long partId
    );
}