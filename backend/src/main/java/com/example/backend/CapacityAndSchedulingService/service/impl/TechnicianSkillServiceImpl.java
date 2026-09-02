package com.example.backend.CapacityAndSchedulingService.service.impl;

import com.example.backend.AssetManagamentService.exception.BusinessValidationException;
import com.example.backend.AssetManagamentService.exception.DuplicateResourceException;
import com.example.backend.AssetManagamentService.exception.ResourceNotFoundException;
import com.example.backend.CapacityAndSchedulingService.dto.technicianskill.AssignSkillRequest;
import com.example.backend.CapacityAndSchedulingService.dto.technicianskill.TechnicianSkillResponse;
import com.example.backend.CapacityAndSchedulingService.entity.Skill;
import com.example.backend.CapacityAndSchedulingService.entity.Technician;
import com.example.backend.CapacityAndSchedulingService.entity.TechnicianSkill;
import com.example.backend.CapacityAndSchedulingService.mapper.CapacitySchedulingMapper;
import com.example.backend.CapacityAndSchedulingService.repository.SkillRepository;
import com.example.backend.CapacityAndSchedulingService.repository.TechnicianRepository;
import com.example.backend.CapacityAndSchedulingService.repository.TechnicianSkillRepository;
import com.example.backend.CapacityAndSchedulingService.service.TechnicianSkillService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TechnicianSkillServiceImpl
        implements TechnicianSkillService {

    private final TechnicianSkillRepository technicianSkillRepository;
    private final TechnicianRepository technicianRepository;
    private final SkillRepository skillRepository;
    private final CapacitySchedulingMapper mapper;

    public TechnicianSkillServiceImpl(
            TechnicianSkillRepository technicianSkillRepository,
            TechnicianRepository technicianRepository,
            SkillRepository skillRepository,
            CapacitySchedulingMapper mapper
    ) {
        this.technicianSkillRepository =
                technicianSkillRepository;
        this.technicianRepository =
                technicianRepository;
        this.skillRepository =
                skillRepository;
        this.mapper =
                mapper;
    }

    /**
     * Assigns an existing Skill certification to an existing Technician.
     *
     * Certifications for the same Technician and Skill must not
     * have overlapping validity periods.
     */
    @Override
    @Transactional
    public void assignSkillToTechnician(
            Long technicianId,
            AssignSkillRequest request
    ) {
        validateTechnicianId(technicianId);
        validateAssignSkillRequest(request);

        String normalizedSkillCode =
                normalizeSkillCode(request.getSkillCode());

        Technician technician =
                getTechnicianEntity(technicianId);

        if (!Boolean.TRUE.equals(technician.getActive())) {
            throw new BusinessValidationException(
                    "Cannot assign a skill to inactive technician "
                            + technicianId
            );
        }

        Skill skill =
                getSkillEntity(normalizedSkillCode);

        boolean exactCertificationExists =
                technicianSkillRepository
                        .existsByTechnician_IdAndSkill_SkillCodeAndValidFrom(
                                technicianId,
                                normalizedSkillCode,
                                request.getValidFrom()
                        );

        if (exactCertificationExists) {
            throw new DuplicateResourceException(
                    "Skill "
                            + normalizedSkillCode
                            + " is already assigned to technician "
                            + technicianId
                            + " with valid-from date "
                            + request.getValidFrom()
            );
        }

        List<TechnicianSkill> existingCertifications =
                technicianSkillRepository
                        .findByTechnician_IdAndSkill_SkillCode(
                                technicianId,
                                normalizedSkillCode
                        );

        validateNoOverlappingCertification(
                existingCertifications,
                request.getValidFrom(),
                request.getValidTo(),
                technicianId,
                normalizedSkillCode
        );

        TechnicianSkill technicianSkill =
                new TechnicianSkill();

        technicianSkill.setTechnician(technician);
        technicianSkill.setSkill(skill);
        technicianSkill.setValidFrom(
                request.getValidFrom()
        );
        technicianSkill.setValidTo(
                request.getValidTo()
        );

        technicianSkillRepository.save(
                technicianSkill
        );
    }

    /**
     * Removes all certification periods for the given
     * Technician and Skill combination.
     *
     * The current API route does not identify one specific
     * certification period, so every matching period is removed.
     */
    @Override
    @Transactional
    public void removeSkillFromTechnician(
            Long technicianId,
            String skillCode
    ) {
        validateTechnicianId(technicianId);
        validateSkillCode(skillCode);

        String normalizedSkillCode =
                normalizeSkillCode(skillCode);

        getTechnicianEntity(technicianId);
        getSkillEntity(normalizedSkillCode);

        List<TechnicianSkill> certifications =
                technicianSkillRepository
                        .findByTechnician_IdAndSkill_SkillCode(
                                technicianId,
                                normalizedSkillCode
                        );

        if (certifications.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Skill "
                            + normalizedSkillCode
                            + " is not assigned to technician "
                            + technicianId
            );
        }

        technicianSkillRepository.deleteAll(
                certifications
        );
    }

    /**
     * Returns all certification periods for a Technician,
     * including current, expired and future certifications.
     */
    @Override
    public List<TechnicianSkillResponse>
    getSkillsForTechnician(
            Long technicianId
    ) {
        validateTechnicianId(technicianId);

        getTechnicianEntity(technicianId);

        return technicianSkillRepository
                .findByTechnician_IdOrderByValidFromDesc(
                        technicianId
                )
                .stream()
                .map(mapper::toTechnicianSkillResponse)
                .toList();
    }

    /**
     * Rejects a requested certification period if it overlaps
     * any existing period for the same Technician and Skill.
     */
    private void validateNoOverlappingCertification(
            List<TechnicianSkill> existingCertifications,
            LocalDate requestedValidFrom,
            LocalDate requestedValidTo,
            Long technicianId,
            String skillCode
    ) {
        boolean overlappingCertificationExists =
                existingCertifications
                        .stream()
                        .anyMatch(existing ->
                                certificationPeriodsOverlap(
                                        existing.getValidFrom(),
                                        existing.getValidTo(),
                                        requestedValidFrom,
                                        requestedValidTo
                                )
                        );

        if (overlappingCertificationExists) {
            throw new BusinessValidationException(
                    "Certification period overlaps an existing "
                            + "certification for skill "
                            + skillCode
                            + " and technician "
                            + technicianId
            );
        }
    }

    /**
     * Checks overlap between two inclusive date ranges.
     *
     * A null valid-to date means that the certification
     * remains valid indefinitely.
     */
    private boolean certificationPeriodsOverlap(
            LocalDate existingValidFrom,
            LocalDate existingValidTo,
            LocalDate requestedValidFrom,
            LocalDate requestedValidTo
    ) {
        boolean existingEndsBeforeRequestedStarts =
                existingValidTo != null
                        && existingValidTo.isBefore(
                        requestedValidFrom
                );

        boolean requestedEndsBeforeExistingStarts =
                requestedValidTo != null
                        && requestedValidTo.isBefore(
                        existingValidFrom
                );

        return !existingEndsBeforeRequestedStarts
                && !requestedEndsBeforeExistingStarts;
    }

    private Technician getTechnicianEntity(
            Long technicianId
    ) {
        return technicianRepository
                .findById(technicianId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Technician not found with id "
                                        + technicianId
                        )
                );
    }

    private Skill getSkillEntity(
            String skillCode
    ) {
        return skillRepository
                .findById(skillCode)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Skill not found with code "
                                        + skillCode
                        )
                );
    }

    private void validateAssignSkillRequest(
            AssignSkillRequest request
    ) {
        if (request == null) {
            throw new BusinessValidationException(
                    "Assign-skill request is required"
            );
        }

        validateSkillCode(request.getSkillCode());

        if (request.getValidFrom() == null) {
            throw new BusinessValidationException(
                    "Valid-from date is required"
            );
        }

        if (request.getValidTo() != null
                && request.getValidTo().isBefore(
                request.getValidFrom()
        )) {

            throw new BusinessValidationException(
                    "Valid-to date cannot be before valid-from date"
            );
        }
    }

    private void validateTechnicianId(
            Long technicianId
    ) {
        if (technicianId == null
                || technicianId <= 0) {

            throw new BusinessValidationException(
                    "Technician ID must be a positive number"
            );
        }
    }

    private void validateSkillCode(
            String skillCode
    ) {
        if (skillCode == null
                || skillCode.isBlank()) {

            throw new BusinessValidationException(
                    "Skill code is required"
            );
        }

        if (skillCode.trim().length() > 50) {
            throw new BusinessValidationException(
                    "Skill code cannot exceed 50 characters"
            );
        }
    }

    private String normalizeSkillCode(
            String skillCode
    ) {
        return skillCode
                .trim()
                .toUpperCase();
    }
}