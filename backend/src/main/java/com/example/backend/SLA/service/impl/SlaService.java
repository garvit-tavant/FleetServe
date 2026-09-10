package com.example.backend.SLA.service.impl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import com.example.backend.CapacityAndSchedulingService.entity.Holiday;
import com.example.backend.CapacityAndSchedulingService.repository.HolidayRepository;
import com.example.backend.CapacityAndSchedulingService.repository.WorkingCalendarRepository;
import com.example.backend.SLA.dto.BookingAwaitingMinutesRow;
import com.example.backend.SLA.dto.MeanTimeToRepairReport;
import com.example.backend.SLA.dto.SlaBasis;
import com.example.backend.SLA.dto.SlaComplianceReport;
import com.example.backend.SLA.service.SlaCalculatorResolver;
import com.example.backend.SLA.service.SlaTimeCalculationStrategy;
import com.example.backend.ExecutionService.dto.CompletedWork;
import com.example.backend.ExecutionService.repository.WorkOrderRepository;
import com.example.backend.SLA.repository.SlaCheckpointRepository;

/**
 * SLA service facade providing high-level SLA queries for controllers and other services.
 *
 * Per project convention (Controller -> Service interface -> ServiceImpl), this should ideally
 * be split into an SlaService interface + SlaServiceImpl in a service/impl package,
 * matching AssetManagementService. Left as a single class for now since it's still minimal.
 *
 * @Service registers this as a Spring component; @Transactional(readOnly = true) applies
 * per project convention. Write methods should override with @Transactional.
 */
@Service
@Transactional(readOnly = true)
public class SlaService {
    private final SlaCalculator slaCalculator;
    private final WorkOrderRepository workOrderRepository;
    private final SlaCheckpointRepository slaCheckpointRepository;
    private final SlaCalculatorResolver slaCalculatorResolver;
    private final WorkingCalendarRepository workingCalendarRepository;
    private final HolidayRepository holidayRepository;

    public SlaService(SlaCalculator slaCalculator, WorkOrderRepository workOrderRepository,
                       SlaCheckpointRepository slaCheckpointRepository,
                       SlaCalculatorResolver slaCalculatorResolver,
                       WorkingCalendarRepository workingCalendarRepository,
                       HolidayRepository holidayRepository) {
        this.slaCalculator = slaCalculator;
        this.workOrderRepository = workOrderRepository;
        this.slaCheckpointRepository = slaCheckpointRepository;
        this.slaCalculatorResolver = slaCalculatorResolver;
        this.workingCalendarRepository = workingCalendarRepository;
        this.holidayRepository = holidayRepository;
    }

    /**
     * US-4.1 "soon to breach" view: priority plus both clocks' remaining time
     * and OK/AT_RISK/BREACHED status, in one call.
     *
     * it updates all the sla_checkpoints where breakdown_request has not been RESOLVED OR CANCELED
     */
    public void updateSlaStatus() {
        // Only evaluate SLA clocks for bookings that currently have an
        // active WorkOrder (SCHEDULED / IN_PROGRESS / AWAITING_PARTS). This
        // avoids wasting work on resolved/cancelled bookings and matches the
        // user's requirement.
    java.util.List<Long> activeBookingIds = workOrderRepository.findActiveBookingIds();
        for (Long bookingId : activeBookingIds) {
            if (bookingId == null) continue;
            slaCalculator.isResponseBreach(bookingId);
            slaCalculator.isResolutionBreach(bookingId);
        }

    }

    public List<SlaComplianceReport> SlaCompliance() {
        updateSlaStatus();
        return slaCheckpointRepository.findSlaComplianceMetrics();
    }

    // time is calculated from the work order timestamps 
    // we have awaiting time all calculated 
    // we dont have time to exlude due to weekends ( we can use sql for finding that out )
    // or we can use java for these calculation 
    // ans: Decision recorded — staying Java-side, reusing the same
    // SlaTimeCalculationStrategy (WORKING_TIME) that SlaCalculator uses for live
    // breach detection, so this MTTR figure and SLA compliance % can't silently
    // diverge from each other. Uses two bulk queries (work orders + awaiting
    // minutes) instead of one-per-row, avoiding an N+1 query pattern; the
    // per-row calculateElapsedTime(...) call is still a Java-side loop, which
    // does not satisfy US-4.2's "CTE/window-function SQL only" acceptance
    // criterion — flagged here as an accepted scope trade-off.
    public List<MeanTimeToRepairReport> MeanTimeToRepair() {

        List<CompletedWork> cWork = workOrderRepository.findCompletedWork();
        if (cWork.isEmpty()) {
            return List.of();
        }

        // cWork => start time , complete time , booking id
        List<Long> bookingIds = cWork.stream()
                .map(CompletedWork::getBookingId)
                .distinct()
                .collect(Collectors.toList());

        // map it asset class awaiting time;
        Map<Long, Long> awaitingByBookingId = slaCheckpointRepository
                .findAccumulatedAwaitingMinutesForBookingIds(bookingIds).stream()
                .collect(Collectors.toMap(
                        BookingAwaitingMinutesRow::getBookingId,
                        row -> row.getMinutes() != null ? row.getMinutes() : 0L));

        SlaTimeCalculationStrategy strategy = slaCalculatorResolver.resolve(SlaBasis.WORKING_TIME);

        // Batch-fetch shift hours (open/close time) for every distinct workshop in one
        // query, instead of hitting WorkingCalendarRepository once per row (N+1).
        Set<Long> workshopIds = cWork.stream()
                .map(CompletedWork::getWorkshopId)
                .collect(Collectors.toSet());

        Map<Long, LocalTime> shiftStartByWorkshop = new HashMap<>();
        Map<Long, LocalTime> shiftEndByWorkshop = new HashMap<>();
        for (var wc : workingCalendarRepository
                .findByWorkshop_IdInOrderByWorkshop_IdAscDayOfWeekAsc(workshopIds)) {
            Long wsId = wc.getWorkshop().getId();
            // Assumption (per WorkingCalendarRepository): open/close time is the same
            // every day of week for a workshop, so keep only the first row seen per id.
            shiftStartByWorkshop.putIfAbsent(wsId, wc.getOpenTime());
            shiftEndByWorkshop.putIfAbsent(wsId, wc.getCloseTime());
        }

        // Batch-fetch ALL applicable holidays (workshop-specific + global) across the
        // full date range spanned by cWork in a single query, instead of one query per row.
        LocalDate minDate = cWork.stream()
                .map(w -> w.getStartedAt().toLocalDate())
                .min(LocalDate::compareTo)
                .orElseThrow();
        LocalDate maxDateExclusive = cWork.stream()
                .map(w -> w.getCompletedAt().toLocalDate())
                .max(LocalDate::compareTo)
                .orElseThrow()
                .plusDays(1);

        Map<Long, Set<LocalDate>> holidaysByWorkshop = new HashMap<>();
        Set<LocalDate> globalHolidays = new HashSet<>();
        for (Holiday holiday : holidayRepository
                .findApplicableHolidaysForWorkshops(workshopIds, minDate, maxDateExclusive)) {
            if (holiday.getWorkshop() == null) {
                globalHolidays.add(holiday.getHolidayDate());
            } else {
                holidaysByWorkshop
                        .computeIfAbsent(holiday.getWorkshop().getId(), id -> new HashSet<>())
                        .add(holiday.getHolidayDate());
            }
        }
        // Global (workshop-agnostic) holidays apply to every workshop.
        for (Long workshopId : workshopIds) {
            holidaysByWorkshop
                    .computeIfAbsent(workshopId, id -> new HashSet<>())
                    .addAll(globalHolidays);
        }

        // cwork => elapsed time calculte and then remove the awaiting time and calculate avg with mapping to assetclass and return
        Map<Long, List<Long>> repairMinutesByAssetClassId = new HashMap<>();
        Map<Long, String> assetClassCodeById = new HashMap<>();

        for (CompletedWork work : cWork) {
            LocalDate dateStart = work.getStartedAt().toLocalDate();
            LocalDate dateEnd = work.getCompletedAt().toLocalDate();

            // Convert the bulk-fetched holiday dates into a count for just this
            // row's start/end range — the strategy only needs a count, never the
            // dates themselves (start/end dates are assumed never to be holidays).
            long holidayCount = holidaysByWorkshop
                    .getOrDefault(work.getWorkshopId(), Set.of())
                    .stream()
                    .filter(d -> d.isAfter(dateStart) && d.isBefore(dateEnd))
                    .count();

            long elapsed = strategy.calculateElapsedTime(
                    work.getStartedAt(),
                    work.getCompletedAt(),
                    work.getWorkshopId(),
                    shiftStartByWorkshop.get(work.getWorkshopId()),
                    shiftEndByWorkshop.get(work.getWorkshopId()),
                    holidayCount);

            long awaiting = awaitingByBookingId.getOrDefault(work.getBookingId(), 0L);
            long repairMinutes = Math.max(0L, elapsed - awaiting);

            repairMinutesByAssetClassId
                    .computeIfAbsent(work.getAssetClassId(), id -> new ArrayList<>())
                    .add(repairMinutes);
            assetClassCodeById.putIfAbsent(work.getAssetClassId(), work.getAssetClassCode());
        }

        return repairMinutesByAssetClassId.entrySet().stream()
                .map(entry -> {
                    Long assetClassId = entry.getKey();
                    List<Long> minutesList = entry.getValue();
                    double mean = minutesList.stream().mapToLong(Long::longValue).average().orElse(0.0);

                    return new MeanTimeToRepairReport(
                            assetClassId,
                            assetClassCodeById.get(assetClassId),
                            mean,
                            (long) minutesList.size());
                })
                .collect(Collectors.toList());
    }

}


