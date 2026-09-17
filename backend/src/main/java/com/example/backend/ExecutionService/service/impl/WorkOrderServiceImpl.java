package com.example.backend.ExecutionService.service.impl;

import com.example.backend.AssetManagamentService.entity.OdometerReading;
import com.example.backend.AssetManagamentService.exception.BusinessValidationException;
import com.example.backend.AssetManagamentService.exception.ResourceNotFoundException;
import com.example.backend.AssetManagamentService.repository.OdometerReadingRepository;
import com.example.backend.CapacityAndSchedulingService.entity.Technician;
import com.example.backend.ExecutionService.dto.workorder.CompleteWorkOrderRequest;
import com.example.backend.ExecutionService.dto.workorder.WorkOrderResponse;
import com.example.backend.ExecutionService.entity.Booking;
import com.example.backend.ExecutionService.entity.BookingHistory;
import com.example.backend.ExecutionService.entity.WorkOrder;
import com.example.backend.ExecutionService.entity.WorkOrderLabour;
import com.example.backend.ExecutionService.mapper.WorkOrderMapper;
import com.example.backend.ExecutionService.repository.*;
import com.example.backend.ExecutionService.service.WorkOrderService;
import com.example.backend.ExecutionService.status.BookingKind;
import com.example.backend.ExecutionService.status.BookingStatus;
import com.example.backend.ExecutionService.status.WorkOrderStatus;
import com.example.backend.ExecutionService.workflow.BreakdownEvent;
import com.example.backend.ExecutionService.workflow.BreakdownRequestStateMachine;
import com.example.backend.ExecutionService.workflow.WorkOrderEvent;
import com.example.backend.ExecutionService.workflow.WorkOrderStateMachine;
import com.example.backend.SLA.dto.BreakdownStatus;
import com.example.backend.SLA.entity.BreakdownRequest;
import com.example.backend.SLA.repository.BreakdownRequestRepository;
import com.example.backend.SLA.service.SlaCheckpointService;
import com.example.backend.SecurityService.entity.AppUser;
import com.example.backend.SecurityService.repository.UserRepository;
import com.example.backend.common.exception.GlobalExceptionHandler;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;

@Service
public class WorkOrderServiceImpl implements WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final Clock clock;
    private final WorkOrderMapper workOrderMapper;
    private final WorkOrderStateMachine workOrderStateMachine;
    private final SlaCheckpointService slaCheckpointService;

    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 100;

    private final WorkOrderLabourRepository
            workOrderLabourRepository;

    private final WorkOrderPartRepository
            workOrderPartRepository;

    private final BookingRepository bookingRepository;

    private final BreakdownRequestRepository
            breakdownRequestRepository;

    private final BreakdownRequestStateMachine
            breakdownRequestStateMachine;

    private final OdometerReadingRepository odometerReadingRepository;

    private final BookingHistoryRepository bookingHistoryRepository;

    private final UserRepository userRepository;

    public WorkOrderServiceImpl(WorkOrderRepository workOrderRepository, Clock clock, WorkOrderMapper workOrderMapper, WorkOrderStateMachine workOrderStateMachine, SlaCheckpointService slaCheckpointService, WorkOrderLabourRepository workOrderLabourRepository, WorkOrderPartRepository workOrderPartRepository, BookingRepository bookingRepository, BreakdownRequestRepository breakdownRequestRepository, BreakdownRequestStateMachine breakdownRequestStateMachine, OdometerReadingRepository odometerReadingRepository, BookingHistoryRepository bookingHistoryRepository, UserRepository userRepository) {
        this.workOrderRepository = workOrderRepository;
        this.clock = clock;
        this.workOrderMapper = workOrderMapper;
        this.workOrderStateMachine = workOrderStateMachine;
        this.slaCheckpointService = slaCheckpointService;
        this.workOrderLabourRepository = workOrderLabourRepository;
        this.workOrderPartRepository = workOrderPartRepository;
        this.bookingRepository = bookingRepository;
        this.breakdownRequestRepository = breakdownRequestRepository;
        this.breakdownRequestStateMachine = breakdownRequestStateMachine;
        this.odometerReadingRepository = odometerReadingRepository;
        this.bookingHistoryRepository = bookingHistoryRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public WorkOrderResponse startWorkOrder(
            Long workOrderId) {

        validateWorkOrderId(workOrderId);

        WorkOrder workOrder =
                findWorkOrderForTransition(
                        workOrderId);

        Booking booking =
                requireBooking(
                        workOrder);

        validateConfirmedBooking(
                booking,
                "start work");

        if (workOrder.getStartedAt() != null) {
            throw new GlobalExceptionHandler.ConflictException(
                    "Work order "
                            + workOrderId
                            + " has already been started");
        }

        if (workOrder.getStatus()
                != WorkOrderStatus.SCHEDULED) {

            throw new GlobalExceptionHandler.ConflictException(
                    "Only SCHEDULED work orders can be started");
        }

        validateAssignedTechnician(
                booking);

        WorkOrderStatus nextStatus =
                workOrderStateMachine.nextState(
                        workOrder.getStatus(),
                        WorkOrderEvent.START_WORK);

        OffsetDateTime startedAt =
                OffsetDateTime.now(clock);

        workOrder.setStartedAt(
                startedAt);

        workOrder.setStatus(
                nextStatus);

        /*
         * Corrective maintenance:
         * response SLA stops when technician
         * actually starts working.
         */
        if (booking.getKind()
                == BookingKind.CORRECTIVE) {

            slaCheckpointService.recordResponse(
                    booking.getId(),
                    startedAt);
        }

        WorkOrder savedWorkOrder =
                workOrderRepository.save(
                        workOrder);

        return workOrderMapper.toResponse(
                savedWorkOrder);
    }

    private void validateAssignedTechnician(
            Booking booking) {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication
                instanceof AnonymousAuthenticationToken) {

            throw new GlobalExceptionHandler.ConflictException(
                    "An authenticated technician is required "
                            + "to start a work order");
        }

        String username =
                authentication.getName();

        Technician assignedTechnician =
                booking.getTechnician();

        if (assignedTechnician == null) {
            throw new GlobalExceptionHandler.ConflictException(
                    "Booking "
                            + booking.getId()
                            + " has no assigned technician");
        }

        AppUser assignedUser =
                assignedTechnician.getAppUser();

        if (assignedUser == null) {
            throw new GlobalExceptionHandler.ConflictException(
                    "Assigned technician has no linked user");
        }

        if (!assignedUser.getUsername()
                .equals(username)) {

            throw new GlobalExceptionHandler.ConflictException(
                    "Only the assigned technician can start "
                            + "this work order");
        }
    }

    private WorkOrder findWorkOrderForTransition(
            Long workOrderId) {

        return workOrderRepository
                .findById(workOrderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Work order not found with id: "
                                        + workOrderId));
    }

    private Booking requireBooking(
            WorkOrder workOrder) {

        Booking booking =
                workOrder.getBooking();

        if (booking == null) {

            throw new BusinessValidationException(
                    "Work order "
                            + workOrder.getId()
                            + " is not linked to a booking");
        }

        return booking;
    }

    private void validateWorkOrderId(
            Long workOrderId) {

        if (workOrderId == null) {

            throw new IllegalArgumentException(
                    "Work order id is required");
        }

        if (workOrderId <= 0L) {

            throw new IllegalArgumentException(
                    "Work order id must be greater than zero");
        }
    }

    private void validateConfirmedBooking(
            Booking booking,
            String operation) {

        if (booking.getStatus() == null) {

            throw new BusinessValidationException(
                    "Booking "
                            + booking.getId()
                            + " has no status");
        }

        if (booking.getStatus()
                != BookingStatus.CONFIRMED) {

            throw new GlobalExceptionHandler.ConflictException(
                    "Cannot "
                            + operation
                            + ". Booking "
                            + booking.getId()
                            + " is currently "
                            + booking.getStatus());
        }
    }

    @Override
    @Transactional
    public WorkOrderResponse completeWorkOrder(
            Long workOrderId,
            CompleteWorkOrderRequest request,
            String idempotencyKey) {

        validateWorkOrderId(
                workOrderId);

        validateCompleteWorkOrderRequest(
                request);

        String normalizedIdempotencyKey =
                normalizeIdempotencyKey(
                        idempotencyKey);

        /*
         * Idempotent replay check.
         *
         * A replay must have:
         * 1. the same work-order ID
         * 2. the same odometer value
         * 3. the same labour hours
         */
        WorkOrder existingByKey =
                workOrderRepository
                        .findByIdempotencyKey(
                                normalizedIdempotencyKey)
                        .orElse(null);

        if (existingByKey != null) {

            validateIdempotentReplay(
                    existingByKey,
                    workOrderId,
                    request);

            return workOrderMapper.toResponse(
                    existingByKey);
        }

        /*
         * Lock the WorkOrder so two concurrent completion requests
         * cannot complete it simultaneously.
         */
        WorkOrder workOrder =
                workOrderRepository
                        .findByIdForTransition(
                                workOrderId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Work order not found with id: "
                                                + workOrderId));

        if (workOrder.getIdempotencyKey() != null) {

            if (!workOrder.getIdempotencyKey()
                    .equals(normalizedIdempotencyKey)) {

                throw new GlobalExceptionHandler.ConflictException(
                        "This work order has already been completed "
                                + "using a different idempotency key");
            }

            validateIdempotentReplay(
                    workOrder,
                    workOrderId,
                    request);

            return workOrderMapper.toResponse(
                    workOrder);
        }

        Booking booking =
                requireBooking(
                        workOrder);

        validateConfirmedBooking(
                booking,
                "complete work");

        validateAssignedTechnician(
                booking);

        if (workOrder.getStartedAt() == null) {
            throw new GlobalExceptionHandler.ConflictException(
                    "Work order "
                            + workOrderId
                            + " cannot be completed before it is started");
        }

        if (workOrder.getCompletedAt() != null) {
            throw new GlobalExceptionHandler.ConflictException(
                    "Work order "
                            + workOrderId
                            + " has already been completed");
        }

        /*
         * The state machine allows only:
         *
         * IN_PROGRESS + COMPLETE_WORK -> COMPLETED
         *
         * It rejects SCHEDULED, AWAITING_PARTS, COMPLETED,
         * and CANCELLED.
         */
        WorkOrderStatus nextStatus =
                workOrderStateMachine.nextState(
                        workOrder.getStatus(),
                        WorkOrderEvent.COMPLETE_WORK);

        validateCompletionOdometer(
                booking,
                request.getOdometerAtService());

        AppUser authenticatedUser =
                getAuthenticatedActiveUser();

        /*
         * Create the actual labour record for the one technician
         * assigned by the Booking.
         *
         * This also snapshots the current hourly rate.
         */
        createAssignedTechnicianLabour(
                workOrder,
                booking,
                request.getHoursWorked());

        /*
         * createAssignedTechnicianLabour() uses saveAndFlush().
         * Do not call flush() again here.
         */
        BigDecimal labourCost =
                valueOrZero(
                        workOrderLabourRepository
                                .calculateTotalLabourCost(
                                        workOrderId));

        BigDecimal partCost =
                valueOrZero(
                        workOrderPartRepository
                                .calculateTotalPartCost(
                                        workOrderId));

        BigDecimal totalCost =
                normalizeCalculatedCost(
                        labourCost.add(partCost));

        OffsetDateTime completedAt =
                OffsetDateTime.now(clock);

        if (completedAt.isBefore(
                workOrder.getStartedAt())) {

            throw new GlobalExceptionHandler.ConflictException(
                    "Work-order completion time cannot be before "
                            + "the work-order start time");
        }

        workOrder.setOdometerAtService(
                request.getOdometerAtService());

        workOrder.setCompletedAt(
                completedAt);

        workOrder.setTotalCost(
                totalCost);

        workOrder.setIdempotencyKey(
                normalizedIdempotencyKey);

        workOrder.setStatus(
                nextStatus);

        /*
         * COMPLETED bookings no longer participate in the partial
         * GiST exclusion constraints, so the bay and technician
         * capacity is released.
         */
        booking.setStatus(
                BookingStatus.COMPLETED);

        BreakdownRequest correctiveBreakdown =
                null;

        if (booking.getKind()
                == BookingKind.CORRECTIVE) {

            correctiveBreakdown =
                    requireCorrectiveBreakdown(
                            booking);

            BreakdownStatus nextBreakdownStatus =
                    breakdownRequestStateMachine.nextState(
                            correctiveBreakdown.getStatus(),
                            BreakdownEvent.RESOLVE);

            correctiveBreakdown.setStatus(
                    nextBreakdownStatus);

            breakdownRequestRepository.save(
                    correctiveBreakdown);
        }

        bookingRepository.save(
                booking);

        /*
         * Flush WorkOrder state before SLA calculation.
         *
         * If the SLA calculator queries WorkOrder, it will now see:
         *
         * status = COMPLETED
         * completedAt = actual completion timestamp
         */
        WorkOrder savedWorkOrder;

        try {
            savedWorkOrder =
                    workOrderRepository.saveAndFlush(
                            workOrder);

        } catch (DataIntegrityViolationException exception) {

            throw new GlobalExceptionHandler.ConflictException(
                    "The work order could not be completed because "
                            + "the request violated an idempotency "
                            + "or completion constraint");
        }

        /*
         * Append service odometer reading after WorkOrder flush.
         * The entire method is still one transaction.
         */
        appendServiceOdometerReading(
                booking,
                request.getOdometerAtService(),
                completedAt,
                authenticatedUser);

        /*
         * Create BookingHistory because the Booking moved from
         * CONFIRMED to COMPLETED.
         */
        createCompletionHistory(
                booking,
                authenticatedUser,
                completedAt);

        /*
         * Only corrective bookings have an SLA checkpoint.
         *
         * Run after the completed WorkOrder is flushed so SLA queries
         * see the final completion state.
         */
        if (booking.getKind()
                == BookingKind.CORRECTIVE) {

            slaCheckpointService.recordResolution(
                    booking.getId(),
                    completedAt);
        }

        return workOrderMapper.toResponse(
                savedWorkOrder);
    }

    private void createAssignedTechnicianLabour(
            WorkOrder workOrder,
            Booking booking,
            BigDecimal hoursWorked) {

        Technician assignedTechnician =
                booking.getTechnician();

        if (assignedTechnician == null
                || assignedTechnician.getId() == null) {

            throw new BusinessValidationException(
                    "Booking "
                            + booking.getId()
                            + " has no assigned technician");
        }

        BigDecimal hourlyRate =
                assignedTechnician.getHourlyRate();

        if (hourlyRate == null
                || hourlyRate.compareTo(
                BigDecimal.ZERO) < 0) {

            throw new BusinessValidationException(
                    "Assigned technician does not have "
                            + "a valid hourly rate");
        }

        if (hourlyRate.scale() > 2
                || hourlyRate.precision() > 12) {

            throw new BusinessValidationException(
                    "Assigned technician hourly rate exceeds "
                            + "the supported NUMERIC(12,2) format");
        }

        if (workOrderLabourRepository
                .findByWorkOrder_IdAndTechnician_Id(
                        workOrder.getId(),
                        assignedTechnician.getId())
                .isPresent()) {

            throw new GlobalExceptionHandler.ConflictException(
                    "Labour has already been recorded for "
                            + "the assigned technician on work order "
                            + workOrder.getId());
        }

        WorkOrderLabour labour =
                new WorkOrderLabour();

        labour.setWorkOrder(
                workOrder);

        labour.setTechnician(
                assignedTechnician);

        labour.setHours(
                hoursWorked);

        /*
         * Snapshot rate_applied now.
         *
         * A future change to technician.hourlyRate must not change
         * historical work-order cost.
         */
        labour.setRateApplied(
                hourlyRate);

        WorkOrderLabour savedLabour =
                workOrderLabourRepository.saveAndFlush(
                        labour);

        workOrder.getLabourEntries()
                .add(savedLabour);
    }

    private void validateCompletionOdometer(
            Booking booking,
            BigDecimal odometerAtService) {

        if (booking.getAsset() == null
                || booking.getAsset().getId() == null) {

            throw new BusinessValidationException(
                    "Booking "
                            + booking.getId()
                            + " has no associated asset");
        }

        BigDecimal latestOdometer =
                odometerReadingRepository
                        .findFirstByAsset_IdOrderByReadAtDesc(
                                booking.getAsset().getId())
                        .map(OdometerReading::getReadingKm)
                        .orElse(
                                booking.getAsset()
                                        .getAcquisitionOdometerKm());

        if (latestOdometer != null
                && odometerAtService
                .compareTo(latestOdometer) < 0) {

            throw new BusinessValidationException(
                    "Odometer reading at service cannot be below "
                            + "the latest recorded odometer reading of "
                            + latestOdometer);
        }
    }

    private String normalizeIdempotencyKey(
            String idempotencyKey) {

        if (idempotencyKey == null
                || idempotencyKey.isBlank()) {

            throw new BusinessValidationException(
                    "Idempotency-Key header is required");
        }

        String normalized =
                idempotencyKey.trim();

        if (normalized.length()
                > MAX_IDEMPOTENCY_KEY_LENGTH) {

            throw new BusinessValidationException(
                    "Idempotency key cannot exceed "
                            + MAX_IDEMPOTENCY_KEY_LENGTH
                            + " characters");
        }

        return normalized;
    }

    private BigDecimal valueOrZero(
            BigDecimal value) {

        return value == null
                ? BigDecimal.ZERO
                : value;
    }

    private BreakdownRequest requireCorrectiveBreakdown(
            Booking booking) {

        BreakdownRequest breakdownRequest =
                booking.getBreakdownRequest();

        if (breakdownRequest == null) {
            throw new BusinessValidationException(
                    "Corrective booking "
                            + booking.getId()
                            + " is not linked to a breakdown request");
        }

        return breakdownRequest;
    }

    private void validateCompleteWorkOrderRequest(
            CompleteWorkOrderRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Complete work order request is required");
        }

        BigDecimal odometerAtService =
                request.getOdometerAtService();

        if (odometerAtService == null) {
            throw new BusinessValidationException(
                    "Odometer reading at service is required");
        }

        if (odometerAtService
                .compareTo(BigDecimal.ZERO) < 0) {

            throw new BusinessValidationException(
                    "Odometer reading at service cannot be negative");
        }

        if (odometerAtService.scale() > 3
                || odometerAtService.precision() > 12) {

            throw new BusinessValidationException(
                    "Odometer reading must contain at most "
                            + "12 total digits and 3 decimal places");
        }

        BigDecimal hoursWorked =
                request.getHoursWorked();

        if (hoursWorked == null) {
            throw new BusinessValidationException(
                    "Hours worked is required");
        }

        if (hoursWorked
                .compareTo(BigDecimal.ZERO) <= 0) {

            throw new BusinessValidationException(
                    "Hours worked must be greater than zero");
        }

        if (hoursWorked.scale() > 2
                || hoursWorked.precision() > 5) {

            throw new BusinessValidationException(
                    "Hours worked must contain at most "
                            + "5 total digits and 2 decimal places");
        }
    }

    private void validateIdempotentReplay(
            WorkOrder existingWorkOrder,
            Long requestedWorkOrderId,
            CompleteWorkOrderRequest request) {

        if (!existingWorkOrder.getId()
                .equals(requestedWorkOrderId)) {

            throw new GlobalExceptionHandler.ConflictException(
                    "The supplied idempotency key has already "
                            + "been used for another work order");
        }

        if (existingWorkOrder.getStatus()
                != WorkOrderStatus.COMPLETED) {

            throw new GlobalExceptionHandler.ConflictException(
                    "The supplied idempotency key belongs to "
                            + "a work order that is not completed");
        }

        if (existingWorkOrder.getOdometerAtService() == null
                || existingWorkOrder
                .getOdometerAtService()
                .compareTo(
                        request.getOdometerAtService()) != 0) {

            throw new GlobalExceptionHandler.ConflictException(
                    "The supplied idempotency key was previously used "
                            + "with a different odometer reading");
        }

        Booking booking =
                requireBooking(
                        existingWorkOrder);

        Technician assignedTechnician =
                booking.getTechnician();

        if (assignedTechnician == null
                || assignedTechnician.getId() == null) {

            throw new GlobalExceptionHandler.ConflictException(
                    "Completed work order does not contain "
                            + "an assigned technician");
        }

        WorkOrderLabour existingLabour =
                workOrderLabourRepository
                        .findByWorkOrder_IdAndTechnician_Id(
                                existingWorkOrder.getId(),
                                assignedTechnician.getId())
                        .orElseThrow(() ->
                                new GlobalExceptionHandler.ConflictException(
                                        "Completed work order does not contain "
                                                + "the assigned technician's "
                                                + "labour record"));

        if (existingLabour.getHours() == null
                || existingLabour
                .getHours()
                .compareTo(
                        request.getHoursWorked()) != 0) {

            throw new GlobalExceptionHandler.ConflictException(
                    "The supplied idempotency key was previously used "
                            + "with different labour hours");
        }
    }

    private void appendServiceOdometerReading(
            Booking booking,
            BigDecimal odometerAtService,
            OffsetDateTime completedAt,
            AppUser authenticatedUser) {

        OdometerReading odometerReading =
                new OdometerReading();

        odometerReading.setAsset(
                booking.getAsset());

        odometerReading.setReadingKm(
                odometerAtService);

        odometerReading.setReadAt(
                completedAt);

        odometerReading.setRecordedById(
                authenticatedUser.getId());

        odometerReadingRepository.save(
                odometerReading);
    }

    private void createCompletionHistory(
            Booking booking,
            AppUser authenticatedUser,
            OffsetDateTime completedAt) {

        BookingHistory history =
                new BookingHistory();

        history.setBooking(
                booking);

        history.setAction(
                "COMPLETED");

        /*
         * The old active slot is recorded as previous.
         *
         * There is no replacement slot because this is completion,
         * not rescheduling.
         */
        history.setPreviousStartAt(booking.getStartAt());
        history.setPreviousEndAt(booking.getEndAt());

        history.setNewStartAt(null);
        history.setNewEndAt(null);

        history.setActor(
                authenticatedUser);

        history.setReason(
                "Work order completed");

        history.setOccurredAt(
                completedAt);

        bookingHistoryRepository.save(
                history);
    }

    private AppUser getAuthenticatedActiveUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication
                instanceof AnonymousAuthenticationToken) {

            throw new GlobalExceptionHandler.ConflictException(
                    "An authenticated technician is required "
                            + "to complete the work order");
        }

        String username =
                authentication.getName();

        if (username == null
                || username.isBlank()) {

            throw new GlobalExceptionHandler.ConflictException(
                    "Authenticated username is unavailable");
        }

        AppUser appUser =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Authenticated user was not found: "
                                                + username));

        if (!Boolean.TRUE.equals(
                appUser.getIsActive())) {

            throw new GlobalExceptionHandler.ConflictException(
                    "Inactive users cannot complete work orders");
        }

        return appUser;
    }

    private BigDecimal normalizeCalculatedCost(
            BigDecimal totalCost) {

        BigDecimal normalized =
                valueOrZero(totalCost)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP);

        if (normalized.precision() > 12) {
            throw new BusinessValidationException(
                    "Calculated work-order total exceeds "
                            + "the supported NUMERIC(12,2) limit");
        }

        return normalized;
    }

    @Override
    public java.util.List<WorkOrderResponse> getAllWorkOrders() {

        return workOrderRepository
                .findAll()
                .stream()
                .map(workOrderMapper::toResponse)
                .toList();
    }

    @Override
    public com.example.backend.ExecutionService.dto.workorder.WorkOrderDetailsResponse
    getWorkOrder(Long workOrderId) {

        validateWorkOrderId(workOrderId);

        WorkOrder workOrder =
                workOrderRepository
                        .findById(workOrderId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Work order not found with id: "
                                                + workOrderId));

        return workOrderMapper.toDetailsResponse(
                workOrder);
    }
}
