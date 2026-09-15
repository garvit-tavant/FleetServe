package com.example.backend.CapacityAndSchedulingService.entity;

import com.example.backend.SecurityService.entity.AppUser;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "technician")
public class Technician {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "app_user_id",
            nullable = false
    )
    private AppUser appUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "workshop_id",
            nullable = false
    )
    private Workshop workshop;

    @Column(
            name = "hourly_rate",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal hourlyRate;

    @Column(
            name = "is_active",
            nullable = false
    )
    private Boolean active = true;

    @Version
    private Long version;

    @OneToMany(
            mappedBy = "technician"
    )
    private List<TechnicianSkill> technicianSkills =
            new ArrayList<>();

    public Technician() {
    }

    public Long getId() {
        return id;
    }

    public AppUser getAppUser() {
        return appUser;
    }

    public void setAppUser(AppUser appUser) {
        this.appUser = appUser;
    }

    public Workshop getWorkshop() {
        return workshop;
    }

    public void setWorkshop(
            Workshop workshop
    ) {
        this.workshop = workshop;
    }

    public BigDecimal getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(
            BigDecimal hourlyRate
    ) {
        this.hourlyRate = hourlyRate;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(
            Boolean active
    ) {
        this.active = active;
    }

    public List<TechnicianSkill> getTechnicianSkills() {
        return technicianSkills;
    }

    public void setTechnicianSkills(
            List<TechnicianSkill> technicianSkills
    ) {
        this.technicianSkills = technicianSkills;
    }

    public Long getVersion() {
        return version;
    }
}