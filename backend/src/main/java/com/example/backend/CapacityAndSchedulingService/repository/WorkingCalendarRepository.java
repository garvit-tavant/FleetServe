package com.example.backend.CapacityAndSchedulingService.repository;

import com.example.backend.CapacityAndSchedulingService.entity.WorkingCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.Collection;
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

    // ans: method name doesn't match Spring Data's derived-query keywords, so it
    // couldn't be resolved automatically - added explicit @Query. Note: this assumes
    // open time is the same across all days of the week for the workshop (a single
    // scalar result); if open time varies by dayOfWeek, this needs a dayOfWeek
    // parameter too, and SlaCalculator would need to pass the actual date being measured.
    @Query("select wc.openTime from WorkingCalendar wc where wc.workshop.id = :workshopId order by wc.dayOfWeek asc")
    LocalTime findopentimebywokrshopID(@Param("workshopId") long workshop_id);

    // ans: same fix as above, for close time.
    @Query("select wc.closeTime from WorkingCalendar wc where wc.workshop.id = :workshopId order by wc.dayOfWeek asc")
    LocalTime findbyclosetimebyworkshopID(@Param("workshopId") long workshopID);

    List<WorkingCalendar> findByWorkshop_IdOrderByDayOfWeekAsc(Long workshopId);

    // Bulk fetch for multiple workshops in a single query (avoids N+1 when batching,
    // e.g. in SlaService.MeanTimeToRepair()). Same "same hours every day" assumption
    // as above applies - callers should just take the first row per workshopId.
    List<WorkingCalendar> findByWorkshop_IdInOrderByWorkshop_IdAscDayOfWeekAsc(Collection<Long> workshopIds);
}
