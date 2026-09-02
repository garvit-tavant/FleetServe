package com.example.backend.CapacityAndSchedulingService.service;

import com.example.backend.CapacityAndSchedulingService.dto.technicianskill.AssignSkillRequest;
import com.example.backend.CapacityAndSchedulingService.dto.technicianskill.TechnicianSkillResponse;

import java.util.List;

public interface TechnicianSkillService {

    void assignSkillToTechnician(
            Long technicianId,
            AssignSkillRequest request
    );

    void removeSkillFromTechnician(
            Long technicianId,
            String skillCode
    );

    List<TechnicianSkillResponse>
    getSkillsForTechnician(
            Long technicianId
    );
}