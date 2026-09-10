package com.example.backend.CapacityAndSchedulingService.repository;

import com.example.backend.CapacityAndSchedulingService.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

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

    // ans: fixed - now filters to holidays that are either specific to this workshop
    // OR global (workshop IS NULL), matching the pattern used by the sibling
    // findByWorkshop_Id.../findByWorkshopIsNull... methods above. Previously this
    // counted holidays across every workshop, which would overcount once more than
    // one workshop exists.
    @Query("select count(h) from Holiday h where (h.workshop.id = :workshopId or h.workshop is null) and h.holidayDate > :start and h.holidayDate < :end")
    long countHoldidayinbetween(@Param("workshopId") long workshopId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("""
        SELECT h
        FROM Holiday h
        WHERE h.holidayDate >= :startDate
        AND h.holidayDate < :endDateExclusive
        AND (
        h.workshop IS NULL
        OR h.workshop.id = :workshopId
        )
        ORDER BY h.holidayDate
    """)
    List<Holiday> findApplicableHolidays(
@Param("workshopId") Long workshopId,
@Param("startDate") LocalDate startDate,
@Param("endDateExclusive") LocalDate endDateExclusive
    );

    /**
     * Bulk variant of findApplicableHolidays for batch callers that need
     * holidays for MANY workshops across one shared date range at once (e.g.
     * SlaService.MeanTimeToRepair aggregating over many completed work
     * orders) — one query instead of one per workshop, avoiding N+1.
     * Global holidays (workshop IS NULL) are always included regardless of
     * which workshopIds are passed.
     */
    @Query("""
        SELECT h
        FROM Holiday h
        WHERE h.holidayDate >= :startDate
        AND h.holidayDate < :endDateExclusive
        AND (
        h.workshop IS NULL
        OR h.workshop.id IN :workshopIds
        )
        ORDER BY h.holidayDate
    """)
    List<Holiday> findApplicableHolidaysForWorkshops(
            @Param("workshopIds") Set<Long> workshopIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDateExclusive") LocalDate endDateExclusive
    );
}
