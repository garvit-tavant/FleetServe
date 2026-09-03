package com.example.backend.CapacityAndSchedulingService.service;

import com.example.backend.CapacityAndSchedulingService.dto.workingcalendar.CreateWorkingCalendarRequest;
import com.example.backend.CapacityAndSchedulingService.dto.workingcalendar.UpdateWorkingCalendarRequest;
import com.example.backend.CapacityAndSchedulingService.dto.workingcalendar.WorkingCalendarResponse;

import java.util.List;

public interface WorkingCalendarService {

    WorkingCalendarResponse createWorkingCalendar(
            Long workshopId,
            CreateWorkingCalendarRequest request
    );

    WorkingCalendarResponse getWorkingCalendar(
            Long workshopId,
            Long calendarId
    );

    List<WorkingCalendarResponse>
    getWorkingCalendarsForWorkshop(
            Long workshopId
    );

    WorkingCalendarResponse updateWorkingCalendar(
            Long workshopId,
            Long calendarId,
            UpdateWorkingCalendarRequest request
    );

    void deleteWorkingCalendar(
            Long workshopId,
            Long calendarId
    );
}