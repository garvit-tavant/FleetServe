package com.example.backend.AssetManagamentService.service.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.example.backend.CapacityAndSchedulingService.entity.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Range;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.AssetManagamentService.dto.duemaintenance.DueMaintenanceResponse;
import com.example.backend.AssetManagamentService.duemaintenance.DueMaintenanceCalculator;
import com.example.backend.AssetManagamentService.duemaintenance.DueStatus;
import com.example.backend.AssetManagamentService.entity.Asset;
import com.example.backend.AssetManagamentService.entity.AssetClassPlan;
import com.example.backend.AssetManagamentService.entity.MaintenancePlan;
import com.example.backend.AssetManagamentService.entity.OdometerReading;
import com.example.backend.AssetManagamentService.exception.BusinessValidationException;
import com.example.backend.AssetManagamentService.exception.DuplicateResourceException;
import com.example.backend.AssetManagamentService.exception.ResourceNotFoundException;
import com.example.backend.AssetManagamentService.repository.AssetClassPlanRepository;
import com.example.backend.AssetManagamentService.repository.AssetRepository;
import com.example.backend.AssetManagamentService.repository.MaintenancePlanRepository;
import com.example.backend.AssetManagamentService.repository.OdometerReadingRepository;
import com.example.backend.AssetManagamentService.service.DueMaintenanceService;
import com.example.backend.AssetManagamentService.status.AssetStatus;
import com.example.backend.CapacityAndSchedulingService.repository.BayRepository;
import com.example.backend.CapacityAndSchedulingService.repository.HolidayRepository;
import com.example.backend.CapacityAndSchedulingService.repository.TechnicianRepository;
import com.example.backend.CapacityAndSchedulingService.repository.WorkingCalendarRepository;
import com.example.backend.CapacityAndSchedulingService.repository.WorkshopRepository;
import com.example.backend.ExecutionService.dto.booking.PreventiveBookingResponse;
import com.example.backend.ExecutionService.entity.Booking;
import com.example.backend.ExecutionService.entity.WorkOrder;
import com.example.backend.ExecutionService.repository.BookingRepository;
import com.example.backend.ExecutionService.repository.WorkOrderRepository;
import com.example.backend.ExecutionService.status.BookingKind;
import com.example.backend.ExecutionService.status.BookingStatus;
import com.example.backend.ExecutionService.status.WorkOrderStatus;
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
public class DueMaintenanceServiceImpl
        implements DueMaintenanceService {

    /*
     * Temporary defaults. Move these to configurable data after the
     * functional booking flow is stable.
     */
    private static final BigDecimal DUE_SOON_KM =
            new BigDecimal("500");

    private static final int DUE_SOON_DAYS = 7;

    private static final int SEARCH_HORIZON_DAYS = 30;

    private static final int MAX_RESULTS_PER_WORKSHOP = 1;

    private final AssetRepository assetRepository;
    private final AssetClassPlanRepository assetClassPlanRepository;
    private final MaintenancePlanRepository maintenancePlanRepository;
    private final OdometerReadingRepository odometerReadingRepository;
    private final WorkshopRepository workshopRepository;
    private final BayRepository bayRepository;
    private final TechnicianRepository technicianRepository;
    private final WorkingCalendarRepository workingCalendarRepository;
    private final HolidayRepository holidayRepository;
    private final BookingRepository bookingRepository;
    private final WorkOrderRepository workOrderRepository;
    private final Clock clock;

    public DueMaintenanceServiceImpl(
            AssetRepository assetRepository,
            AssetClassPlanRepository assetClassPlanRepository,
            MaintenancePlanRepository maintenancePlanRepository,
            OdometerReadingRepository odometerReadingRepository,
            WorkshopRepository workshopRepository,
            BayRepository bayRepository,
            TechnicianRepository technicianRepository,
            WorkingCalendarRepository workingCalendarRepository,
            HolidayRepository holidayRepository,
            BookingRepository bookingRepository,
            WorkOrderRepository workOrderRepository,
            Clock clock) {

        this.assetRepository = assetRepository;
        this.assetClassPlanRepository = assetClassPlanRepository;
        this.maintenancePlanRepository = maintenancePlanRepository;
        this.odometerReadingRepository = odometerReadingRepository;
        this.workshopRepository = workshopRepository;
        this.bayRepository = bayRepository;
        this.technicianRepository = technicianRepository;
        this.workingCalendarRepository = workingCalendarRepository;
        this.holidayRepository = holidayRepository;
        this.bookingRepository = bookingRepository;
        this.workOrderRepository = workOrderRepository;
        this.clock = clock;
    }

    @Override
    public List<DueMaintenanceResponse> getDueMaintenanceAssets() {

        List<DueMaintenanceResponse> result =
                new ArrayList<>();

        List<Asset> activeAssets =
                assetRepository.findByStatus(
                        AssetStatus.ACTIVE);

        for (Asset asset : activeAssets) {

            if (asset.getAssetClass() == null
                    || asset.getAssetClass().getId() == null) {

                continue;
            }

            List<AssetClassPlan> mappings =
                    assetClassPlanRepository
                            .findByAssetClass_Id(
                                    asset.getAssetClass().getId());

            for (AssetClassPlan mapping : mappings) {

                MaintenancePlan maintenancePlan =
                        mapping.getMaintenancePlan();

                if (maintenancePlan == null) {
                    continue;
                }

                DueMaintenanceResponse response =
                        buildDueMaintenanceResponse(
                                asset,
                                maintenancePlan);

                if (!DueStatus.OK.name()
                        .equals(response.getDueStatus())) {

                    result.add(response);
                }
            }
        }

        return result;
    }

    private DueMaintenanceResponse buildDueMaintenanceResponse(
            Asset asset,
            MaintenancePlan maintenancePlan) {

        BigDecimal currentOdometerKm =
                getLatestOdometer(asset);

        ServiceBaseline baseline =
                getServiceBaseline(
                        asset,
                        maintenancePlan);

        BigDecimal nextDueKm =
                DueMaintenanceCalculator.nextDueKm(
                        baseline.odometerKm(),
                        maintenancePlan
                                .getDistanceIntervalKm());

        LocalDate nextDueDate =
                DueMaintenanceCalculator.nextDueDate(
                        baseline.serviceDate(),
                        maintenancePlan
                                .getTimeIntervalDays());

        DueStatus dueStatus =
                calculateDueStatus(
                        currentOdometerKm,
                        nextDueKm,
                        nextDueDate);

        DueMaintenanceResponse response =
                new DueMaintenanceResponse();

        response.setAssetId(asset.getId());
        response.setVin(asset.getVin());

        response.setMaintenancePlanId(
                maintenancePlan.getId());

        response.setMaintenancePlanCode(
                maintenancePlan.getCode());

        response.setCurrentOdometerKm(
                currentOdometerKm);

        response.setNextDueKm(nextDueKm);
        response.setNextDueDate(nextDueDate);

        response.setDueStatus(
                dueStatus.name());

        return response;
    }

    private ServiceBaseline getServiceBaseline(
            Asset asset,
            MaintenancePlan maintenancePlan) {

        List<WorkOrder> completedServices =
                workOrderRepository
                        .findCompletedPreventiveServices(
                                asset.getId(),
                                maintenancePlan.getId(),
                                PageRequest.of(0, 1));

        if (!completedServices.isEmpty()) {

            WorkOrder workOrder =
                    completedServices.get(0);

            return new ServiceBaseline(
                    workOrder.getOdometerAtService(),
                    workOrder.getCompletedAt()
                            .toLocalDate());
        }

        return new ServiceBaseline(
                asset.getAcquisitionOdometerKm(),
                asset.getAcquisitionDate());
    }

    private BigDecimal getLatestOdometer(
            Asset asset) {

        return odometerReadingRepository
                .findFirstByAsset_IdOrderByReadAtDesc(
                        asset.getId())
                .map(OdometerReading::getReadingKm)
                .orElse(
                        asset.getAcquisitionOdometerKm());
    }

    private DueStatus calculateDueStatus(
            BigDecimal currentOdometerKm,
            BigDecimal nextDueKm,
            LocalDate nextDueDate) {

        LocalDate today =
                LocalDate.now(clock);

        boolean overdueByDistance =
                nextDueKm != null
                        && currentOdometerKm != null
                        && currentOdometerKm
                        .compareTo(nextDueKm) >= 0;

        boolean overdueByDate =
                nextDueDate != null
                        && !today.isBefore(nextDueDate);

        if (overdueByDistance
                || overdueByDate) {

            return DueStatus.OVERDUE;
        }

        boolean dueSoonByDistance =
                nextDueKm != null
                        && currentOdometerKm != null
                        && nextDueKm
                        .subtract(currentOdometerKm)
                        .compareTo(DUE_SOON_KM) <= 0;

        boolean dueSoonByDate =
                nextDueDate != null
                        && !today
                        .plusDays(DUE_SOON_DAYS)
                        .isBefore(nextDueDate);

        if (dueSoonByDistance
                || dueSoonByDate) {

            return DueStatus.DUE_SOON;
        }

        return DueStatus.OK;
    }

    @Override
    @Transactional
    public PreventiveBookingResponse createPreventiveBooking(
            Long assetId,
            Long maintenancePlanId) {

        validateIdentifier(
                assetId,
                "Asset ID");

        validateIdentifier(
                maintenancePlanId,
                "Maintenance plan ID");

        Asset asset =
                assetRepository.findById(assetId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Asset not found with id: "
                                                + assetId));

        MaintenancePlan maintenancePlan =
                maintenancePlanRepository
                        .findById(maintenancePlanId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Maintenance plan not found with id: "
                                                + maintenancePlanId));

        validatePreventiveBookingRequest(
                asset,
                maintenancePlan);

        Long homeDepotId =
                asset.getHomeDepot().getId();

        List<Workshop> workshops =
                workshopRepository
                        .findByDepot_IdAndIsActiveTrueOrderByIdAsc(
                                homeDepotId);

        if (workshops.isEmpty()) {
            throw new BusinessValidationException(
                    "No active workshop exists for depot "
                            + homeDepotId);
        }

        SlotSelection selectedSlot =
                findEarliestSlot(
                        workshops,
                        maintenancePlan);

        Bay selectedBay =
                bayRepository
                        .findById(
                                selectedSlot.slot().bayId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Selected service bay not found: "
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
                                        "Selected technician not found: "
                                                + selectedSlot
                                                .slot()
                                                .technicianId()));

        validateSelectedResources(
                selectedSlot.workshop(),
                selectedBay,
                selectedTechnician);

        Booking booking =
                buildPreventiveBooking(
                        asset,
                        maintenancePlan,
                        selectedSlot,
                        selectedBay,
                        selectedTechnician);

        Booking savedBooking;

        try {
            /* trying to save without hibernate
             *
             * saveAndFlush forces PostgreSQL to immediately evaluate:
             *
             * 1. bay/slot GiST exclusion constraint
             * 2. technician/slot GiST exclusion constraint
             * 3. active preventive booking uniqueness
             */
             savedBooking = bookingRepository.saveAndFlush(booking);


        } catch (DataIntegrityViolationException exception) {

            throw new ConflictException(
                    "The selected bay or technician is no longer "
                            + "available, or an active preventive booking "
                            + "already exists for this asset and plan. "
                            + "Search for another slot.");
        }

        WorkOrder workOrder =
                buildScheduledWorkOrder(
                        savedBooking);

        WorkOrder savedWorkOrder;

        try {
            savedWorkOrder =
                    workOrderRepository
                            .saveAndFlush(
                                    workOrder);

        } catch (DataIntegrityViolationException exception) {

            throw new ConflictException(
                    "A work order already exists for booking "
                            + savedBooking.getId());
        }

        savedBooking.setWorkOrder(
                savedWorkOrder);

        return buildPreventiveBookingResponse(
                savedBooking,
                savedWorkOrder,
                selectedSlot);
    }

    private void validatePreventiveBookingRequest(
            Asset asset,
            MaintenancePlan maintenancePlan) {

        if (asset.getStatus()
                != AssetStatus.ACTIVE) {

            throw new BusinessValidationException(
                    "Only active assets can be booked "
                            + "for preventive maintenance");
        }

        if (asset.getHomeDepot() == null
                || asset.getHomeDepot().getId() == null) {

            throw new BusinessValidationException(
                    "Asset "
                            + asset.getId()
                            + " is not assigned to a home depot");
        }

        if (!Boolean.TRUE.equals(
                asset.getHomeDepot().getActive())) {

            throw new BusinessValidationException(
                    "The home depot for asset "
                            + asset.getId()
                            + " is inactive");
        }

        boolean planBelongsToAssetClass =
                assetClassPlanRepository
                        .existsByAssetClass_IdAndMaintenancePlan_Id(
                                asset.getAssetClass().getId(),
                                maintenancePlan.getId());

        if (!planBelongsToAssetClass) {
            throw new BusinessValidationException(
                    "Maintenance plan "
                            + maintenancePlan.getId()
                            + " is not mapped to asset class "
                            + asset.getAssetClass().getId());
        }

        Integer estimatedDurationMinutes =
                maintenancePlan
                        .getEstimatedDurationMinutes();

        if (estimatedDurationMinutes == null
                || estimatedDurationMinutes <= 0) {

            throw new BusinessValidationException(
                    "Maintenance-plan estimated duration "
                            + "must be greater than zero");
        }

        if (estimatedDurationMinutes
                % FeasibleSlotEngine
                .SLOT_GRANULARITY_MINUTES != 0) {

            throw new BusinessValidationException(
                    "Maintenance-plan estimated duration "
                            + "must be a multiple of "
                            + FeasibleSlotEngine
                            .SLOT_GRANULARITY_MINUTES
                            + " minutes");
        }

        if (maintenancePlan
                .getRequiredSkillCode() == null
                || maintenancePlan
                .getRequiredSkillCode()
                .isBlank()) {

            throw new BusinessValidationException(
                    "Maintenance plan must define "
                            + "a required technician skill");
        }

        if (maintenancePlan
                .getRequiredCapabilityCode() == null
                || maintenancePlan
                .getRequiredCapabilityCode()
                .isBlank()) {

            throw new BusinessValidationException(
                    "Maintenance plan must define "
                            + "a required bay capability");
        }

        DueMaintenanceResponse dueMaintenance =
                buildDueMaintenanceResponse(
                        asset,
                        maintenancePlan);

        if (DueStatus.OK.name()
                .equals(
                        dueMaintenance.getDueStatus())) {

            throw new BusinessValidationException(
                    "Asset "
                            + asset.getId()
                            + " is not due for maintenance plan "
                            + maintenancePlan.getId());
        }

        boolean activeBookingExists =
                bookingRepository
                        .existsByAsset_IdAndMaintenancePlan_IdAndStatusIn(
                                asset.getId(),
                                maintenancePlan.getId(),
                                List.of(
                                        BookingStatus.CONFIRMED));

        if (activeBookingExists) {
            throw new DuplicateResourceException(
                    "A confirmed preventive booking already "
                            + "exists for asset "
                            + asset.getId()
                            + " and maintenance plan "
                            + maintenancePlan.getId());
        }
    }

    private void validateSelectedResources(
            Workshop workshop,
            Bay bay,
            Technician technician) {

        if (bay.getWorkshop() == null
                || !workshop.getId().equals(
                bay.getWorkshop().getId())) {

            throw new BusinessValidationException(
                    "Selected service bay does not belong "
                            + "to the selected workshop");
        }

        if (!Boolean.TRUE.equals(
                bay.getIsActive())) {

            throw new BusinessValidationException(
                    "Selected service bay is inactive");
        }

        if (technician.getWorkshop() == null
                || !workshop.getId().equals(
                technician.getWorkshop().getId())) {

            throw new BusinessValidationException(
                    "Selected technician does not belong "
                            + "to the selected workshop");
        }

        if (!Boolean.TRUE.equals(
                technician.getActive())) {

            throw new BusinessValidationException(
                    "Selected technician is inactive");
        }
    }

    private SlotSelection findEarliestSlot(
            List<Workshop> workshops,
            MaintenancePlan maintenancePlan) {

        SlotSelection earliestSelection = null;

        for (Workshop workshop : workshops) {

            SlotSelection workshopSelection =
                    findEarliestSlotAtWorkshop(
                            workshop,
                            maintenancePlan);

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
                    "No feasible preventive-maintenance slot "
                            + "was found within the next "
                            + SEARCH_HORIZON_DAYS
                            + " days");
        }

        return earliestSelection;
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

    private SlotSelection findEarliestSlotAtWorkshop(
            Workshop workshop,
            MaintenancePlan maintenancePlan) {

        ZoneId workshopZone =
                getWorkshopZone(workshop);

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

        SlotSearchRequest request =
                new SlotSearchRequest(
                        maintenancePlan
                                .getEstimatedDurationMinutes(),
                        maintenancePlan
                                .getRequiredSkillCode(),
                        maintenancePlan
                                .getRequiredCapabilityCode(),
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
                        request);

        if (feasibleSlots.isEmpty()) {
            return null;
        }

        FeasibleSlot firstSlot =
                feasibleSlots.get(0);

        OffsetDateTime start =
                firstSlot.date()
                        .atTime(firstSlot.start())
                        .atZone(workshopZone)
                        .toOffsetDateTime();

        OffsetDateTime end =
                firstSlot.date()
                        .atTime(firstSlot.end())
                        .atZone(workshopZone)
                        .toOffsetDateTime();

        return new SlotSelection(
                workshop,
                firstSlot,
                start,
                end);
    }

    private ZoneId getWorkshopZone(
            Workshop workshop) {

        if (workshop.getTimeZone() == null
                || workshop.getTimeZone()
                .isBlank()) {

            throw new BusinessValidationException(
                    "Time zone is not configured "
                            + "for workshop "
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

        OffsetDateTime start = booking.getStartAt();
        OffsetDateTime end = booking.getEndAt();

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

        if (!localStart.toLocalDate()
                .equals(localEnd.toLocalDate())) {

            throw new BusinessValidationException(
                    "Existing booking "
                            + booking.getId()
                            + " spans multiple workshop-local dates");
        }

        return new ExistingBooking(
                localStart.toLocalDate(),
                localStart.toLocalTime()
                        .withSecond(0)
                        .withNano(0),
                localEnd.toLocalTime()
                        .withSecond(0)
                        .withNano(0),
                booking.getBay().getId(),
                booking.getTechnician().getId());
    }

    private Booking buildPreventiveBooking(
            Asset asset,
            MaintenancePlan maintenancePlan,
            SlotSelection selectedSlot,
            Bay selectedBay,
            Technician selectedTechnician) {

        Booking booking =
                new Booking();

        System.out.println("preventing oye");
        booking.setAsset(asset);

        booking.setWorkshop(
                selectedSlot.workshop());

        booking.setBay(
                selectedBay);

        booking.setTechnician(
                selectedTechnician);

        booking.setStartAt(selectedSlot.start());
        booking.setEndAt(selectedSlot.end());
        booking.setKind(
                BookingKind.PREVENTIVE);

        booking.setMaintenancePlan(
                maintenancePlan);

        booking.setBreakdownRequest(null);

        booking.setStatus(
                BookingStatus.CONFIRMED);

        System.out.println("===========\n"+booking.toString()+"==========\n");

        return booking;
    }

    private WorkOrder buildScheduledWorkOrder(
            Booking booking) {
        System.out.println("inside work order");
        WorkOrder workOrder =
                new WorkOrder();

        workOrder.setWorkOrderNumber(
                generateWorkOrderNumber());

        workOrder.setBooking(
                booking);

        workOrder.setStatus(
                WorkOrderStatus.SCHEDULED);

        workOrder.setStartedAt(booking.getStartAt());
        workOrder.setCompletedAt(null);
        workOrder.setOdometerAtService(null);
        workOrder.setTotalCost(BigDecimal.ZERO);
        workOrder.setIdempotencyKey(null);

        return workOrder;
    }

    private String generateWorkOrderNumber() {

        return "WO-"
                + UUID.randomUUID()
                .toString()
                .toUpperCase();
    }

    private PreventiveBookingResponse
    buildPreventiveBookingResponse(
            Booking booking,
            WorkOrder workOrder,
            SlotSelection selectedSlot) {

        PreventiveBookingResponse response =
                new PreventiveBookingResponse();

        response.setBookingId(
                booking.getId());

        response.setWorkOrderId(
                workOrder.getId());

        response.setWorkOrderNumber(
                workOrder.getWorkOrderNumber());

        response.setAssetId(
                booking.getAsset().getId());

        response.setMaintenancePlanId(
                booking.getMaintenancePlan().getId());

        response.setWorkshopId(
                booking.getWorkshop().getId());

        response.setBayId(
                booking.getBay().getId());

        response.setTechnicianId(
                booking.getTechnician().getId());

        response.setStart(
                selectedSlot.start());

        response.setEnd(
                selectedSlot.end());

        response.setBookingKind(
                booking.getKind().name());

        response.setBookingStatus(
                booking.getStatus().name());

        response.setWorkOrderStatus(
                workOrder.getStatus().name());

        return response;
    }

    private void validateIdentifier(
            Long value,
            String fieldName) {

        if (value == null) {
            throw new IllegalArgumentException(
                    fieldName + " is required");
        }

        if (value <= 0L) {
            throw new IllegalArgumentException(
                    fieldName
                            + " must be greater than zero");
        }
    }

    private record SlotSelection(
            Workshop workshop,
            FeasibleSlot slot,
            OffsetDateTime start,
            OffsetDateTime end) {
    }

    private record ServiceBaseline(
            BigDecimal odometerKm,
            LocalDate serviceDate) {
    }
}