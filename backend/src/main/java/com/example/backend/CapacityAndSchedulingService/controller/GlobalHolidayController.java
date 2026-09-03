package com.example.backend.CapacityAndSchedulingService.controller;

import com.example.backend.CapacityAndSchedulingService.dto.holiday.CreateGlobalHolidayRequest;
import com.example.backend.CapacityAndSchedulingService.dto.holiday.HolidayResponse;
import com.example.backend.CapacityAndSchedulingService.service.HolidayService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/holidays")
public class GlobalHolidayController {

    private final HolidayService holidayService;

    public GlobalHolidayController(
            HolidayService holidayService
    ) {
        this.holidayService = holidayService;
    }

    @PostMapping
    public ResponseEntity<HolidayResponse>
    createGlobalHoliday(
            @Valid
            @RequestBody
            CreateGlobalHolidayRequest request
    ) {

        HolidayResponse response =
                holidayService.createGlobalHoliday(
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<HolidayResponse>>
    getGlobalHolidays() {

        return ResponseEntity.ok(
                holidayService.getGlobalHolidays()
        );
    }

    @DeleteMapping("/{holidayId}")
    public ResponseEntity<Void>
    deleteGlobalHoliday(
            @PathVariable Long holidayId
    ) {

        holidayService.deleteGlobalHoliday(
                holidayId
        );

        return ResponseEntity
                .noContent()
                .build();
    }
}