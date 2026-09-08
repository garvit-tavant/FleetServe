package com.example.backend.SLA.calendar;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.AssetManagamentService.exception.ResourceNotFoundException;
import com.example.backend.CapacityAndSchedulingService.entity.WorkingCalendar;
import com.example.backend.CapacityAndSchedulingService.entity.Workshop;
import com.example.backend.CapacityAndSchedulingService.repository.HolidayRepository;
import com.example.backend.CapacityAndSchedulingService.repository.WorkingCalendarRepository;
import com.example.backend.CapacityAndSchedulingService.repository.WorkshopRepository;

/**
 * Loads a workshop's calendar out of the database and into the value object
 * consumed by {@link WorkingCalendarArithmetic}.
 *
 * <p>All persistence lives here so the arithmetic itself stays pure. The whole
 * week is fetched in one query rather than one query per day, and holidays are
 * fetched once for the evaluation window.
 */
@Component
@Transactional(readOnly = true)
public class WorkshopCalendarProvider {

    private final WorkshopRepository workshopRepository;
    private final WorkingCalendarRepository workingCalendarRepository;
    private final HolidayRepository holidayRepository;

    public WorkshopCalendarProvider(
            WorkshopRepository workshopRepository,
            WorkingCalendarRepository workingCalendarRepository,
            HolidayRepository holidayRepository) {
        this.workshopRepository = workshopRepository;
        this.workingCalendarRepository = workingCalendarRepository;
        this.holidayRepository = holidayRepository;
    }

    public WorkshopCalendar load(long workshopId, LocalDate from, LocalDate to) {
        Workshop workshop = workshopRepository.findById(workshopId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Workshop not found: " + workshopId));

        Map<DayOfWeek, ShiftWindow> shifts = new EnumMap<>(DayOfWeek.class);
        for (WorkingCalendar row : workingCalendarRepository
                .findByWorkshop_IdOrderByDayOfWeek(workshopId)) {

            if (row.getDayOfWeek() == null || row.getOpenTime() == null || row.getCloseTime() == null) {
                continue;
            }
            // working_calendar.day_of_week uses ISO numbering, 1 = Monday.
            shifts.put(DayOfWeek.of(row.getDayOfWeek()),
                    new ShiftWindow(row.getOpenTime(), row.getCloseTime()));
        }

        LocalDate windowStart = from.isBefore(to) ? from : to;
        LocalDate windowEnd = from.isBefore(to) ? to : from;
        Set<LocalDate> holidays = new HashSet<>(
                holidayRepository.findHolidayDatesBetween(workshopId, windowStart, windowEnd));

        return new WorkshopCalendar(zoneOf(workshop), shifts, holidays);
    }

    /**
     * A 24/7 calendar used when no workshop is known yet, so that elapsed-hours
     * measurement still has a time zone to report against.
     */
    public WorkshopCalendar continuousCalendar() {
        return new WorkshopCalendar(ZoneId.of("UTC"), Map.of(), Set.of());
    }

    private static ZoneId zoneOf(Workshop workshop) {
        try {
            return ZoneId.of(workshop.getTimeZone());
        } catch (DateTimeException | NullPointerException ex) {
            throw new ResourceNotFoundException(
                    "Workshop " + workshop.getId() + " has an unusable time zone: "
                            + workshop.getTimeZone());
        }
    }
}
