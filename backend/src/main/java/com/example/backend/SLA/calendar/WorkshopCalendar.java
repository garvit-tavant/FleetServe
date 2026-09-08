package com.example.backend.SLA.calendar;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The working calendar of a single workshop, as a value object.
 *
 * <p>Carries its own {@link ZoneId} because the specification stores every
 * timestamp as UTC while each workshop keeps its own time zone "for rendering
 * and for calendar arithmetic". A shift time such as 08:00 is meaningless
 * without it.
 */
public record WorkshopCalendar(
        ZoneId zone,
        Map<DayOfWeek, ShiftWindow> shifts,
        Set<LocalDate> holidays) {

    public WorkshopCalendar {
        Objects.requireNonNull(zone, "zone");
        shifts = Map.copyOf(Objects.requireNonNull(shifts, "shifts"));
        holidays = Set.copyOf(Objects.requireNonNull(holidays, "holidays"));
    }

    /**
     * The open period on the given local date, or empty when the workshop is
     * closed because the date is a holiday or the weekday carries no shift.
     */
    public Optional<ShiftWindow> shiftOn(LocalDate date) {
        if (holidays.contains(date)) {
            return Optional.empty();
        }
        return Optional.ofNullable(shifts.get(date.getDayOfWeek()));
    }
}
