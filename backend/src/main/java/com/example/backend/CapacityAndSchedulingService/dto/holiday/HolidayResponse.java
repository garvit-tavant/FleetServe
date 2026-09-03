package com.example.backend.CapacityAndSchedulingService.dto.holiday;

import java.time.LocalDate;

public class HolidayResponse {

    private Long id;

    private Long workshopId;

    private String workshopCode;

    private LocalDate holidayDate;

    private String description;

    private Boolean globalHoliday;

    // getters setters


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWorkshopId() {
        return workshopId;
    }

    public void setWorkshopId(Long workshopId) {
        this.workshopId = workshopId;
    }

    public String getWorkshopCode() {
        return workshopCode;
    }

    public void setWorkshopCode(String workshopCode) {
        this.workshopCode = workshopCode;
    }

    public LocalDate getHolidayDate() {
        return holidayDate;
    }

    public void setHolidayDate(LocalDate holidayDate) {
        this.holidayDate = holidayDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getGlobalHoliday() {
        return globalHoliday;
    }

    public void setGlobalHoliday(Boolean globalHoliday) {
        this.globalHoliday = globalHoliday;
    }
}