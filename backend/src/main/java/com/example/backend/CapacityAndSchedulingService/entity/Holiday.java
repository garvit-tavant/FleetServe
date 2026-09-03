package com.example.backend.CapacityAndSchedulingService.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "holiday")
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Nullable because a holiday may be:
     *
     * 1. Workshop-specific, where workshop_id has a value.
     * 2. Global, where workshop_id is null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "workshop_id"
    )
    private Workshop workshop;

    @Column(
            name = "holiday_date",
            nullable = false
    )
    private LocalDate holidayDate;

    @Column(
            nullable = false,
            length = 255
    )
    private String description;

    @Version
    @Column(nullable = false)
    private Long version;

    public Holiday() {
    }

    public Long getId() {
        return id;
    }

    public void setId(
            Long id
    ) {
        this.id = id;
    }

    public Workshop getWorkshop() {
        return workshop;
    }

    public void setWorkshop(
            Workshop workshop
    ) {
        this.workshop = workshop;
    }

    public LocalDate getHolidayDate() {
        return holidayDate;
    }

    public void setHolidayDate(
            LocalDate holidayDate
    ) {
        this.holidayDate = holidayDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(
            String description
    ) {
        this.description = description;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}