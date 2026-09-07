package com.example.backend.InventoryService.service.impl;

import com.example.backend.AssetManagamentService.exception.BusinessValidationException;
import com.example.backend.AssetManagamentService.exception.ResourceNotFoundException;
import com.example.backend.CapacityAndSchedulingService.entity.Workshop;
import com.example.backend.CapacityAndSchedulingService.repository.WorkshopRepository;
import com.example.backend.InventoryService.dtos.partstock.PartStockResponse;
import com.example.backend.InventoryService.entity.Part;
import com.example.backend.InventoryService.entity.PartReorderLevel;
import com.example.backend.InventoryService.entity.PartStockView;
import com.example.backend.InventoryService.mapper.PartStockMapper;
import com.example.backend.InventoryService.repository.PartReorderLevelRepository;
import com.example.backend.InventoryService.repository.PartRepository;
import com.example.backend.InventoryService.repository.PartStockViewRepository;
import com.example.backend.InventoryService.service.PartStockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PartStockServiceImpl
        implements PartStockService {

    private final PartStockViewRepository stockViewRepository;
    private final PartRepository partRepository;
    private final PartReorderLevelRepository reorderLevelRepository;
    private final WorkshopRepository workshopRepository;

    public PartStockServiceImpl(
            PartStockViewRepository stockViewRepository,
            PartRepository partRepository,
            PartReorderLevelRepository reorderLevelRepository,
            WorkshopRepository workshopRepository
    ) {
        this.stockViewRepository = stockViewRepository;
        this.partRepository = partRepository;
        this.reorderLevelRepository =
                reorderLevelRepository;
        this.workshopRepository = workshopRepository;
    }

    @Override
    public PartStockResponse getStock(
            Long partId,
            Long workshopId
    ) {
        validateId(partId, "Part ID");
        validateId(workshopId, "Workshop ID");

        Part part = getPartEntity(partId);
        Workshop workshop =
                getWorkshopEntity(workshopId);

        PartStockView stockView =
                stockViewRepository
                        .findByIdPartIdAndIdWorkshopId(
                                partId,
                                workshopId
                        )
                        .orElseGet(() ->
                                createZeroStockView(
                                        partId,
                                        workshopId
                                )
                        );

        BigDecimal reorderLevel =
                getReorderLevel(partId, workshopId);

        return PartStockMapper.toResponse(
                stockView,
                part,
                workshop.getCode(),
                reorderLevel
        );
    }

    @Override
    public List<PartStockResponse> getStockForWorkshop(
            Long workshopId
    ) {
        validateId(workshopId, "Workshop ID");

        Workshop workshop =
                getWorkshopEntity(workshopId);

        List<PartStockView> stockRows =
                stockViewRepository
                        .findByIdWorkshopId(workshopId);

        Map<Long, PartReorderLevel> reorderLevels =
                reorderLevelRepository
                        .findByWorkshop_Id(workshopId)
                        .stream()
                        .collect(Collectors.toMap(
                                level -> level.getPart().getId(),
                                Function.identity()
                        ));

        return stockRows.stream()
                .map(stockView -> {
                    Long partId =
                            stockView.getId().getPartId();

                    Part part = getPartEntity(partId);

                    BigDecimal reorderLevel =
                            reorderLevels.containsKey(partId)
                                    ? reorderLevels
                                    .get(partId)
                                    .getReorderLevel()
                                    : BigDecimal.ZERO;

                    return PartStockMapper.toResponse(
                            stockView,
                            part,
                            workshop.getCode(),
                            reorderLevel
                    );
                })
                .toList();
    }

    @Override
    public List<PartStockResponse> getStockForPart(
            Long partId
    ) {
        validateId(partId, "Part ID");

        Part part = getPartEntity(partId);

        return stockViewRepository
                .findByIdPartId(partId)
                .stream()
                .map(stockView -> {
                    Long workshopId =
                            stockView.getId()
                                    .getWorkshopId();

                    Workshop workshop =
                            getWorkshopEntity(workshopId);

                    return PartStockMapper.toResponse(
                            stockView,
                            part,
                            workshop.getCode(),
                            getReorderLevel(
                                    partId,
                                    workshopId
                            )
                    );
                })
                .toList();
    }

    @Override
    public List<PartStockResponse> getReorderAlerts(
            Long workshopId
    ) {
        validateId(workshopId, "Workshop ID");

        Workshop workshop =
                getWorkshopEntity(workshopId);

        return reorderLevelRepository
                .findByWorkshop_Id(workshopId)
                .stream()
                .map(level -> {
                    Long partId =
                            level.getPart().getId();

                    PartStockView stockView =
                            stockViewRepository
                                    .findByIdPartIdAndIdWorkshopId(
                                            partId,
                                            workshopId
                                    )
                                    .orElseGet(() ->
                                            createZeroStockView(
                                                    partId,
                                                    workshopId
                                            )
                                    );

                    return PartStockMapper.toResponse(
                            stockView,
                            level.getPart(),
                            workshop.getCode(),
                            level.getReorderLevel()
                    );
                })
                .filter(response ->
                        Boolean.TRUE.equals(
                                response.getReorderRequired()
                        )
                )
                .toList();
    }

    private BigDecimal getReorderLevel(
            Long partId,
            Long workshopId
    ) {
        return reorderLevelRepository
                .findByPart_IdAndWorkshop_Id(
                        partId,
                        workshopId
                )
                .map(PartReorderLevel::getReorderLevel)
                .orElse(BigDecimal.ZERO);
    }

    private PartStockView createZeroStockView(
            Long partId,
            Long workshopId
    ) {
        PartStockView stockView =
                new PartStockView();

        com.example.backend.InventoryService.entity.PartStockViewId id =
                new com.example.backend.InventoryService.entity.PartStockViewId();

        id.setPartId(partId);
        id.setWorkshopId(workshopId);

        stockView.setId(id);
        stockView.setOnHand(BigDecimal.ZERO);

        return stockView;
    }

    private Part getPartEntity(
            Long partId
    ) {
        return partRepository
                .findById(partId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Part not found with id "
                                        + partId
                        )
                );
    }

    private Workshop getWorkshopEntity(
            Long workshopId
    ) {
        return workshopRepository
                .findById(workshopId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Workshop not found with id "
                                        + workshopId
                        )
                );
    }

    private void validateId(
            Long id,
            String fieldName
    ) {
        if (id == null || id <= 0) {
            throw new BusinessValidationException(
                    fieldName + " must be a positive number"
            );
        }
    }
}