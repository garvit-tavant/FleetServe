package com.example.backend.CapacityAndSchedulingService.repository;

import com.example.backend.CapacityAndSchedulingService.entity.TechnicianSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TechnicianSkillRepository
        extends JpaRepository<TechnicianSkill, Long> {

    List<TechnicianSkill> findByTechnician_Id(
            Long technicianId
    );

    List<TechnicianSkill> findByTechnician_IdOrderByValidFromDesc(
            Long technicianId
    );

    List<TechnicianSkill> findBySkill_SkillCode(
            String skillCode
    );

    List<TechnicianSkill>
    findByTechnician_IdAndSkill_SkillCode(
            Long technicianId,
            String skillCode
    );

    boolean
    existsByTechnician_IdAndSkill_SkillCodeAndValidFrom(
            Long technicianId,
            String skillCode,
            LocalDate validFrom
    );
}