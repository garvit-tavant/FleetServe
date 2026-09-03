package com.example.backend.CapacityAndSchedulingService.controller;

import com.example.backend.CapacityAndSchedulingService.dto.workingcalendar.CreateWorkingCalendarRequest;
import com.example.backend.CapacityAndSchedulingService.dto.workingcalendar.UpdateWorkingCalendarRequest;
import com.example.backend.CapacityAndSchedulingService.dto.workingcalendar.WorkingCalendarResponse;
import com.example.backend.CapacityAndSchedulingService.service.WorkingCalendarService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workshops/{workshopId}/working-calendars")
public class WorkingCalendarController {

    private final WorkingCalendarService workingCalendarService;

    public WorkingCalendarController(
            WorkingCalendarService workingCalendarService
    ) {
        this.workingCalendarService =
                workingCalendarService;
    }

    @PostMapping
    public ResponseEntity<WorkingCalendarResponse>
    createWorkingCalendar(
            @PathVariable Long workshopId,

            @Valid
            @RequestBody
            CreateWorkingCalendarRequest request
    ) {

        WorkingCalendarResponse response =
                workingCalendarService
                        .createWorkingCalendar(
                                workshopId,
                                request
                        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<
            List<WorkingCalendarResponse>>
    getWorkingCalendarsForWorkshop(
            @PathVariable Long workshopId
    ) {

        return ResponseEntity.ok(
                workingCalendarService
                        .getWorkingCalendarsForWorkshop(
                                workshopId
                        )
        );
    }

    @GetMapping("/{calendarId}")
    public ResponseEntity<WorkingCalendarResponse>
    getWorkingCalendar(
            @PathVariable Long workshopId,

            @PathVariable Long calendarId
    ) {

        return ResponseEntity.ok(
                workingCalendarService
                        .getWorkingCalendar(
                                workshopId,
                                calendarId
                        )
        );
    }

    @PutMapping("/{calendarId}")
    public ResponseEntity<WorkingCalendarResponse>
    updateWorkingCalendar(
            @PathVariable Long workshopId,

            @PathVariable Long calendarId,

            @Valid
            @RequestBody
            UpdateWorkingCalendarRequest request
    ) {

        return ResponseEntity.ok(
                workingCalendarService
                        .updateWorkingCalendar(
                                workshopId,
                                calendarId,
                                request
                        )
        );
    }

    @DeleteMapping("/{calendarId}")
    public ResponseEntity<Void>
    deleteWorkingCalendar(
            @PathVariable Long workshopId,

            @PathVariable Long calendarId
    ) {

        workingCalendarService
                .deleteWorkingCalendar(
                        workshopId,
                        calendarId
                );

        return ResponseEntity
                .noContent()
                .build();
    }
}