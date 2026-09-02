package com.example.backend.CapacityAndSchedulingService.service;

import com.example.backend.CapacityAndSchedulingService.dto.workshop.CreateWorkshopRequest;
import com.example.backend.CapacityAndSchedulingService.dto.workshop.WorkshopResponse;

import java.util.List;

public interface WorkshopService {

    WorkshopResponse createWorkshop(
            CreateWorkshopRequest request
    );

    WorkshopResponse getWorkshop(
            Long workshopId
    );

    List<WorkshopResponse> getAllWorkshops();

    List<WorkshopResponse> getWorkshopsByDepot(
            Long depotId
    );

    void activateWorkshop(
            Long workshopId
    );

    void deactivateWorkshop(
            Long workshopId
    );
}