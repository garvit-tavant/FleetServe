package com.example.backend.CapacityAndSchedulingService.service.impl;

import com.example.backend.AssetManagamentService.exception.DuplicateResourceException;
import com.example.backend.AssetManagamentService.exception.ResourceNotFoundException;
import com.example.backend.CapacityAndSchedulingService.dto.skill.CreateSkillRequest;
import com.example.backend.CapacityAndSchedulingService.dto.skill.SkillResponse;
import com.example.backend.CapacityAndSchedulingService.entity.Skill;
import com.example.backend.CapacityAndSchedulingService.mapper.CapacitySchedulingMapper;
import com.example.backend.CapacityAndSchedulingService.repository.SkillRepository;
import com.example.backend.CapacityAndSchedulingService.service.SkillService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SkillServiceImpl
        implements SkillService {

    private final SkillRepository skillRepository;

    private final CapacitySchedulingMapper mapper;

    public SkillServiceImpl(
            SkillRepository skillRepository,
            CapacitySchedulingMapper mapper
    ) {
        this.skillRepository = skillRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public SkillResponse createSkill(
            CreateSkillRequest request
    ) {

        String normalizedSkillCode =
                request.getSkillCode()
                        .trim()
                        .toUpperCase();

        String normalizedSkillName =
                request.getName()
                        .trim()
                        .toUpperCase();

        if (skillRepository.existsById(
                normalizedSkillCode
        )) {

            throw new DuplicateResourceException(
                    "Skill already exists with code "
                            + normalizedSkillCode
            );
        }

        Skill skill = new Skill();

        skill.setSkillCode(
                normalizedSkillCode
        );

        skill.setName(
                normalizedSkillName
        );

        skill.setDescription(
                request.getDescription()
                        .trim()
        );

        skill.setTime(
                request.getTime()
        );

        Skill savedSkill =
                skillRepository.save(skill);

        return mapper.toSkillResponse(
                savedSkill
        );
    }

    @Override
    public SkillResponse getSkill(
            String skillCode
    ) {

        Skill skill =
                skillRepository
                        .findById(
                                skillCode
                                        .trim()
                                        .toUpperCase()
                        )
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Skill not found with code "
                                                        + skillCode
                                        )
                        );

        return mapper.toSkillResponse(
                skill
        );
    }

    @Override
    public List<SkillResponse> getAllSkills() {

        return skillRepository
                .findAll()
                .stream()
                .map(
                        mapper::toSkillResponse
                )
                .toList();
    }
}