package com.example.backend.CapacityAndSchedulingService.service;

import com.example.backend.CapacityAndSchedulingService.dto.technician.CreateTechnicianRequest;
import com.example.backend.CapacityAndSchedulingService.dto.technician.TechnicianResponse;

import java.util.List;

public interface TechnicianService {

    TechnicianResponse createTechnician(
            CreateTechnicianRequest request
    );

    TechnicianResponse getTechnician(
            Long technicianId
    );

    List<TechnicianResponse> getAllTechnicians();

    List<TechnicianResponse> getTechniciansByWorkshop(
            Long workshopId
    );

    void activateTechnician(
            Long technicianId
    );

    void deactivateTechnician(
            Long technicianId
    );
}
