package com.example.backend.AssetManagamentService.service.impl;

import com.example.backend.AssetManagamentService.dto.duemaintenance.DueMaintenanceResponse;
import com.example.backend.AssetManagamentService.duemaintenance.DueMaintenanceCalculator;
import com.example.backend.AssetManagamentService.duemaintenance.DueStatus;
import com.example.backend.AssetManagamentService.entity.Asset;
import com.example.backend.AssetManagamentService.entity.AssetClassPlan;
import com.example.backend.AssetManagamentService.entity.MaintenancePlan;
import com.example.backend.AssetManagamentService.entity.OdometerReading;
import com.example.backend.AssetManagamentService.repository.AssetClassPlanRepository;
import com.example.backend.AssetManagamentService.repository.AssetRepository;
import com.example.backend.AssetManagamentService.repository.MaintenancePlanRepository;
import com.example.backend.AssetManagamentService.repository.OdometerReadingRepository;
import com.example.backend.AssetManagamentService.service.DueMaintenanceService;

import com.example.backend.AssetManagamentService.status.AssetStatus;
import com.example.backend.CapacityAndSchedulingService.entity.BayCapability;
import com.example.backend.CapacityAndSchedulingService.entity.Capability;
import com.example.backend.CapacityAndSchedulingService.entity.Holiday;
import com.example.backend.CapacityAndSchedulingService.entity.TechnicianSkill;
import com.example.backend.CapacityAndSchedulingService.entity.Workshop;
import com.example.backend.CapacityAndSchedulingService.repository.BayRepository;
import com.example.backend.CapacityAndSchedulingService.repository.HolidayRepository;
import com.example.backend.CapacityAndSchedulingService.repository.TechnicianRepository;
import com.example.backend.CapacityAndSchedulingService.repository.WorkingCalendarRepository;
import com.example.backend.CapacityAndSchedulingService.repository.WorkshopRepository;

import com.example.backend.ExecutionService.dto.PreventiveBookingResponse;
import com.example.backend.ExecutionService.entity.Booking;
import com.example.backend.ExecutionService.entity.WorkOrder;
import com.example.backend.ExecutionService.repository.BookingRepository;
import com.example.backend.ExecutionService.repository.WorkOrderRepository;
import com.example.backend.ExecutionService.status.BookingKind;
import com.example.backend.ExecutionService.status.BookingStatus;
import com.example.backend.ExecutionService.status.WorkOrderStatus;

import com.example.slotengine.FeasibleSlotEngine;
import com.example.slotengine.model.BayCandidate;
import com.example.slotengine.model.ExistingBooking;
import com.example.slotengine.model.FeasibleSlot;
import com.example.slotengine.model.SearchHorizon;
import com.example.slotengine.model.SkillCertification;
import com.example.slotengine.model.SlotSearchRequest;
import com.example.slotengine.model.TechnicianCandidate;
import com.example.slotengine.model.WorkingDayHours;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Range;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
@Transactional(readOnly = true)
public class DueMaintenanceServiceImpl
        implements DueMaintenanceService {

    /*
     * Temporary constants.
     *
     * FleetServe requires DUE_SOON thresholds to eventually come
     * from configurable data.
     */
    private static final BigDecimal DUE_SOON_KM =
            new BigDecimal("500");

    private static final int DUE_SOON_DAYS = 7;

    private static final int SEARCH_HORIZON_DAYS = 30;

    /*
     * The slot engine returns its results earliest-first.
     *
     * Therefore, only the first result from each workshop is needed.
     * The service then compares those workshop results.
     */
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
            Clock clock
    ) {
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

    /*
     * ==============================================================
     * Due-maintenance listing
     * ==============================================================
     */

    @Override
    public List<DueMaintenanceResponse> getDueMaintenanceAssets() {

        List<DueMaintenanceResponse> result =
                new ArrayList<>();

        List<Asset> assets =
                assetRepository.findAll();

        for (Asset asset : assets) {

            List<AssetClassPlan> assetClassPlans =
                    assetClassPlanRepository
                            .findByAssetClass_Id(
                                    asset.getAssetClass().getId()
                            );

            for (AssetClassPlan assetClassPlan : assetClassPlans) {

                MaintenancePlan maintenancePlan =
                        assetClassPlan.getMaintenancePlan();

                DueMaintenanceResponse response =
                        buildDueMaintenanceResponse(
                                asset,
                                maintenancePlan
                        );

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
            MaintenancePlan maintenancePlan
    ) {

        BigDecimal currentOdometerKm =
                getLatestOdometer(asset);

        /*
         * Current baseline:
         *
         * asset acquisition odometer
         * asset acquisition date
         *
         * This should later use the latest COMPLETED preventive
         * WorkOrder for the same asset and maintenance plan.
         */
        BigDecimal nextDueKm =
                DueMaintenanceCalculator.nextDueKm(
                        asset.getAcquisitionOdometerKm(),
                        maintenancePlan.getDistanceIntervalKm()
                );

        LocalDate nextDueDate =
                DueMaintenanceCalculator.nextDueDate(
                        asset.getAcquisitionDate(),
                        maintenancePlan.getTimeIntervalDays()
                );

        DueStatus dueStatus =
                calculateDueStatus(
                        currentOdometerKm,
                        nextDueKm,
                        nextDueDate
                );

        DueMaintenanceResponse response =
                new DueMaintenanceResponse();

        response.setAssetId(
                asset.getId()
        );

        response.setVin(
                asset.getVin()
        );

        response.setMaintenancePlanId(
                maintenancePlan.getId()
        );

        response.setMaintenancePlanCode(
                maintenancePlan.getCode()
        );

        response.setCurrentOdometerKm(
                currentOdometerKm
        );

        response.setNextDueKm(
                nextDueKm
        );

        response.setNextDueDate(
                nextDueDate
        );

        response.setDueStatus(
                dueStatus.name()
        );

        return response;
    }

    private BigDecimal getLatestOdometer(
            Asset asset
    ) {

        return odometerReadingRepository
                .findFirstByAsset_IdOrderByReadAtDesc(
                        asset.getId()
                )
                .map(OdometerReading::getReadingKm)
                .orElse(
                        asset.getAcquisitionOdometerKm()
                );
    }

    private DueStatus calculateDueStatus(
            BigDecimal currentOdometerKm,
            BigDecimal nextDueKm,
            LocalDate nextDueDate
    ) {

        LocalDate today =
                LocalDate.now(clock);

        boolean overdueByDistance =
                nextDueKm != null
                        && currentOdometerKm
                        .compareTo(nextDueKm) >= 0;

        boolean overdueByDate =
                nextDueDate != null
                        && !today.isBefore(nextDueDate);

        if (overdueByDistance || overdueByDate) {
            return DueStatus.OVERDUE;
        }

        boolean dueSoonByDistance =
                nextDueKm != null
                        && nextDueKm
                        .subtract(currentOdometerKm)
                        .compareTo(DUE_SOON_KM) <= 0;

        boolean dueSoonByDate =
                nextDueDate != null
                        && !today
                        .plusDays(DUE_SOON_DAYS)
                        .isBefore(nextDueDate);

        if (dueSoonByDistance || dueSoonByDate) {
            return DueStatus.DUE_SOON;
        }

        return DueStatus.OK;
    }

    /*
     * ==============================================================
     * Preventive booking creation
     * ==============================================================
     */

    @Override
    @Transactional
    public PreventiveBookingResponse createPreventiveBooking(
            Long assetId,
            Long maintenancePlanId
    ) {

        Asset asset =
                assetRepository
                        .findById(assetId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Asset not found with id: "
                                                + assetId
                                )
                        );

        MaintenancePlan maintenancePlan =
                maintenancePlanRepository
                        .findById(maintenancePlanId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Maintenance plan not found with id: "
                                                + maintenancePlanId
                                )
                        );

        validatePreventiveBookingRequest(
                asset,
                maintenancePlan
        );

        Long homeDepotId =
                asset.getHomeDepot().getId();

        List<Workshop> workshops =
                workshopRepository
                        .findByDepot_IdAndIsActiveTrueOrderByIdAsc(
                                homeDepotId
                        );

        if (workshops.isEmpty()) {
            throw new IllegalStateException(
                    "No active workshop exists for depot "
                            + homeDepotId
            );
        }

        SlotSelection selectedSlot =
                findEarliestSlot(
                        workshops,
                        maintenancePlan
                );

        Booking booking =
                buildPreventiveBooking(
                        asset,
                        maintenancePlan,
                        selectedSlot
                );

        Booking savedBooking;

        try {
            /*
             * The slot engine operates on a snapshot of existing
             * bookings.
             *
             * Another request may take the selected slot between
             * slot calculation and INSERT.
             *
             * saveAndFlush causes PostgreSQL to evaluate the
             * exclusion constraints immediately.
             */
            savedBooking =
                    bookingRepository.saveAndFlush(
                            booking
                    );

        } catch (DataIntegrityViolationException exception) {

            throw new IllegalStateException(
                    "The selected booking slot was taken by another "
                            + "request. Search for another slot.",
                    exception
            );
        }

        WorkOrder workOrder =
                buildScheduledWorkOrder(
                        asset,
                        savedBooking
                );

        WorkOrder savedWorkOrder =
                workOrderRepository.saveAndFlush(
                        workOrder
                );

        /*
         * WorkOrder.booking owns the relationship.
         *
         * Booking.workOrder is mappedBy and therefore is the
         * inverse side. This assignment keeps the Java object
         * graph consistent.
         */
        savedBooking.setWorkOrder(
                savedWorkOrder
        );

        return buildPreventiveBookingResponse(
                savedBooking,
                savedWorkOrder,
                selectedSlot
        );
    }

    /*
     * ==============================================================
     * Preventive booking validation
     * ==============================================================
     */

    private void validatePreventiveBookingRequest(
            Asset asset,
            MaintenancePlan maintenancePlan
    ) {

        boolean planBelongsToAssetClass =
                assetClassPlanRepository
                        .existsByAssetClass_IdAndMaintenancePlan_Id(
                                asset.getAssetClass().getId(),
                                maintenancePlan.getId()
                        );

        if (!planBelongsToAssetClass) {
            throw new IllegalArgumentException(
                    "Maintenance plan "
                            + maintenancePlan.getId()
                            + " is not mapped to asset class "
                            + asset.getAssetClass().getId()
            );
        }

        Integer estimatedDurationMinutes =
                maintenancePlan.getEstimatedDurationMinutes();

        if (estimatedDurationMinutes == null
                || estimatedDurationMinutes <= 0) {

            throw new IllegalArgumentException(
                    "Maintenance plan estimated duration "
                            + "must be positive"
            );
        }

        if (asset.getStatus()!= AssetStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Inactive assets cannot be booked"
            );
        }

        if (estimatedDurationMinutes
                % FeasibleSlotEngine.SLOT_GRANULARITY_MINUTES
                != 0) {

            throw new IllegalArgumentException(
                    "Maintenance plan estimated duration "
                            + "must be a multiple of "
                            + FeasibleSlotEngine
                            .SLOT_GRANULARITY_MINUTES
                            + " minutes"
            );
        }

        DueMaintenanceResponse dueMaintenance =
                buildDueMaintenanceResponse(
                        asset,
                        maintenancePlan
                );

        if (DueStatus.OK.name()
                .equals(dueMaintenance.getDueStatus())) {

            throw new IllegalStateException(
                    "Asset "
                            + asset.getId()
                            + " is not due for maintenance plan "
                            + maintenancePlan.getId()
            );
        }

        boolean exists =
                bookingRepository
                        .existsByAssetIdAndMaintenancePlanIdAndStatusIn(
                                asset.getId(),
                                maintenancePlan.getId(),
                                List.of(
                                        BookingStatus.HELD,
                                        BookingStatus.CONFIRMED
                                )
                        );

        if(exists){
            throw new IllegalArgumentException(
                    "Preventive booking for maintenance plan "
                            + maintenancePlan.getId()
                            + " and asset class "
                            + asset.getAssetClass().getId()
                            + " is already done"
            );
        }
    }

    /*
     * ==============================================================
     * Find the earliest slot across all active workshops
     * ==============================================================
     */

    private SlotSelection findEarliestSlot(
            List<Workshop> workshops,
            MaintenancePlan maintenancePlan
    ) {

        SlotSelection earliestSelection = null;

        for (Workshop workshop : workshops) {

            SlotSelection workshopSelection =
                    findEarliestSlotAtWorkshop(
                            workshop,
                            maintenancePlan
                    );

            if (workshopSelection == null) {
                continue;
            }

            if (earliestSelection == null
                    || compareSlotSelections(
                    workshopSelection,
                    earliestSelection
            ) < 0) {

                earliestSelection =
                        workshopSelection;
            }
        }

        if (earliestSelection == null) {
            throw new IllegalStateException(
                    "No feasible preventive-maintenance slot "
                            + "was found within the next "
                            + SEARCH_HORIZON_DAYS
                            + " days"
            );
        }

        return earliestSelection;
    }

    private int compareSlotSelections(
            SlotSelection first,
            SlotSelection second
    ) {

        int startComparison =
                first.start().compareTo(
                        second.start()
                );

        if (startComparison != 0) {
            return startComparison;
        }

        int workshopComparison =
                Long.compare(
                        first.workshop().getId(),
                        second.workshop().getId()
                );

        if (workshopComparison != 0) {
            return workshopComparison;
        }

        int bayComparison =
                Long.compare(
                        first.slot().bayId(),
                        second.slot().bayId()
                );

        if (bayComparison != 0) {
            return bayComparison;
        }

        return Long.compare(
                first.slot().technicianId(),
                second.slot().technicianId()
        );
    }

    /*
     * ==============================================================
     * Find the earliest slot at one workshop
     * ==============================================================
     */

    private SlotSelection findEarliestSlotAtWorkshop(
            Workshop workshop,
            MaintenancePlan maintenancePlan
    ) {

        ZoneId workshopZone =
                getWorkshopZone(workshop);

        /*
         * Start from tomorrow.
         *
         * The slot engine searches complete working days and does not
         * currently accept an earliest LocalTime for the first day.
         * Starting tomorrow prevents creation of a booking in the past.
         */
        LocalDate searchStartDate =
                LocalDate.now(
                        clock.withZone(workshopZone)
                ).plusDays(1);

        LocalDate searchEndDateExclusive =
                searchStartDate.plusDays(
                        SEARCH_HORIZON_DAYS
                );

        List<BayCandidate> bayCandidates =
                buildBayCandidates(
                        workshop.getId()
                );

        if (bayCandidates.isEmpty()) {
            return null;
        }

        List<TechnicianCandidate> technicianCandidates =
                buildTechnicianCandidates(
                        workshop.getId()
                );

        if (technicianCandidates.isEmpty()) {
            return null;
        }

        com.example.slotengine.model.WorkingCalendar
                slotEngineCalendar =
                buildSlotEngineCalendar(
                        workshop.getId(),
                        searchStartDate,
                        searchEndDateExclusive
                );

        if (slotEngineCalendar.weeklyHours().isEmpty()) {
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
                        horizonEnd
                );

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
                                SEARCH_HORIZON_DAYS
                        ),
                        MAX_RESULTS_PER_WORKSHOP
                );

        List<FeasibleSlot> feasibleSlots =
                FeasibleSlotEngine.findSlots(
                        request
                );

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

        return new SlotSelection(
                workshop,
                firstSlot,
                start,
                end
        );
    }

    private ZoneId getWorkshopZone(
            Workshop workshop
    ) {

        if (workshop.getTimeZone() == null
                || workshop.getTimeZone().isBlank()) {

            throw new IllegalStateException(
                    "Time zone is not configured for workshop "
                            + workshop.getId()
            );
        }

        try {
            return ZoneId.of(
                    workshop.getTimeZone()
            );

        } catch (RuntimeException exception) {

            throw new IllegalStateException(
                    "Invalid time zone configured for workshop "
                            + workshop.getId()
                            + ": "
                            + workshop.getTimeZone(),
                    exception
            );
        }
    }

    /*
     * ==============================================================
     * Convert bays into slot-engine input
     * ==============================================================
     */

    private List<BayCandidate> buildBayCandidates(
            Long workshopId
    ) {

        return bayRepository
                .findByWorkshop_IdOrderByIdAsc(
                        workshopId
                )
                .stream()
                .map(bay -> {

                    Set<String> capabilityCodes =
                            bay.getCapabilities()
                                    .stream()
                                    .map(
                                            BayCapability::getCapability
                                    )
                                    .map(
                                            Capability::getCapabilityCode
                                    )
                                    .collect(
                                            Collectors.toSet()
                                    );

                    return new BayCandidate(
                            bay.getId(),
                            Boolean.TRUE.equals(
                                    bay.getIsActive()
                            ),
                            capabilityCodes
                    );
                })
                .toList();
    }

    /*
     * ==============================================================
     * Convert technicians into slot-engine input
     * ==============================================================
     */

    private List<TechnicianCandidate>
    buildTechnicianCandidates(
            Long workshopId
    ) {

        return technicianRepository
                .findByWorkshop_IdOrderByIdAsc(
                        workshopId
                )
                .stream()
                .map(technician -> {

                    List<SkillCertification> certifications =
                            technician
                                    .getTechnicianSkills()
                                    .stream()
                                    .map(
                                            this::toSkillCertification
                                    )
                                    .toList();

                    return new TechnicianCandidate(
                            technician.getId(),
                            Boolean.TRUE.equals(
                                    technician.getActive()
                            ),
                            certifications
                    );
                })
                .toList();
    }

    private SkillCertification toSkillCertification(
            TechnicianSkill technicianSkill
    ) {

        return new SkillCertification(
                technicianSkill
                        .getSkill()
                        .getSkillCode(),
                technicianSkill.getValidFrom(),
                technicianSkill.getValidTo()
        );
    }

    /*
     * ==============================================================
     * Convert workshop calendar into slot-engine input
     * ==============================================================
     */

    private com.example.slotengine.model.WorkingCalendar
    buildSlotEngineCalendar(
            Long workshopId,
            LocalDate startDate,
            LocalDate endDateExclusive
    ) {

        Map<DayOfWeek, WorkingDayHours> weeklyHours =
                workingCalendarRepository
                        .findByWorkshop_IdOrderByDayOfWeekAsc(
                                workshopId
                        )
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        calendar ->
                                                DayOfWeek.of(
                                                        calendar
                                                                .getDayOfWeek()
                                                                .intValue()
                                                ),
                                        calendar ->
                                                new WorkingDayHours(
                                                        calendar.getOpenTime(),
                                                        calendar.getCloseTime()
                                                ),
                                        (first, second) -> {
                                            throw new IllegalStateException(
                                                    "Duplicate working-calendar "
                                                            + "entry for workshop "
                                                            + workshopId
                                            );
                                        },
                                        () ->
                                                new EnumMap<>(
                                                        DayOfWeek.class
                                                )
                                )
                        );

        Set<LocalDate> holidayDates =
                holidayRepository
                        .findApplicableHolidays(
                                workshopId,
                                startDate,
                                endDateExclusive
                        )
                        .stream()
                        .map(
                                Holiday::getHolidayDate
                        )
                        .collect(
                                Collectors.toSet()
                        );

        return new com.example.slotengine.model.WorkingCalendar(
                weeklyHours,
                holidayDates
        );
    }

    /*
     * ==============================================================
     * Convert active database bookings into slot-engine input
     * ==============================================================
     */

    private List<ExistingBooking> buildExistingBookings(
            Long workshopId,
            ZoneId workshopZone,
            OffsetDateTime horizonStart,
            OffsetDateTime horizonEnd
    ) {

        return bookingRepository
                .findBlockingBookings(
                        workshopId,
                        horizonStart,
                        horizonEnd
                )
                .stream()
                .map(booking ->
                        toExistingBooking(
                                booking,
                                workshopZone
                        )
                )
                .toList();
    }

    private ExistingBooking toExistingBooking(
            Booking booking,
            ZoneId workshopZone
    ) {

        if (booking.getSlot() == null) {
            throw new IllegalStateException(
                    "Booking "
                            + booking.getId()
                            + " does not contain a slot"
            );
        }

        OffsetDateTime start =
                getBoundedRangeValue(
                        booking
                                .getSlot()
                                .getLowerBound(),
                        booking.getId(),
                        "lower"
                );

        OffsetDateTime end =
                getBoundedRangeValue(
                        booking
                                .getSlot()
                                .getUpperBound(),
                        booking.getId(),
                        "upper"
                );

        ZonedDateTime localStart =
                start.atZoneSameInstant(
                        workshopZone
                );

        ZonedDateTime localEnd =
                end.atZoneSameInstant(
                        workshopZone
                );

        if (!localStart
                .toLocalDate()
                .equals(localEnd.toLocalDate())) {

            throw new IllegalStateException(
                    "Existing booking "
                            + booking.getId()
                            + " spans multiple workshop-local dates"
            );
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
                booking.getBayId(),
                booking.getTechnicianId()
        );
    }

    private OffsetDateTime getBoundedRangeValue(
            Range.Bound<OffsetDateTime> bound,
            Long bookingId,
            String boundName
    ) {

        return bound
                .getValue()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Booking "
                                        + bookingId
                                        + " contains an unbounded "
                                        + boundName
                                        + " slot value"
                        )
                );
    }

    /*
     * ==============================================================
     * Build preventive Booking entity
     * ==============================================================
     */

    private Booking buildPreventiveBooking(
            Asset asset,
            MaintenancePlan maintenancePlan,
            SlotSelection selectedSlot
    ) {

        Booking booking =
                new Booking();

        booking.setAssetId(
                asset.getId()
        );

        booking.setWorkshop(
                selectedSlot.workshop()
        );

        booking.setBayId(
                selectedSlot.slot().bayId()
        );

        booking.setTechnicianId(
                selectedSlot.slot().technicianId()
        );

        /*
         * Range.rightOpen creates:
         *
         * [start, end)
         *
         * This matches the slot engine and PostgreSQL tstzrange
         * half-open interval convention.
         */
        booking.setSlot(
                Range.rightOpen(
                        selectedSlot.start(),
                        selectedSlot.end()
                )
        );

        booking.setKind(
                BookingKind.PREVENTIVE
        );

        booking.setMaintenancePlanId(
                maintenancePlan.getId()
        );

        booking.setBreakdownRequest(
                null
        );

        booking.setStatus(
                BookingStatus.CONFIRMED
        );

        /*
         * Do not manually set the version.
         * Hibernate manages the @Version field.
         */

        return booking;
    }

    /*
     * ==============================================================
     * Build scheduled WorkOrder entity
     * ==============================================================
     */

    private WorkOrder buildScheduledWorkOrder(
            Asset asset,
            Booking booking
    ) {

        WorkOrder workOrder =
                new WorkOrder();

        workOrder.setWorkOrderNumber(
                generateWorkOrderNumber()
        );

        workOrder.setBooking(
                booking
        );

        workOrder.setAssetId(
                asset.getId()
        );

        workOrder.setStatus(
                WorkOrderStatus.SCHEDULED
        );

        workOrder.setStartedAt(
                null
        );

        workOrder.setCompletedAt(
                null
        );

        workOrder.setOdometerAtService(
                null
        );

        workOrder.setTotalCost(
                BigDecimal.ZERO
        );

        workOrder.setIdempotencyKey(
                null
        );

        /*
         * Do not manually set the version.
         * Hibernate manages the @Version field.
         */

        return workOrder;
    }

    private String generateWorkOrderNumber() {

        return "WO-"
                + UUID.randomUUID()
                .toString()
                .toUpperCase();
    }

    /*
     * ==============================================================
     * Response mapping
     * ==============================================================
     */

    private PreventiveBookingResponse
    buildPreventiveBookingResponse(
            Booking booking,
            WorkOrder workOrder,
            SlotSelection selectedSlot
    ) {

        PreventiveBookingResponse response =
                new PreventiveBookingResponse();

        response.setBookingId(
                booking.getId()
        );

        response.setWorkOrderId(
                workOrder.getId()
        );

        response.setWorkOrderNumber(
                workOrder.getWorkOrderNumber()
        );

        response.setAssetId(
                booking.getAssetId()
        );

        response.setMaintenancePlanId(
                booking.getMaintenancePlanId()
        );

        response.setWorkshopId(
                booking.getWorkshop().getId()
        );

        response.setBayId(
                booking.getBayId()
        );

        response.setTechnicianId(
                booking.getTechnicianId()
        );

        response.setStart(
                selectedSlot.start()
        );

        response.setEnd(
                selectedSlot.end()
        );

        response.setBookingKind(
                booking.getKind().name()
        );

        response.setBookingStatus(
                booking.getStatus().name()
        );

        response.setWorkOrderStatus(
                workOrder.getStatus().name()
        );

        return response;
    }

    /*
     * ==============================================================
     * Internal value object
     * ==============================================================
     */

    private record SlotSelection(
            Workshop workshop,
            FeasibleSlot slot,
            OffsetDateTime start,
            OffsetDateTime end
    ) {
    }
}