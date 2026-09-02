package com.example.backend.CapacityAndSchedulingService.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "skill")
public class Skill {

    @Id
    @Column(
            name = "skill_code",
            nullable = false,
            length = 50
    )
    private String skillCode;

    @Column(
            nullable = false,
            length = 100
    )
    private String name;

    @Column(
            nullable = false,
            length = 255
    )
    private String description;

    @Column(nullable = false)
    private Long time;

    @Version
    private Long version;

    @OneToMany(
            mappedBy = "skill"
    )
    private List<TechnicianSkill> technicianSkills =
            new ArrayList<>();

    public Skill() {
    }

    public String getSkillCode() {
        return skillCode;
    }

    public void setSkillCode(String skillCode) {
        this.skillCode = skillCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(
            String description
    ) {
        this.description = description;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public Long getVersion() {
        return version;
    }

    public List<TechnicianSkill> getTechnicianSkills() {
        return technicianSkills;
    }

    public void setTechnicianSkills(
            List<TechnicianSkill> technicianSkills
    ) {
        this.technicianSkills = technicianSkills;
    }
}