package com.example.backend.ExecutionService.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.AssetManagamentService.entity.Asset;
import com.example.backend.AssetManagamentService.entity.MaintenancePlan;
import com.example.backend.AssetManagamentService.exception.BusinessValidationException;
import com.example.backend.AssetManagamentService.exception.DuplicateResourceException;
import com.example.backend.AssetManagamentService.exception.ResourceNotFoundException;
import com.example.backend.AssetManagamentService.repository.AssetClassPlanRepository;
import com.example.backend.AssetManagamentService.repository.AssetRepository;
import com.example.backend.AssetManagamentService.repository.MaintenancePlanRepository;
import com.example.backend.AssetManagamentService.status.AssetStatus;
import com.example.backend.CapacityAndSchedulingService.entity.Skill;
import com.example.backend.CapacityAndSchedulingService.entity.Workshop;
import com.example.backend.CapacityAndSchedulingService.repository.SkillRepository;
import com.example.backend.CapacityAndSchedulingService.repository.WorkshopRepository;
import com.example.backend.ExecutionService.dto.booking.BookBreakdownSlotRequest;
import com.example.backend.ExecutionService.dto.booking.BookPreventiveSlotRequest;
import com.example.backend.ExecutionService.dto.booking.BookingResponse;
import com.example.backend.ExecutionService.repository.BookingRepository;
import com.example.backend.ExecutionService.scheduling.WorkshopScheduleLoader;
import com.example.backend.ExecutionService.service.SlotBookingService;
import com.example.backend.SLA.entity.Booking;
import com.example.backend.SLA.entity.BreakdownRequest;
import com.example.backend.SLA.repository.BreakdownRequestRepository;
import com.example.backend.SLA.status.BreakdownStatus;
import com.example.slotengine.FeasibleSlotEngine;
import com.example.slotengine.model.FeasibleSlot;
import com.example.slotengine.model.SearchHorizon;
import com.example.slotengine.model.SlotSearchRequest;

@Service
@Transactional(readOnly = true)
public class SlotBookingServiceImpl implements SlotBookingService {

    /** Breakdown states from which triage may still book a slot. */
    private static final Set<BreakdownStatus> BOOKABLE_BREAKDOWN_STATUSES =
            BreakdownStatus.bookableStatuses();

    /**
     * A booking may not span two working days and is capped at 24 hours by
     * ck_booking_max_duration, so a longer estimate could never be scheduled.
     */
    private static final long MAX_JOB_MINUTES = 1440;

    private final Clock clock;
    private final AssetRepository assetRepository;
    private final MaintenancePlanRepository maintenancePlanRepository;
    private final AssetClassPlanRepository assetClassPlanRepository;
    private final BreakdownRequestRepository breakdownRequestRepository;
    private final BookingRepository bookingRepository;
    private final WorkshopScheduleLoader scheduleLoader;
    private final WorkshopRepository workshopRepository;
    private final SkillRepository skillRepository;

    public SlotBookingServiceImpl(
            Clock clock,
            AssetRepository assetRepository,
            MaintenancePlanRepository maintenancePlanRepository,
            AssetClassPlanRepository assetClassPlanRepository,
            BreakdownRequestRepository breakdownRequestRepository,
            BookingRepository bookingRepository,
            WorkshopScheduleLoader scheduleLoader,
            WorkshopRepository workshopRepository,
            SkillRepository skillRepository) {
        this.clock = clock;
        this.assetRepository = assetRepository;
        this.maintenancePlanRepository = maintenancePlanRepository;
        this.assetClassPlanRepository = assetClassPlanRepository;
        this.breakdownRequestRepository = breakdownRequestRepository;
        this.bookingRepository = bookingRepository;
        this.scheduleLoader = scheduleLoader;
        this.workshopRepository = workshopRepository;
        this.skillRepository = skillRepository;
    }

    @Override
    @Transactional
    public BookingResponse bookPreventiveSlot(BookPreventiveSlotRequest request) {
        Asset asset = assetRepository.findById(request.assetId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Asset not found with id " + request.assetId()));

        if (asset.getStatus() == AssetStatus.RETIRED) {
            throw new BusinessValidationException(
                    "Asset " + asset.getVin() + " is retired and cannot be booked for service");
        }

        MaintenancePlan plan = maintenancePlanRepository.findById(request.maintenancePlanId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Maintenance plan not found with id " + request.maintenancePlanId()));

        // Plans attach to the class, not the individual asset, so a plan that is
        // not on this asset's class is not a plan this asset can be booked under.
        boolean planAppliesToAsset = assetClassPlanRepository
                .existsByAssetClass_IdAndMaintenancePlan_Id(
                        asset.getAssetClass().getId(), plan.getId());
        if (!planAppliesToAsset) {
            throw new BusinessValidationException(
                    "Maintenance plan " + plan.getCode()
                            + " does not apply to asset class "
                            + asset.getAssetClass().getCode());
        }

        bookingRepository.findLivePreventiveBooking(asset.getId(), plan.getId())
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "Asset " + asset.getVin() + " already has booking "
                                    + existing.getId() + " held for plan " + plan.getCode());
                });

        FeasibleSlotOutcome outcome = findEarliestSlot(
                request.workshopId(),
                request.searchFrom(),
                plan.getEstimatedDurationMinutes(),
                plan.getRequiredSkillCode(),
                plan.getRequiredCapabilityCode());

        long bookingId = bookingRepository.insertBooking(
                asset.getId(),
                request.workshopId(),
                outcome.slot().bayId(),
                outcome.slot().technicianId(),
                outcome.start(),
                outcome.end(),
                Booking.KIND_PREVENTIVE,
                plan.getId(),
                null,
                Booking.STATUS_HELD);

        return toResponse(bookingId);
    }

    @Override
    @Transactional
    public BookingResponse bookBreakdownSlot(
            long breakdownRequestId, BookBreakdownSlotRequest request) {

        BreakdownRequest breakdown = breakdownRequestRepository.findById(breakdownRequestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Breakdown request not found with id " + breakdownRequestId));

        // A breakdown that is already booked should say so, rather than being
        // reported as merely being in the wrong state, so this check comes first.
        bookingRepository.findLiveBookingForBreakdown(breakdownRequestId)
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "Breakdown " + breakdownRequestId
                                    + " is already booked as booking " + existing.getId());
                });

        if (!BOOKABLE_BREAKDOWN_STATUSES.contains(breakdown.getStatus())) {
            throw new BusinessValidationException(
                    "Breakdown " + breakdownRequestId + " is " + breakdown.getStatus()
                            + " and can no longer be booked; bookable states are "
                            + BOOKABLE_BREAKDOWN_STATUSES);
        }

        requireWorkshopServesDepot(request.workshopId(), breakdown.getDepotId());

        int durationMinutes = resolveEstimatedDuration(breakdown);

        FeasibleSlotOutcome outcome = findEarliestSlot(
                request.workshopId(),
                request.searchFrom(),
                durationMinutes,
                breakdown.getRequiredSkillCode(),
                breakdown.getRequiredCapabilityCode());

        long bookingId = bookingRepository.insertBooking(
                breakdown.getAssetId(),
                request.workshopId(),
                outcome.slot().bayId(),
                outcome.slot().technicianId(),
                outcome.start(),
                outcome.end(),
                Booking.KIND_CORRECTIVE,
                null,
                breakdownRequestId,
                Booking.STATUS_HELD);

        // The breakdown has now been scheduled, so it leaves triage.
        breakdown.setStatus(BreakdownStatus.BOOKED);
        breakdownRequestRepository.save(breakdown);

        return toResponse(bookingId);
    }

    /** A chosen slot, together with its absolute instants. */
    private record FeasibleSlotOutcome(
            FeasibleSlot slot, OffsetDateTime start, OffsetDateTime end) {
    }

    /**
     * A breakdown is serviced by a workshop in the depot that reported it.
     */
    private void requireWorkshopServesDepot(long workshopId, Long breakdownDepotId) {
        Workshop workshop = workshopRepository.findById(workshopId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Workshop not found with id " + workshopId));

        Long workshopDepotId = workshop.getDepot() == null ? null : workshop.getDepot().getId();
        if (breakdownDepotId == null || !breakdownDepotId.equals(workshopDepotId)) {
            throw new BusinessValidationException(
                    "Workshop " + workshop.getCode() + " belongs to depot " + workshopDepotId
                            + " and cannot service a breakdown reported at depot "
                            + breakdownDepotId);
        }
    }

    /**
     * The stored estimate, or the required skill's standard time when triage has
     * not pinned one yet.
     *
     * <p>A derived value is written back onto the breakdown so it is fixed from
     * that point: editing {@code skill.time} later must not retrospectively
     * change the duration of work already booked.
     */
    private int resolveEstimatedDuration(BreakdownRequest breakdown) {
        Integer stored = breakdown.getEstimatedDurationMinutes();
        if (stored != null && stored > 0) {
            return stored;
        }

        String skillCode = breakdown.getRequiredSkillCode();
        if (skillCode == null) {
            throw new BusinessValidationException(
                    "Breakdown " + breakdown.getId() + " has neither an estimated duration nor a "
                            + "required skill to derive one from; triage it before booking");
        }

        Skill skill = skillRepository.findById(skillCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Skill not found with code " + skillCode));

        Long standardMinutes = skill.getTime();
        if (standardMinutes == null || standardMinutes <= 0 || standardMinutes > MAX_JOB_MINUTES) {
            throw new BusinessValidationException(
                    "Skill " + skillCode + " has a standard time of " + standardMinutes
                            + " minutes, which cannot be scheduled as a single booking");
        }

        int derived = standardMinutes.intValue();
        breakdown.setEstimatedDurationMinutes(derived);
        breakdownRequestRepository.save(breakdown);
        return derived;
    }

    private FeasibleSlotOutcome findEarliestSlot(
            long workshopId,
            LocalDate requestedStart,
            Integer durationMinutes,
            String requiredSkillCode,
            String requiredCapabilityCode) {

        if (durationMinutes == null || durationMinutes <= 0) {
            throw new BusinessValidationException(
                    "A positive estimated duration is required to search for a slot");
        }

        // Resolve the workshop's zone first, so "today" means today where the work
        // will actually happen rather than wherever the server is running.
        ZoneId zone = scheduleLoader.load(workshopId, new SearchHorizon(LocalDate.now(clock), 0))
                .zone();
        LocalDate today = LocalDate.now(clock.withZone(zone));
        LocalDate start = requestedStart != null ? requestedStart : today;

        // A slot in the past cannot be worked, so searching from one would either
        // return nothing useful or, worse, hold a bay on a date already gone.
        if (start.isBefore(today)) {
            throw new BusinessValidationException(
                    "No slot was booked: the search cannot start in the past. "
                            + start + " is before " + today
                            + " at workshop " + workshopId);
        }

        SearchHorizon horizon = new SearchHorizon(start, SEARCH_HORIZON_DAYS);
        WorkshopScheduleLoader.WorkshopSchedule schedule = scheduleLoader.load(workshopId, horizon);

        SlotSearchRequest searchRequest = new SlotSearchRequest(
                durationMinutes,
                requiredSkillCode,
                requiredCapabilityCode,
                schedule.bays(),
                schedule.technicians(),
                schedule.calendar(),
                schedule.bookings(),
                horizon,
                1);

        List<FeasibleSlot> slots = FeasibleSlotEngine.findSlots(searchRequest);
        if (slots.isEmpty()) {
            throw new BusinessValidationException(
                    "No slot was booked: no feasible slot exists at workshop " + workshopId
                            + " for a " + durationMinutes + " minute job between "
                            + horizon.startDate() + " and "
                            + horizon.endDateExclusive().minusDays(1)
                            + " (" + SEARCH_HORIZON_DAYS + " days)");
        }

        FeasibleSlot slot = slots.get(0);
        return new FeasibleSlotOutcome(
                slot,
                slot.date().atTime(slot.start()).atZone(schedule.zone()).toOffsetDateTime(),
                slot.date().atTime(slot.end()).atZone(schedule.zone()).toOffsetDateTime());
    }

    private BookingResponse toResponse(long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found with id " + bookingId));

        return new BookingResponse(
                booking.getId(),
                booking.getAssetId(),
                booking.getWorkshop().getId(),
                booking.getBayId(),
                booking.getTechnicianId(),
                booking.getSlotStart(),
                booking.getSlotEnd(),
                booking.getKind(),
                booking.getStatus(),
                booking.getMaintenancePlanId(),
                booking.getBreakdownRequest() == null
                        ? null
                        : booking.getBreakdownRequest().getId());
    }
}
