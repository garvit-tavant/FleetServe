package com.example.backend.CapacityAndSchedulingService.service.impl;

import com.example.backend.AssetManagamentService.exception.BusinessValidationException;
import com.example.backend.AssetManagamentService.exception.DuplicateResourceException;
import com.example.backend.AssetManagamentService.exception.ResourceNotFoundException;
import com.example.backend.CapacityAndSchedulingService.dto.depot.CreateDepotRequest;
import com.example.backend.CapacityAndSchedulingService.dto.depot.DepotResponse;
import com.example.backend.CapacityAndSchedulingService.entity.Depot;
import com.example.backend.CapacityAndSchedulingService.mapper.CapacitySchedulingMapper;
import com.example.backend.CapacityAndSchedulingService.repository.DepotRepository;
import com.example.backend.CapacityAndSchedulingService.service.DepotService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class DepotServiceImpl implements DepotService {

    private final DepotRepository depotRepository;

    private final CapacitySchedulingMapper mapper;

    public DepotServiceImpl(
            DepotRepository depotRepository,
            CapacitySchedulingMapper mapper
    ) {
        this.depotRepository = depotRepository;
        this.mapper = mapper;
    }

    @Override
    public DepotResponse createDepot(
            CreateDepotRequest request
    ) {

        if (depotRepository.existsByCode(
                request.getCode()
        )) {

            throw new DuplicateResourceException(
                    "Depot already exists with code "
                            + request.getCode()
            );
        }

        Depot depot = new Depot();

        depot.setCode(
                request.getCode()
        );

        depot.setRegion(
                request.getRegion()
        );

        depot.setActive(true);

        depot = depotRepository.save(
                depot
        );

        return mapper.toDepotResponse(
                depot
        );
    }

    @Override
    public DepotResponse getDepot(
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

        return mapper.toDepotResponse(
                depot
        );
    }

    @Override
    public List<DepotResponse> getAllDepots() {

        return depotRepository
                .findAll()
                .stream()
                .map(mapper::toDepotResponse)
                .toList();
    }

    @Override
    public void activateDepot(
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
        if (Boolean.TRUE.equals(depot.getActive())) {
            throw new BusinessValidationException(
                    "Depot is already active"
            );
        }
        depot.setActive(true);

        depotRepository.save(
                depot
        );
    }

    @Override
    public void deactivateDepot(
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
        if (Boolean.FALSE.equals(depot.getActive())) {
            throw new BusinessValidationException(
                    "Depot is already inactive"
            );
        }
        depot.setActive(false);

        depotRepository.save(
                depot
        );
    }
}