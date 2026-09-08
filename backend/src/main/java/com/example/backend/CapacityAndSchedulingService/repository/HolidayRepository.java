package com.example.backend.CapacityAndSchedulingService.repository;

import com.example.backend.CapacityAndSchedulingService.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // Returns workshop-specific and global (workshop IS NULL) holiday dates in
    // the window. The join must be a LEFT JOIN: an implicit path join would be
    // an inner join and would silently drop every global holiday.
    @Query("""
            select h.holidayDate
            from Holiday h
            left join h.workshop w
            where (w.id = :workshopId or w is null)
              and h.holidayDate between :start and :end
            """)
    List<LocalDate> findHolidayDatesBetween(
            @Param("workshopId") long workshopId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

}