package com.example.backend.CapacityAndSchedulingService.repository;

import com.example.backend.CapacityAndSchedulingService.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface HolidayRepository
        extends JpaRepository<Holiday, Long> {

    List<Holiday>
    findByWorkshop_IdOrderByHolidayDate(
            Long workshopId
    );

    List<Holiday>
    findByWorkshopIsNullOrderByHolidayDate();

    boolean existsByWorkshop_IdAndHolidayDate(
            Long workshopId,
            LocalDate holidayDate
    );

    boolean existsByWorkshopIsNullAndHolidayDate(
            LocalDate holidayDate
    );
}