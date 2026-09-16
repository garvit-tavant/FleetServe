package com.example.backend.ExecutionService.service.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.example.backend.AssetManagamentService.status.AssetStatus;
import com.example.backend.ExecutionService.workflow.BreakdownEvent;
import com.example.backend.ExecutionService.workflow.BreakdownRequestStateMachine;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Range;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.AssetManagamentService.entity.Asset;
import com.example.backend.AssetManagamentService.exception.BusinessValidationException;
import com.example.backend.AssetManagamentService.exception.DuplicateResourceException;
import com.example.backend.AssetManagamentService.exception.ResourceNotFoundException;
import com.example.backend.CapacityAndSchedulingService.entity.Bay;
import com.example.backend.CapacityAndSchedulingService.entity.BayCapability;
import com.example.backend.CapacityAndSchedulingService.entity.Capability;
import com.example.backend.CapacityAndSchedulingService.entity.Holiday;
import com.example.backend.CapacityAndSchedulingService.entity.Technician;
import com.example.backend.CapacityAndSchedulingService.entity.TechnicianSkill;
import com.example.backend.CapacityAndSchedulingService.entity.Workshop;
import com.example.backend.CapacityAndSchedulingService.repository.BayRepository;
import com.example.backend.CapacityAndSchedulingService.repository.HolidayRepository;
import com.example.backend.CapacityAndSchedulingService.repository.TechnicianRepository;
import com.example.backend.CapacityAndSchedulingService.repository.WorkingCalendarRepository;
import com.example.backend.CapacityAndSchedulingService.repository.WorkshopRepository;
import com.example.backend.ExecutionService.dto.booking.CorrectiveBookingResponse;
import com.example.backend.ExecutionService.dto.booking.CreateCorrectiveBookingRequest;
import com.example.backend.ExecutionService.entity.Booking;
import com.example.backend.ExecutionService.entity.WorkOrder;
import com.example.backend.ExecutionService.mapper.CorrectiveBookingMapper;
import com.example.backend.ExecutionService.repository.BookingRepository;
import com.example.backend.ExecutionService.repository.WorkOrderRepository;
import com.example.backend.ExecutionService.service.CorrectiveBookingService;
import com.example.backend.ExecutionService.status.BookingKind;
import com.example.backend.ExecutionService.status.BookingStatus;
import com.example.backend.ExecutionService.status.WorkOrderStatus;
import com.example.backend.SLA.dto.BreakdownStatus;
import com.example.backend.SLA.entity.BreakdownRequest;
import com.example.backend.SLA.entity.SlaCheckpoint;
import com.example.backend.SLA.repository.BreakdownRequestRepository;
import com.example.backend.SLA.repository.SlaCheckpointRepository;
import com.example.backend.common.exception.GlobalExceptionHandler.ConflictException;
import com.example.slotengine.FeasibleSlotEngine;
import com.example.slotengine.model.BayCandidate;
import com.example.slotengine.model.ExistingBooking;
import com.example.slotengine.model.FeasibleSlot;
import com.example.slotengine.model.SearchHorizon;
import com.example.slotengine.model.SkillCertification;
import com.example.slotengine.model.SlotSearchRequest;
import com.example.slotengine.model.TechnicianCandidate;
import com.example.slotengine.model.WorkingDayHours;

@Service
@Transactional(readOnly = true)
public class CorrectiveBookingServiceImpl
        implements CorrectiveBookingService {

    private static final int SEARCH_HORIZON_DAYS = 30;

    /*
     * The engine returns deterministic earliest-first results.
     * We only require the first result from each workshop and then
     * compare those results across workshops.
     */
    private static final int MAX_RESULTS_PER_WORKSHOP = 1;

    private final BreakdownRequestRepository breakdownRequestRepository;
    private final BookingRepository bookingRepository;
    private final WorkOrderRepository workOrderRepository;
    private final SlaCheckpointRepository slaCheckpointRepository;
    private final WorkshopRepository workshopRepository;
    private final BayRepository bayRepository;
    private final TechnicianRepository technicianRepository;
    private final WorkingCalendarRepository workingCalendarRepository;
    private final HolidayRepository holidayRepository;
    private final BreakdownRequestStateMachine breakdownRequestStateMachine;
    private final CorrectiveBookingMapper correctiveBookingMapper;
    private final Clock clock;

    public CorrectiveBookingServiceImpl(
            BreakdownRequestRepository breakdownRequestRepository,
            BookingRepository bookingRepository,
            WorkOrderRepository workOrderRepository,
            SlaCheckpointRepository slaCheckpointRepository,
            WorkshopRepository workshopRepository,
            BayRepository bayRepository,
            TechnicianRepository technicianRepository,
            WorkingCalendarRepository workingCalendarRepository,
            HolidayRepository holidayRepository,
            BreakdownRequestStateMachine breakdownRequestStateMachine,
            CorrectiveBookingMapper correctiveBookingMapper,
            Clock clock) {

        this.breakdownRequestRepository =
                breakdownRequestRepository;

        this.bookingRepository =
                bookingRepository;

        this.workOrderRepository =
                workOrderRepository;

        this.slaCheckpointRepository =
                slaCheckpointRepository;

        this.workshopRepository =
                workshopRepository;

        this.bayRepository =
                bayRepository;

        this.technicianRepository =
                technicianRepository;

        this.workingCalendarRepository =
                workingCalendarRepository;

        this.holidayRepository =
                holidayRepository;

        this.breakdownRequestStateMachine =
                breakdownRequestStateMachine;

        this.correctiveBookingMapper =
                correctiveBookingMapper;

        this.clock =
                clock;
    }

    @Override
    @Transactional
    public CorrectiveBookingResponse createCorrectiveBooking(
            Long breakdownRequestId,
            CreateCorrectiveBookingRequest request) {

        validateRequest(
                breakdownRequestId,
                request);

        /*
         * A pessimistic write lock serializes booking attempts for the
         * same breakdown request.
         *
         * The database unique index on breakdown_request_id remains the
         * final concurrency guarantee.
         */
        BreakdownRequest breakdownRequest =
                breakdownRequestRepository
                        .findByIdForCorrectiveBooking(
                                breakdownRequestId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Breakdown request not found with id: "
                                                + breakdownRequestId));

        validateBreakdownRequest(
                breakdownRequest);


        BreakdownStatus nextStatus =
                breakdownRequestStateMachine.nextState(
                        breakdownRequest.getStatus(),
                        BreakdownEvent.BOOK_BREAKDOWN);

        SlotSelection selectedSlot =
                findEarliestCorrectiveSlot(
                        breakdownRequest,
                        request);

        Bay selectedBay =
                bayRepository
                        .findById(
                                selectedSlot.slot().bayId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Selected service bay not found with id: "
                                                + selectedSlot
                                                .slot()
                                                .bayId()));

        Technician selectedTechnician =
                technicianRepository
                        .findById(
                                selectedSlot
                                        .slot()
                                        .technicianId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Selected technician not found with id: "
                                                + selectedSlot
                                                .slot()
                                                .technicianId()));

        validateSelectedResources(
                selectedSlot,
                selectedBay,
                selectedTechnician,
                request);

        Booking booking =
                buildCorrectiveBooking(
                        breakdownRequest,
                        selectedSlot,
                        selectedBay,
                        selectedTechnician);

        Booking savedBooking;

        try {
            /*
             * saveAndFlush immediately evaluates:
             *
             * 1. the bay-slot GiST exclusion constraint
             * 2. the technician-slot GiST exclusion constraint
             * 3. one-booking-per-breakdown uniqueness
             */
            savedBooking =
                    bookingRepository.saveAndFlush(
                            booking);

        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException(
                    "The corrective booking could not be created because "
                            + "the selected slot is no longer available, "
                            + "the breakdown is already booked, or another "
                            + "booking constraint was violated.");
        }

        WorkOrder workOrder =
                buildScheduledWorkOrder(
                        savedBooking);

        WorkOrder savedWorkOrder;

        try {
            savedWorkOrder =
                    workOrderRepository.saveAndFlush(
                            workOrder);

        } catch (DataIntegrityViolationException exception) {

            throw new ConflictException(
                    "A work order already exists for booking "
                            + savedBooking.getId());
        }

        savedBooking.setWorkOrder(
                savedWorkOrder);

        createSlaCheckpoint(
                savedBooking);


        breakdownRequest.setBooking(
                savedBooking);

        breakdownRequest.setStatus(
                nextStatus);

        breakdownRequestRepository.save(
                breakdownRequest);

        /*
         * Keeps the inverse side of the one-to-one relationship
         * consistent in the current Java object graph.
         */

        return correctiveBookingMapper.toResponse(
                savedBooking,
                savedWorkOrder);
    }

    private void validateRequest(
            Long breakdownRequestId,
            CreateCorrectiveBookingRequest request) {

        if (breakdownRequestId == null) {
            throw new IllegalArgumentException(
                    "Breakdown request ID is required");
        }

        if (breakdownRequestId <= 0L) {
            throw new IllegalArgumentException(
                    "Breakdown request ID must be greater than zero");
        }

        if (request == null) {
            throw new IllegalArgumentException(
                    "Corrective booking request is required");
        }

        if (request.getRequiredSkillCode() == null
                || request.getRequiredSkillCode().isBlank()) {

            throw new BusinessValidationException(
                    "Required technician skill code is required");
        }

        if (request.getRequiredCapabilityCode() == null
                || request.getRequiredCapabilityCode().isBlank()) {

            throw new BusinessValidationException(
                    "Required bay capability code is required");
        }

        Integer estimatedDurationMinutes =
                request.getEstimatedDurationMinutes();

        if (estimatedDurationMinutes == null
                || estimatedDurationMinutes <= 0) {

            throw new BusinessValidationException(
                    "Estimated duration must be greater than zero");
        }

        if (estimatedDurationMinutes
                % FeasibleSlotEngine
                .SLOT_GRANULARITY_MINUTES != 0) {

            throw new BusinessValidationException(
                    "Estimated duration must be a multiple of "
                            + FeasibleSlotEngine
                            .SLOT_GRANULARITY_MINUTES
                            + " minutes");
        }

        if (estimatedDurationMinutes
                > 24 * 60) {

            throw new BusinessValidationException(
                    "Estimated duration cannot exceed 24 hours");
        }
    }

    private void validateBreakdownRequest(
            BreakdownRequest breakdownRequest) {

        if (breakdownRequest.getStatus()
                != BreakdownStatus.REPORTED) {

            throw new BusinessValidationException(
                    "Only a REPORTED breakdown request can be booked. "
                            + "Current status is "
                            + breakdownRequest.getStatus());
        }

        if (breakdownRequest.getAsset() == null
                || breakdownRequest.getAsset().getId() == null) {

            throw new BusinessValidationException(
                    "Breakdown request does not contain an asset");
        }

        if (breakdownRequest.getDepot() == null
                || breakdownRequest.getDepot().getId() == null) {

            throw new BusinessValidationException(
                    "Breakdown request does not contain a depot");
        }

        if (!Boolean.TRUE.equals(
                breakdownRequest.getDepot().getActive())) {

            throw new BusinessValidationException(
                    "The depot associated with this breakdown request "
                            + "is inactive");
        }

        if (breakdownRequest.getAsset().getStatus()
                != AssetStatus.ACTIVE) {

            throw new BusinessValidationException(
                    "Only an ACTIVE asset can be booked "
                            + "for corrective maintenance");
        }

        if (breakdownRequest.getAsset().getHomeDepot() == null
                || breakdownRequest
                .getAsset()
                .getHomeDepot()
                .getId() == null) {

            throw new BusinessValidationException(
                    "The breakdown asset has no home depot");
        }

        if (!breakdownRequest
                .getDepot()
                .getId()
                .equals(
                        breakdownRequest
                                .getAsset()
                                .getHomeDepot()
                                .getId())) {

            throw new BusinessValidationException(
                    "Breakdown depot does not match the asset home depot");
        }

        if (breakdownRequest.getSlaPolicy() == null
                || breakdownRequest.getSlaPolicy().getId() == null) {

            throw new BusinessValidationException(
                    "Breakdown request does not have a pinned SLA policy");
        }

        if (breakdownRequest.getReportedAt() == null) {
            throw new BusinessValidationException(
                    "Breakdown request does not have a reported timestamp");
        }

        if (bookingRepository
                .existsByBreakdownRequest_Id(
                        breakdownRequest.getId())) {

            throw new DuplicateResourceException(
                    "A booking has already been created for breakdown request "
                            + breakdownRequest.getId());
        }

        if (breakdownRequest.getBooking() != null) {
            throw new DuplicateResourceException(
                    "Breakdown request "
                            + breakdownRequest.getId()
                            + " is already linked to booking "
                            + breakdownRequest.getBooking().getId());
        }
    }

    private SlotSelection findEarliestCorrectiveSlot(
            BreakdownRequest breakdownRequest,
            CreateCorrectiveBookingRequest request) {

        Long depotId =
                breakdownRequest
                        .getDepot()
                        .getId();

        List<Workshop> workshops =
                workshopRepository
                        .findByDepot_IdAndIsActiveTrueOrderByIdAsc(
                                depotId);

        if (workshops.isEmpty()) {
            throw new BusinessValidationException(
                    "No active workshop exists for depot "
                            + depotId);
        }

        SlotSelection earliestSelection =
                null;

        for (Workshop workshop : workshops) {

            SlotSelection workshopSelection =
                    findEarliestSlotAtWorkshop(
                            workshop,
                            request);

            if (workshopSelection == null) {
                continue;
            }

            if (earliestSelection == null
                    || compareSlotSelections(
                    workshopSelection,
                    earliestSelection) < 0) {

                earliestSelection =
                        workshopSelection;
            }
        }

        if (earliestSelection == null) {
            throw new BusinessValidationException(
                    "No feasible corrective-maintenance slot was found "
                            + "within the next "
                            + SEARCH_HORIZON_DAYS
                            + " days for skill "
                            + request.getRequiredSkillCode().trim()
                            + " and capability "
                            + request.getRequiredCapabilityCode().trim());
        }

        return earliestSelection;
    }

    private SlotSelection findEarliestSlotAtWorkshop(
            Workshop workshop,
            CreateCorrectiveBookingRequest request) {

        ZoneId workshopZone =
                getWorkshopZone(
                        workshop);

        /*
         * The current slot engine searches complete working days.
         * Starting tomorrow prevents creation of a slot earlier than
         * the current instant.
         */
        LocalDate searchStartDate =
                LocalDate.now(
                                clock.withZone(workshopZone))
                        .plusDays(1);

        LocalDate searchEndDateExclusive =
                searchStartDate.plusDays(
                        SEARCH_HORIZON_DAYS);

        List<BayCandidate> bayCandidates =
                buildBayCandidates(
                        workshop.getId());

        if (bayCandidates.isEmpty()) {
            return null;
        }

        List<TechnicianCandidate> technicianCandidates =
                buildTechnicianCandidates(
                        workshop.getId());

        if (technicianCandidates.isEmpty()) {
            return null;
        }

        com.example.slotengine.model.WorkingCalendar
                slotEngineCalendar =
                buildSlotEngineCalendar(
                        workshop.getId(),
                        searchStartDate,
                        searchEndDateExclusive);

        if (slotEngineCalendar
                .weeklyHours()
                .isEmpty()) {

            return null;
        }

        OffsetDateTime horizonStart =
                searchStartDate
                        .atStartOfDay(workshopZone)
                        .toOffsetDateTime();

        OffsetDateTime horizonEnd =
                searchEndDateExclusive
                        .atStartOfDay(workshopZone)
                        .toOffsetDateTime();

        List<ExistingBooking> existingBookings =
                buildExistingBookings(
                        workshop.getId(),
                        workshopZone,
                        horizonStart,
                        horizonEnd);

        SlotSearchRequest slotSearchRequest =
                new SlotSearchRequest(
                        request.getEstimatedDurationMinutes(),
                        request.getRequiredSkillCode().trim(),
                        request.getRequiredCapabilityCode().trim(),
                        bayCandidates,
                        technicianCandidates,
                        slotEngineCalendar,
                        existingBookings,
                        new SearchHorizon(
                                searchStartDate,
                                SEARCH_HORIZON_DAYS),
                        MAX_RESULTS_PER_WORKSHOP);

        List<FeasibleSlot> feasibleSlots =
                FeasibleSlotEngine.findSlots(
                        slotSearchRequest);

        if (feasibleSlots.isEmpty()) {
            return null;
        }

        FeasibleSlot firstSlot =
                feasibleSlots.get(0);

        OffsetDateTime start =
                firstSlot
                        .date()
                        .atTime(firstSlot.start())
                        .atZone(workshopZone)
                        .toOffsetDateTime();

        OffsetDateTime end =
                firstSlot
                        .date()
                        .atTime(firstSlot.end())
                        .atZone(workshopZone)
                        .toOffsetDateTime();

        if (!end.isAfter(start)) {
            throw new BusinessValidationException(
                    "Slot engine returned an invalid slot interval");
        }

        return new SlotSelection(
                workshop,
                firstSlot,
                start,
                end);
    }

    private int compareSlotSelections(
            SlotSelection first,
            SlotSelection second) {

        int startComparison =
                first.start().compareTo(
                        second.start());

        if (startComparison != 0) {
            return startComparison;
        }

        int workshopComparison =
                Long.compare(
                        first.workshop().getId(),
                        second.workshop().getId());

        if (workshopComparison != 0) {
            return workshopComparison;
        }

        int bayComparison =
                Long.compare(
                        first.slot().bayId(),
                        second.slot().bayId());

        if (bayComparison != 0) {
            return bayComparison;
        }

        return Long.compare(
                first.slot().technicianId(),
                second.slot().technicianId());
    }

    private ZoneId getWorkshopZone(
            Workshop workshop) {

        if (workshop.getTimeZone() == null
                || workshop.getTimeZone().isBlank()) {

            throw new BusinessValidationException(
                    "Time zone is not configured for workshop "
                            + workshop.getId());
        }

        try {
            return ZoneId.of(
                    workshop.getTimeZone());

        } catch (RuntimeException exception) {

            throw new BusinessValidationException(
                    "Invalid time zone configured for workshop "
                            + workshop.getId()
                            + ": "
                            + workshop.getTimeZone());
        }
    }

    private List<BayCandidate> buildBayCandidates(
            Long workshopId) {

        return bayRepository
                .findByWorkshop_IdOrderByIdAsc(
                        workshopId)
                .stream()
                .map(bay -> {

                    Set<String> capabilityCodes =
                            bay.getCapabilities()
                                    .stream()
                                    .map(
                                            BayCapability::getCapability)
                                    .map(
                                            Capability::getCapabilityCode)
                                    .collect(
                                            Collectors.toSet());

                    return new BayCandidate(
                            bay.getId(),
                            Boolean.TRUE.equals(
                                    bay.getIsActive()),
                            capabilityCodes);
                })
                .toList();
    }

    private List<TechnicianCandidate>
    buildTechnicianCandidates(
            Long workshopId) {

        return technicianRepository
                .findByWorkshop_IdOrderByIdAsc(
                        workshopId)
                .stream()
                .map(technician -> {

                    List<SkillCertification> certifications =
                            technician
                                    .getTechnicianSkills()
                                    .stream()
                                    .map(
                                            this::toSkillCertification)
                                    .toList();

                    return new TechnicianCandidate(
                            technician.getId(),
                            Boolean.TRUE.equals(
                                    technician.getActive()),
                            certifications);
                })
                .toList();
    }

    private SkillCertification toSkillCertification(
            TechnicianSkill technicianSkill) {

        if (technicianSkill.getSkill() == null
                || technicianSkill
                .getSkill()
                .getSkillCode() == null) {

            throw new BusinessValidationException(
                    "A technician skill contains no skill code");
        }

        return new SkillCertification(
                technicianSkill
                        .getSkill()
                        .getSkillCode(),
                technicianSkill.getValidFrom(),
                technicianSkill.getValidTo());
    }

    private com.example.slotengine.model.WorkingCalendar
    buildSlotEngineCalendar(
            Long workshopId,
            LocalDate startDate,
            LocalDate endDateExclusive) {

        Map<DayOfWeek, WorkingDayHours> weeklyHours =
                workingCalendarRepository
                        .findByWorkshop_IdOrderByDayOfWeekAsc(
                                workshopId)
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        calendar ->
                                                DayOfWeek.of(
                                                        calendar
                                                                .getDayOfWeek()
                                                                .intValue()),
                                        calendar ->
                                                new WorkingDayHours(
                                                        calendar.getOpenTime(),
                                                        calendar.getCloseTime()),
                                        (first, second) -> {
                                            throw new BusinessValidationException(
                                                    "Duplicate working-calendar "
                                                            + "entry for workshop "
                                                            + workshopId);
                                        },
                                        () ->
                                                new EnumMap<>(
                                                        DayOfWeek.class)));

        Set<LocalDate> holidayDates =
                holidayRepository
                        .findApplicableHolidays(
                                workshopId,
                                startDate,
                                endDateExclusive)
                        .stream()
                        .map(
                                Holiday::getHolidayDate)
                        .collect(
                                Collectors.toSet());

        return new com.example.slotengine.model.WorkingCalendar(
                weeklyHours,
                holidayDates);
    }

    private List<ExistingBooking> buildExistingBookings(
            Long workshopId,
            ZoneId workshopZone,
            OffsetDateTime horizonStart,
            OffsetDateTime horizonEnd) {

        return bookingRepository
                .findBlockingBookings(
                        workshopId,
                        horizonStart,
                        horizonEnd)
                .stream()
                .map(booking ->
                        toExistingBooking(
                                booking,
                                workshopZone))
                .toList();
    }

    private ExistingBooking toExistingBooking(
            Booking booking,
            ZoneId workshopZone) {

        OffsetDateTime start =
                booking.getStartAt();
        OffsetDateTime end =
                booking.getEndAt();

        if (start == null || end == null) {
            throw new BusinessValidationException(
                    "Booking "
                            + booking.getId()
                            + " has an incomplete slot");
        }

        if (!end.isAfter(start)) {
            throw new BusinessValidationException(
                    "Booking "
                            + booking.getId()
                            + " has an invalid slot interval");
        }

        if (booking.getBay() == null
                || booking.getTechnician() == null) {

            throw new BusinessValidationException(
                    "Booking "
                            + booking.getId()
                            + " does not contain assigned resources");
        }

        ZonedDateTime localStart =
                start.atZoneSameInstant(
                        workshopZone);

        ZonedDateTime localEnd =
                end.atZoneSameInstant(
                        workshopZone);

        if (!localStart
                .toLocalDate()
                .equals(
                        localEnd.toLocalDate())) {

            throw new BusinessValidationException(
                    "Existing booking "
                            + booking.getId()
                            + " spans multiple workshop-local dates");
        }

        return new ExistingBooking(
                localStart.toLocalDate(),
                localStart
                        .toLocalTime()
                        .withSecond(0)
                        .withNano(0),
                localEnd
                        .toLocalTime()
                        .withSecond(0)
                        .withNano(0),
                booking.getBay().getId(),
                booking.getTechnician().getId());
    }

    private void validateSelectedResources(
            SlotSelection selectedSlot,
            Bay selectedBay,
            Technician selectedTechnician,
            CreateCorrectiveBookingRequest request) {

        Workshop workshop =
                selectedSlot.workshop();

        if (selectedBay.getWorkshop() == null
                || !workshop.getId().equals(
                selectedBay.getWorkshop().getId())) {

            throw new BusinessValidationException(
                    "Selected bay does not belong to the selected workshop");
        }

        if (!Boolean.TRUE.equals(
                selectedBay.getIsActive())) {

            throw new BusinessValidationException(
                    "Selected bay is inactive");
        }

        if (!Boolean.TRUE.equals(
                workshop.getActive())) {

            throw new BusinessValidationException(
                    "Selected workshop is inactive");
        }

        boolean bayHasRequiredCapability =
                selectedBay.getCapabilities()
                        .stream()
                        .map(BayCapability::getCapability)
                        .filter(capability ->
                                capability != null)
                        .map(Capability::getCapabilityCode)
                        .anyMatch(code ->
                                code != null
                                        && code.equals(
                                        request
                                                .getRequiredCapabilityCode()
                                                .trim()));

        if (!bayHasRequiredCapability) {
            throw new BusinessValidationException(
                    "Selected bay does not have required capability "
                            + request
                            .getRequiredCapabilityCode()
                            .trim());
        }

        if (selectedTechnician.getWorkshop() == null
                || !workshop.getId().equals(
                selectedTechnician
                        .getWorkshop()
                        .getId())) {

            throw new BusinessValidationException(
                    "Selected technician does not belong "
                            + "to the selected workshop");
        }

        if (!Boolean.TRUE.equals(
                selectedTechnician.getActive())) {

            throw new BusinessValidationException(
                    "Selected technician is inactive");
        }

        LocalDate slotDate =
                selectedSlot.slot().date();

        boolean technicianHasRequiredSkill =
                selectedTechnician
                        .getTechnicianSkills()
                        .stream()
                        .anyMatch(skill ->
                                isCertificationValid(
                                        skill,
                                        request
                                                .getRequiredSkillCode()
                                                .trim(),
                                        slotDate));

        if (!technicianHasRequiredSkill) {
            throw new BusinessValidationException(
                    "Selected technician does not have a valid "
                            + "certification for required skill "
                            + request
                            .getRequiredSkillCode()
                            .trim()
                            + " on "
                            + slotDate);
        }
    }

    /*
     * This separate helper is intentionally not used for slot derivation.
     * The selected slot date is already validated by the slot engine.
     *
     * The complete selected-slot validation below is performed in
     * buildCorrectiveBooking, using SlotSelection.start().
     */
    private java.time.Instant selectedSlotInstant(
            CreateCorrectiveBookingRequest request,
            Workshop workshop) {

        return clock.instant();
    }

    private boolean isCertificationValid(
            TechnicianSkill technicianSkill,
            String requiredSkillCode,
            LocalDate slotDate) {

        if (technicianSkill.getSkill() == null
                || technicianSkill
                .getSkill()
                .getSkillCode() == null) {

            return false;
        }

        if (!requiredSkillCode.equals(
                technicianSkill
                        .getSkill()
                        .getSkillCode())) {

            return false;
        }

        if (technicianSkill.getValidFrom() != null
                && slotDate.isBefore(
                technicianSkill.getValidFrom())) {

            return false;
        }

        return technicianSkill.getValidTo() == null
                || !slotDate.isAfter(
                technicianSkill.getValidTo());
    }

    private Booking buildCorrectiveBooking(
            BreakdownRequest breakdownRequest,
            SlotSelection selectedSlot,
            Bay selectedBay,
            Technician selectedTechnician) {

        Asset asset =
                breakdownRequest.getAsset();

        Booking booking =
                new Booking();

        booking.setAsset(
                asset);

        booking.setWorkshop(
                selectedSlot.workshop());

        booking.setBay(
                selectedBay);

        booking.setTechnician(
                selectedTechnician);

        booking.setStartAt(selectedSlot.start());
        booking.setEndAt(selectedSlot.end());

        booking.setKind(
                BookingKind.CORRECTIVE);

        booking.setMaintenancePlan(
                null);

        booking.setBreakdownRequest(
                breakdownRequest);

        booking.setStatus(
                BookingStatus.CONFIRMED);

        return booking;
    }

    private WorkOrder buildScheduledWorkOrder(
            Booking booking) {

        WorkOrder workOrder =
                new WorkOrder();

        workOrder.setWorkOrderNumber(
                generateWorkOrderNumber());

        workOrder.setBooking(
                booking);

        workOrder.setStatus(
                WorkOrderStatus.SCHEDULED);

        workOrder.setStartedAt(null);
        workOrder.setCompletedAt(null);
        workOrder.setOdometerAtService(null);
        workOrder.setTotalCost(BigDecimal.ZERO);
        workOrder.setIdempotencyKey(null);

        return workOrder;
    }

    private void createSlaCheckpoint(
            Booking booking) {

        if (booking.getKind()
                != BookingKind.CORRECTIVE) {

            throw new BusinessValidationException(
                    "An SLA checkpoint can only be created "
                            + "for a corrective booking");
        }

        if (booking.getBreakdownRequest() == null) {
            throw new BusinessValidationException(
                    "Corrective booking does not contain "
                            + "a breakdown request");
        }

        if (booking.getBreakdownRequest()
                .getSlaPolicy() == null) {

            throw new BusinessValidationException(
                    "Corrective booking breakdown does not "
                            + "have a pinned SLA policy");
        }

        if (slaCheckpointRepository
                .existsById(
                        booking.getId())) {

            throw new DuplicateResourceException(
                    "An SLA checkpoint already exists for booking "
                            + booking.getId());
        }

        SlaCheckpoint checkpoint =
                new SlaCheckpoint();

        /*
         * @MapsId derives booking_id from this association.
         */
        checkpoint.setBooking(
                booking);

        checkpoint.setRespondedAt(null);
        checkpoint.setResolvedAt(null);
        checkpoint.setResponseBreach(false);
        checkpoint.setResolutionBreach(false);
        checkpoint.setAccumulatedAwaitingMinutes(0L);
        checkpoint.setLastAwaitingRaisedAt(null);

        slaCheckpointRepository.saveAndFlush(
                checkpoint);
    }

    private String generateWorkOrderNumber() {

        return "WO-"
                + UUID.randomUUID()
                .toString()
                .toUpperCase();
    }

    private record SlotSelection(
            Workshop workshop,
            FeasibleSlot slot,
            OffsetDateTime start,
            OffsetDateTime end) {
    }
}