package com.example.backend.CapacityAndSchedulingService.controller;

import com.example.backend.CapacityAndSchedulingService.dto.holiday.CreateWorkshopHolidayRequest;
import com.example.backend.CapacityAndSchedulingService.dto.holiday.HolidayResponse;
import com.example.backend.CapacityAndSchedulingService.service.HolidayService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workshops/{workshopId}/holidays")
public class WorkshopHolidayController {

    private final HolidayService holidayService;

    public WorkshopHolidayController(
            HolidayService holidayService
    ) {
        this.holidayService = holidayService;
    }

    @PostMapping
    public ResponseEntity<HolidayResponse>
    createWorkshopHoliday(
            @PathVariable Long workshopId,

            @Valid
            @RequestBody
            CreateWorkshopHolidayRequest request
    ) {

        HolidayResponse response =
                holidayService.createWorkshopHoliday(
                        workshopId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<HolidayResponse>>
    getWorkshopHolidays(
            @PathVariable Long workshopId
    ) {

        return ResponseEntity.ok(
                holidayService.getWorkshopHolidays(
                        workshopId
                )
        );
    }

    @DeleteMapping("/{holidayId}")
    public ResponseEntity<Void>
    deleteWorkshopHoliday(
            @PathVariable Long workshopId,

            @PathVariable Long holidayId
    ) {

        holidayService.deleteWorkshopHoliday(
                workshopId,
                holidayId
        );

        return ResponseEntity
                .noContent()
                .build();
    }
}