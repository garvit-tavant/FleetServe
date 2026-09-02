package com.example.backend.CapacityAndSchedulingService.service;

import com.example.backend.CapacityAndSchedulingService.dto.skill.CreateSkillRequest;
import com.example.backend.CapacityAndSchedulingService.dto.skill.SkillResponse;

import java.util.List;

public interface SkillService {

    SkillResponse createSkill(
            CreateSkillRequest request
    );

    SkillResponse getSkill(
            String skillCode
    );

    List<SkillResponse> getAllSkills();
}