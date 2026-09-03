package com.example.backend.CapacityAndSchedulingService.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "workshop")
public class Workshop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            nullable = false,
            length = 30
    )
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "depot_id",
            nullable = false
    )
    private Depot depot;

    @Column(
            name = "time_zone",
            nullable = false,
            length = 100
    )
    private String timeZone;

    @Column(
            name = "is_active",
            nullable = false
    )
    private Boolean isActive = true;

    @Version
    private Long version;

    @OneToMany(
            mappedBy = "workshop"
    )
    private List<Bay> bays =
            new ArrayList<>();

    @OneToMany(mappedBy = "workshop")
    private List<WorkingCalendar> workingCalendars =
            new ArrayList<>();

    @OneToMany(mappedBy = "workshop")
    private List<Holiday> holidays =
            new ArrayList<>();

    public Workshop() {
    }

    // getters setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public List<Bay> getBays() {
        return bays;
    }

    public void setBays(List<Bay> bays) {
        this.bays = bays;
    }

    public Depot getDepot() {
        return depot;
    }

    public void setDepot(Depot depot) {
        this.depot = depot;
    }

    public List<WorkingCalendar> getWorkingCalendars() {
        return workingCalendars;
    }

    public void setWorkingCalendars(List<WorkingCalendar> workingCalendars) {
        this.workingCalendars = workingCalendars;
    }

    public List<Holiday> getHolidays() {
        return holidays;
    }

    public void setHolidays(List<Holiday> holidays) {
        this.holidays = holidays;
    }
}
