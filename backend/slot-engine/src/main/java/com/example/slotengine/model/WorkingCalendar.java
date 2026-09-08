package com.example.slotengine.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A workshop's weekly opening pattern plus its closed dates.
 *
 * <p>A date is a working day when the weekday carries hours and the date is not
 * a holiday. Holidays are held here rather than passed separately so that
 * "is this date workable" has exactly one answer.
 */
public record WorkingCalendar(Map<DayOfWeek, WorkingDayHours> weeklyHours, Set<LocalDate> holidays) {

    public WorkingCalendar {
        weeklyHours = weeklyHours == null ? Map.of() : Map.copyOf(weeklyHours);
        holidays = holidays == null ? Set.of() : Set.copyOf(holidays);
    }

    public static WorkingCalendar of(Map<DayOfWeek, WorkingDayHours> weeklyHours) {
        return new WorkingCalendar(weeklyHours, Set.of());
    }

    /** The opening period on that date, or empty when the workshop is shut. */
    public Optional<WorkingDayHours> hoursOn(LocalDate date) {
        if (holidays.contains(date)) {
            return Optional.empty();
        }
        return Optional.ofNullable(weeklyHours.get(date.getDayOfWeek()));
    }

    public boolean isWorkingDay(LocalDate date) {
        return hoursOn(date).isPresent();
    }
}
