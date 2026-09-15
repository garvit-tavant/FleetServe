package com.example.backend.CapacityAndSchedulingService.service.impl;

import com.example.backend.AssetManagamentService.exception.DuplicateResourceException;
import com.example.backend.AssetManagamentService.exception.ResourceNotFoundException;
import com.example.backend.CapacityAndSchedulingService.dto.workshop.CreateWorkshopRequest;
import com.example.backend.CapacityAndSchedulingService.dto.workshop.WorkshopResponse;
import com.example.backend.CapacityAndSchedulingService.entity.Depot;
import com.example.backend.CapacityAndSchedulingService.entity.Workshop;
import com.example.backend.CapacityAndSchedulingService.mapper.CapacitySchedulingMapper;
import com.example.backend.CapacityAndSchedulingService.repository.DepotRepository;
import com.example.backend.CapacityAndSchedulingService.repository.WorkshopRepository;
import com.example.backend.CapacityAndSchedulingService.service.WorkshopService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class WorkshopServiceImpl implements WorkshopService {

    private final WorkshopRepository workshopRepository;

    private final DepotRepository depotRepository;

    private final CapacitySchedulingMapper mapper;

    public WorkshopServiceImpl(
            WorkshopRepository workshopRepository,
            DepotRepository depotRepository,
            CapacitySchedulingMapper mapper
    ) {
        this.workshopRepository = workshopRepository;
        this.depotRepository = depotRepository;
        this.mapper = mapper;
    }

    @Override
    public WorkshopResponse createWorkshop(
            CreateWorkshopRequest request
    ) {

        if (workshopRepository.existsByCode(
                request.getCode()
        )) {

            throw new DuplicateResourceException(
                    "Workshop already exists with code "
                            + request.getCode()
            );
        }

        Depot depot = depotRepository
                .findById(request.getDepotId())
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Depot not found with id "
                                                + request.getDepotId()
                                )
                );

        Workshop workshop = new Workshop();

        workshop.setCode(
                request.getCode()
        );

        workshop.setDepot(
                depot
        );

        workshop.setTimeZone(
                request.getTimeZone()
        );

        workshop.setActive(true);

        workshop = workshopRepository.save(
                workshop
        );

        return mapper.toWorkshopResponse(
                workshop
        );
    }

    @Override
    public WorkshopResponse getWorkshop(
            Long workshopId
    ) {

        Workshop workshop =
                getWorkshopEntity(workshopId);

        return mapper.toWorkshopResponse(
                workshop
        );
    }

    @Override
    public List<WorkshopResponse> getAllWorkshops() {

        return workshopRepository
                .findAll()
                .stream()
                .map(mapper::toWorkshopResponse)
                .toList();
    }

    @Override
    public List<WorkshopResponse> getWorkshopsByDepot(
            Long depotId
    ) {

        Depot depot = depotRepository
                .findById(depotId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Depot not found with id "
                                                + depotId
                                )
                );

        return workshopRepository
                .findByDepot(depot)
                .stream()
                .map(mapper::toWorkshopResponse)
                .toList();
    }

    @Override
    public void activateWorkshop(
            Long workshopId
    ) {

        Workshop workshop =
                getWorkshopEntity(workshopId);

        workshop.setActive(true);

        workshopRepository.save(
                workshop
        );
    }

    @Override
    public void deactivateWorkshop(
            Long workshopId
    ) {

        Workshop workshop =
                getWorkshopEntity(workshopId);

        workshop.setActive(false);

        workshopRepository.save(
                workshop
        );
    }

    private Workshop getWorkshopEntity(
            Long workshopId
    ) {

        return workshopRepository
                .findById(workshopId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Workshop not found with id "
                                                + workshopId
                                )
                );
    }
}