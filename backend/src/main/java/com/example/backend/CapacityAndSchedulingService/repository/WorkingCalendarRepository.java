package com.example.backend.CapacityAndSchedulingService.repository;

import com.example.backend.CapacityAndSchedulingService.entity.WorkingCalendar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkingCalendarRepository
        extends JpaRepository<WorkingCalendar, Long> {

    List<WorkingCalendar>
    findByWorkshop_IdOrderByDayOfWeek(
            Long workshopId
    );

    Optional<WorkingCalendar>
    findByWorkshop_IdAndDayOfWeek(
            Long workshopId,
            Short dayOfWeek
    );

    boolean existsByWorkshop_IdAndDayOfWeek(
            Long workshopId,
            Short dayOfWeek
    );
}