package com.example.backend.CapacityAndSchedulingService.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "technician_skill")
public class TechnicianSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "technician_id",
            nullable = false
    )
    private Technician technician;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "skill_code",
            nullable = false
    )
    private Skill skill;

    @Column(
            name = "valid_from",
            nullable = false
    )
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    public TechnicianSkill() {
    }

    public Long getId() {
        return id;
    }

    public Technician getTechnician() {
        return technician;
    }

    public void setTechnician(
            Technician technician
    ) {
        this.technician = technician;
    }

    public Skill getSkill() {
        return skill;
    }

    public void setSkill(
            Skill skill
    ) {
        this.skill = skill;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(
            LocalDate validFrom
    ) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public void setValidTo(
            LocalDate validTo
    ) {
        this.validTo = validTo;
    }
}