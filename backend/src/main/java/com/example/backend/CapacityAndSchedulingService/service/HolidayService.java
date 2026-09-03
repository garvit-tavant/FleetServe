package com.example.backend.CapacityAndSchedulingService.service;

import com.example.backend.CapacityAndSchedulingService.dto.holiday.CreateGlobalHolidayRequest;
import com.example.backend.CapacityAndSchedulingService.dto.holiday.CreateWorkshopHolidayRequest;
import com.example.backend.CapacityAndSchedulingService.dto.holiday.HolidayResponse;

import java.util.List;

public interface HolidayService {

    HolidayResponse createWorkshopHoliday(
            Long workshopId,
            CreateWorkshopHolidayRequest request
    );

    List<HolidayResponse> getWorkshopHolidays(
            Long workshopId
    );

    void deleteWorkshopHoliday(
            Long workshopId,
            Long holidayId
    );

    HolidayResponse createGlobalHoliday(
            CreateGlobalHolidayRequest request
    );

    List<HolidayResponse> getGlobalHolidays();

    void deleteGlobalHoliday(
            Long holidayId
    );
}