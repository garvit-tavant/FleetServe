package com.example.backend.CapacityAndSchedulingService.entity;

import jakarta.persistence.*;

import java.time.LocalTime;

@Entity
@Table(name = "working_calendar")
public class WorkingCalendar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "workshop_id",
            nullable = false
    )
    private Workshop workshop;

    /*
     * ISO-8601 day-of-week numbering:
     *
     * 1 = Monday
     * 2 = Tuesday
     * 3 = Wednesday
     * 4 = Thursday
     * 5 = Friday
     * 6 = Saturday
     * 7 = Sunday
     *
     * The database CHECK constraint enforces the range 1 to 7.
     */
    @Column(
            name = "day_of_week",
            nullable = false
    )
    private Short dayOfWeek;

    @Column(
            name = "open_time",
            nullable = false
    )
    private LocalTime openTime;

    @Column(
            name = "close_time",
            nullable = false
    )
    private LocalTime closeTime;

    @Version
    @Column(nullable = false)
    private Long version;

    public WorkingCalendar() {
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

    public Short getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(
            Short dayOfWeek
    ) {
        this.dayOfWeek = dayOfWeek;
    }

    public LocalTime getOpenTime() {
        return openTime;
    }

    public void setOpenTime(
            LocalTime openTime
    ) {
        this.openTime = openTime;
    }

    public LocalTime getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(
            LocalTime closeTime
    ) {
        this.closeTime = closeTime;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}