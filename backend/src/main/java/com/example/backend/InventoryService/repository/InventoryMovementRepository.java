package com.example.backend.InventoryService.repository;

import com.example.backend.InventoryService.entity.InventoryMovement;
import com.example.backend.InventoryService.entity.InventoryMovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryMovementRepository
        extends JpaRepository<InventoryMovement, Long> {

    List<InventoryMovement>
    findByPart_IdOrderByOccurredAtDesc(
            Long partId
    );

    List<InventoryMovement>
    findByWorkshop_IdOrderByOccurredAtDesc(
            Long workshopId
    );

    List<InventoryMovement>
    findByPart_IdAndWorkshop_IdOrderByOccurredAtDesc(
            Long partId,
            Long workshopId
    );

    Page<InventoryMovement>
    findByPart_Id(
            Long partId,
            Pageable pageable
    );

    Page<InventoryMovement>
    findByWorkshop_Id(
            Long workshopId,
            Pageable pageable
    );

    Page<InventoryMovement>
    findByPart_IdAndWorkshop_Id(
            Long partId,
            Long workshopId,
            Pageable pageable
    );

    List<InventoryMovement>
    findByTransferReference(
            String transferReference
    );

    List<InventoryMovement>
    findByMovementType(
            InventoryMovementType movementType
    );
}