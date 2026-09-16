package com.example.backend.ExecutionService.service.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.AssetManagamentService.exception.BusinessValidationException;
import com.example.backend.AssetManagamentService.exception.ResourceNotFoundException;
import com.example.backend.CapacityAndSchedulingService.entity.Technician;
import com.example.backend.ExecutionService.dto.workorder.IssueWorkOrderPartRequest;
import com.example.backend.ExecutionService.dto.workorder.WorkOrderResponse;
import com.example.backend.ExecutionService.entity.Booking;
import com.example.backend.ExecutionService.entity.WorkOrder;
import com.example.backend.ExecutionService.entity.WorkOrderPart;
import com.example.backend.ExecutionService.entity.WorkOrderPartRequirement;
import com.example.backend.ExecutionService.mapper.WorkOrderMapper;
import com.example.backend.ExecutionService.repository.WorkOrderPartRepository;
import com.example.backend.ExecutionService.repository.WorkOrderPartRequirementRepository;
import com.example.backend.ExecutionService.repository.WorkOrderRepository;
import com.example.backend.ExecutionService.service.WorkOrderIssuePartService;
import com.example.backend.ExecutionService.status.BookingStatus;
import com.example.backend.ExecutionService.status.WorkOrderPartRequirementStatus;
import com.example.backend.ExecutionService.status.WorkOrderStatus;
import com.example.backend.InventoryService.dtos.inventorymovement.InventoryMovementResponse;
import com.example.backend.InventoryService.dtos.inventorymovement.IssuePartRequest;
import com.example.backend.InventoryService.entity.InventoryMovement;
import com.example.backend.InventoryService.entity.Part;
import com.example.backend.InventoryService.repository.InventoryMovementRepository;
import com.example.backend.InventoryService.repository.PartRepository;
import com.example.backend.InventoryService.service.InventoryService;
import com.example.backend.SecurityService.entity.AppUser;
import com.example.backend.common.exception.GlobalExceptionHandler;

@Service
public class WorkOrderIssuePartServiceImpl
        implements WorkOrderIssuePartService {

    private final WorkOrderRepository workOrderRepository;

    private final WorkOrderPartRepository workOrderPartRepository;

    private final WorkOrderPartRequirementRepository
            requirementRepository;

    private final InventoryMovementRepository
            inventoryMovementRepository;

    private final PartRepository partRepository;

    private final InventoryService inventoryService;

    private final WorkOrderMapper workOrderMapper;

    private final Clock clock;

    public WorkOrderIssuePartServiceImpl(
            WorkOrderRepository workOrderRepository,
            WorkOrderPartRepository workOrderPartRepository,
            WorkOrderPartRequirementRepository requirementRepository,
            InventoryMovementRepository inventoryMovementRepository,
            PartRepository partRepository,
            InventoryService inventoryService,
            WorkOrderMapper workOrderMapper,
            Clock clock) {

        this.workOrderRepository =
                workOrderRepository;

        this.workOrderPartRepository =
                workOrderPartRepository;

        this.requirementRepository =
                requirementRepository;

        this.inventoryMovementRepository =
                inventoryMovementRepository;

        this.partRepository =
                partRepository;

        this.inventoryService =
                inventoryService;

        this.workOrderMapper =
                workOrderMapper;

        this.clock =
                clock;
    }

    @Override
    @Transactional
    public WorkOrderResponse issuePart(
            Long workOrderId,
            IssueWorkOrderPartRequest request) {

        validateWorkOrderId(
                workOrderId);

        validateRequest(
                request);

        WorkOrder workOrder =
                workOrderRepository
                        .findByIdForTransition(
                                workOrderId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Work order not found with id "
                                                + workOrderId));

        Booking booking =
                requireBooking(
                        workOrder);

        validateBooking(
                booking);

        validateAssignedTechnician(
                booking);

        validateWorkOrderStatus(
                workOrder);

        Part part =
                partRepository
                        .findById(
                                request.getPartId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Part not found with id "
                                                + request.getPartId()));

        if (!Boolean.TRUE.equals(
                part.getActive())) {

            throw new BusinessValidationException(
                    "Inactive part "
                            + part.getId()
                            + " cannot be issued");
        }

        if (booking.getWorkshop() == null
                || booking.getWorkshop().getId() == null) {

            throw new BusinessValidationException(
                    "Booking "
                            + booking.getId()
                            + " has no associated workshop");
        }

        WorkOrderPartRequirement pendingRequirement =
                findAndValidatePendingRequirement(
                        workOrder,
                        request);

        IssuePartRequest inventoryRequest =
                buildInventoryIssueRequest(
                        booking,
                        request);

        /*
         * InventoryService performs:
         *
         * 1. Part lock
         * 2. Workshop validation
         * 3. On-hand stock validation
         * 4. InventoryMovement ISSUE insertion
         *
         * Since both services use Spring transactions, this call
         * participates in the same transaction.
         */
        InventoryMovementResponse movementResponse =
                inventoryService.issuePart(
                        inventoryRequest);

        if (movementResponse == null
                || movementResponse.getId() == null) {

            throw new BusinessValidationException(
                    "Inventory issue did not return "
                            + "a valid movement");
        }

        InventoryMovement inventoryMovement =
                inventoryMovementRepository
                        .findById(
                                movementResponse.getId())
                        .orElseThrow(() ->
                                new BusinessValidationException(
                                        "Inventory movement "
                                                + movementResponse.getId()
                                                + " was not found after issue"));

        /*
         * The issued quantity in InventoryMovement is negative.
         * WorkOrderPart stores the positive consumed quantity.
         */
        WorkOrderPart workOrderPart =
                new WorkOrderPart();

        workOrderPart.setWorkOrder(
                workOrder);

        workOrderPart.setPart(
                part);

        workOrderPart.setQuantity(
                request.getQuantity());

        /*
         * Capture the issue-time unit cost from the actual
         * InventoryMovement, not from the current Part master.
         */
        workOrderPart.setUnitCost(
                inventoryMovement.getUnitCost());

        workOrderPart.setMovement(
                inventoryMovement);

        WorkOrderPart savedWorkOrderPart =
                workOrderPartRepository.save(
                        workOrderPart);

        workOrder.getPartEntries()
                .add(savedWorkOrderPart);

        /*
         * A requirement exists only when the WorkOrder was waiting
         * for this part. Normal IN_PROGRESS issues do not need a
         * requirement record.
         */
        if (pendingRequirement != null) {

            pendingRequirement.setStatus(
                    WorkOrderPartRequirementStatus.RESOLVED);

            pendingRequirement.setResolvedAt(
                    OffsetDateTime.now(clock));

            requirementRepository.save(
                    pendingRequirement);
        }

        /*
         * Do not change WorkOrder status here.
         *
         * If it is AWAITING_PARTS, the technician must call
         * resumeWork() after every pending requirement has been
         * resolved.
         */
        WorkOrder savedWorkOrder =
                workOrderRepository.save(
                        workOrder);

        return workOrderMapper.toResponse(
                savedWorkOrder);
    }

    private WorkOrderPartRequirement
    findAndValidatePendingRequirement(
            WorkOrder workOrder,
            IssueWorkOrderPartRequest request) {

        WorkOrderPartRequirement requirement =
                requirementRepository
                        .findByWorkOrder_IdAndPart_IdAndStatus(
                                workOrder.getId(),
                                request.getPartId(),
                                WorkOrderPartRequirementStatus.PENDING)
                        .orElse(null);

        if (workOrder.getStatus()
                == WorkOrderStatus.AWAITING_PARTS) {

            if (requirement == null) {
                throw new GlobalExceptionHandler.ConflictException(
                        "Work order "
                                + workOrder.getId()
                                + " has no pending requirement "
                                + "for part "
                                + request.getPartId());
            }

            /*
             * The current requirement model has no fulfilledQuantity.
             * Therefore partial fulfilment cannot be tracked safely.
             *
             * Require the issued quantity to exactly match the
             * pending quantity.
             */
            if (request.getQuantity()
                    .compareTo(
                            requirement.getQuantityRequired()) != 0) {

                throw new BusinessValidationException(
                        "Issued quantity must equal the pending "
                                + "required quantity of "
                                + requirement.getQuantityRequired()
                                + " for part "
                                + request.getPartId());
            }
        }

        return requirement;
    }

    private IssuePartRequest buildInventoryIssueRequest(
            Booking booking,
            IssueWorkOrderPartRequest request) {

        IssuePartRequest inventoryRequest =
                new IssuePartRequest();

        inventoryRequest.setPartId(
                request.getPartId());

        inventoryRequest.setWorkshopId(
                booking.getWorkshop().getId());

        inventoryRequest.setQuantity(
                request.getQuantity());

        return inventoryRequest;
    }

    private void validateWorkOrderStatus(
            WorkOrder workOrder) {

        if (workOrder.getStatus() == null) {
            throw new BusinessValidationException(
                    "Work order "
                            + workOrder.getId()
                            + " has no status");
        }

        if (workOrder.getStatus()
                != WorkOrderStatus.IN_PROGRESS
                && workOrder.getStatus()
                != WorkOrderStatus.AWAITING_PARTS) {

            throw new GlobalExceptionHandler.ConflictException(
                    "Parts can be issued only when the work order "
                            + "is IN_PROGRESS or AWAITING_PARTS. "
                            + "Current status is "
                            + workOrder.getStatus());
        }
    }

    private Booking requireBooking(
            WorkOrder workOrder) {

        Booking booking =
                workOrder.getBooking();

        if (booking == null
                || booking.getId() == null) {

            throw new BusinessValidationException(
                    "Work order "
                            + workOrder.getId()
                            + " is not linked to a booking");
        }

        return booking;
    }

    private void validateBooking(
            Booking booking) {

        if (booking.getStatus() == null) {
            throw new BusinessValidationException(
                    "Booking "
                            + booking.getId()
                            + " has no status");
        }

        if (booking.getStatus()
                != BookingStatus.CONFIRMED) {

            throw new GlobalExceptionHandler.ConflictException(
                    "Parts cannot be issued because booking "
                            + booking.getId()
                            + " is "
                            + booking.getStatus());
        }
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
                            + "to issue a work-order part");
        }

        String authenticatedUsername =
                authentication.getName();

        Technician assignedTechnician =
                booking.getTechnician();

        if (assignedTechnician == null) {
            throw new BusinessValidationException(
                    "Booking "
                            + booking.getId()
                            + " has no assigned technician");
        }

        AppUser assignedUser =
                assignedTechnician.getAppUser();

        if (assignedUser == null
                || assignedUser.getUsername() == null) {

            throw new BusinessValidationException(
                    "Assigned technician has no linked user");
        }

        if (!assignedUser.getUsername()
                .equals(authenticatedUsername)) {

            throw new GlobalExceptionHandler.ConflictException(
                    "Only the assigned technician can issue "
                            + "parts for this work order");
        }
    }

    private void validateWorkOrderId(
            Long workOrderId) {

        if (workOrderId == null
                || workOrderId <= 0L) {

            throw new BusinessValidationException(
                    "Work order ID must be a positive number");
        }
    }

    private void validateRequest(
            IssueWorkOrderPartRequest request) {

        if (request == null) {
            throw new BusinessValidationException(
                    "Issue work-order part request is required");
        }

        if (request.getPartId() == null
                || request.getPartId() <= 0L) {

            throw new BusinessValidationException(
                    "Part ID must be a positive number");
        }

        if (request.getQuantity() == null
                || request.getQuantity()
                .compareTo(BigDecimal.ZERO) <= 0) {

            throw new BusinessValidationException(
                    "Quantity must be greater than zero");
        }

        if (request.getQuantity().scale() > 3
                || request.getQuantity().precision() > 12) {

            throw new BusinessValidationException(
                    "Quantity must fit NUMERIC(12,3)");
        }
    }
}