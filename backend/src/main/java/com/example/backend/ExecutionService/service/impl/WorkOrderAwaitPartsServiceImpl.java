package com.example.backend.ExecutionService.service.impl;

import com.example.backend.AssetManagamentService.exception.BusinessValidationException;
import com.example.backend.AssetManagamentService.exception.ResourceNotFoundException;
import com.example.backend.ExecutionService.dto.workorder.CreatePartRequirementRequest;
import com.example.backend.ExecutionService.dto.workorder.WorkOrderResponse;
import com.example.backend.ExecutionService.entity.Booking;
import com.example.backend.ExecutionService.entity.WorkOrder;
import com.example.backend.ExecutionService.entity.WorkOrderPartRequirement;
import com.example.backend.ExecutionService.mapper.WorkOrderMapper;
import com.example.backend.ExecutionService.repository.WorkOrderPartRequirementRepository;
import com.example.backend.ExecutionService.repository.WorkOrderRepository;
import com.example.backend.ExecutionService.service.WorkOrderAwaitPartsService;
import com.example.backend.ExecutionService.status.WorkOrderPartRequirementStatus;
import com.example.backend.ExecutionService.status.WorkOrderStatus;
import com.example.backend.ExecutionService.workflow.WorkOrderEvent;
import com.example.backend.ExecutionService.workflow.WorkOrderStateMachine;
import com.example.backend.InventoryService.entity.Part;
import com.example.backend.InventoryService.repository.PartRepository;
import com.example.backend.SLA.service.SlaCheckpointService;
import com.example.backend.common.exception.GlobalExceptionHandler;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

@Service
public class WorkOrderAwaitPartsServiceImpl
        implements WorkOrderAwaitPartsService {

    private final WorkOrderRepository workOrderRepository;

    private final WorkOrderPartRequirementRepository
            requirementRepository;

    private final PartRepository partRepository;

    private final WorkOrderStateMachine
            workOrderStateMachine;

    private final WorkOrderMapper workOrderMapper;

    private final Clock clock;

    private final SlaCheckpointService slaCheckpointService;

    public WorkOrderAwaitPartsServiceImpl(
            WorkOrderRepository workOrderRepository,
            WorkOrderPartRequirementRepository requirementRepository,
            PartRepository partRepository,
            WorkOrderStateMachine workOrderStateMachine,
            WorkOrderMapper workOrderMapper,
            Clock clock,
            SlaCheckpointService slaCheckpointService
    ) {
        this.workOrderRepository =
                workOrderRepository;

        this.requirementRepository =
                requirementRepository;

        this.partRepository =
                partRepository;

        this.workOrderStateMachine =
                workOrderStateMachine;

        this.workOrderMapper =
                workOrderMapper;

        this.clock =
                clock;
        this.slaCheckpointService = slaCheckpointService;
    }

    @Override
    @Transactional
    public WorkOrderResponse awaitParts(
            Long workOrderId,
            CreatePartRequirementRequest request
    ) {

        validateRequest(request);

        WorkOrder workOrder =
                workOrderRepository
                        .findByIdForTransition(
                                workOrderId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Work order not found with id "
                                                + workOrderId
                                )
                        );

        if (requirementRepository
                .existsByWorkOrder_IdAndPart_IdAndStatus(
                        workOrderId,
                        request.getPartId(),
                        WorkOrderPartRequirementStatus.PENDING
                )) {

            throw new GlobalExceptionHandler
                    .ConflictException(
                    "Work order already has a pending "
                            + "part requirement"
            );
        }

        if (workOrder.getStatus()
                != WorkOrderStatus.IN_PROGRESS) {

            throw new GlobalExceptionHandler
                    .ConflictException(
                    "Only IN_PROGRESS work orders "
                            + "can move to AWAITING_PARTS"
            );
        }

        Booking booking =
                workOrder.getBooking();

        if (booking == null) {
            throw new BusinessValidationException(
                    "Work order is not linked "
                            + "to a booking"
            );
        }

        validateAssignedTechnician(
                booking
        );

        Part part =
                partRepository
                        .findById(
                                request.getPartId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Part not found with id "
                                                + request.getPartId()
                                )
                        );

        if (!Boolean.TRUE.equals(
                part.getActive()
        )) {

            throw new BusinessValidationException(
                    "Inactive parts cannot be used "
                            + "for work-order requirements"
            );
        }

        WorkOrderPartRequirement
                requirement =
                new WorkOrderPartRequirement();

        requirement.setWorkOrder(
                workOrder
        );

        requirement.setPart(
                part
        );

        requirement.setQuantityRequired(
                request.getQuantityRequired()
        );

        requirement.setReason(
                request.getReason()
        );

        requirement.setStatus(
                WorkOrderPartRequirementStatus
                        .PENDING
        );

        requirement.setCreatedAt(
                OffsetDateTime.now(clock)
        );

        requirementRepository.save(
                requirement
        );

        WorkOrderStatus nextStatus =
                workOrderStateMachine.nextState(
                        workOrder.getStatus(),
                        WorkOrderEvent.WAIT_FOR_PARTS
                );

        workOrder.setStatus(
                nextStatus
        );

        /// sla clock pauses
        slaCheckpointService.recordAwaitingParts(
                workOrder.getBooking().getId(),
                OffsetDateTime.now(clock)
        );

        WorkOrder savedWorkOrder =
                workOrderRepository.save(
                        workOrder
                );

        return workOrderMapper.toResponse(
                savedWorkOrder
        );
    }

    private void validateRequest(
            CreatePartRequirementRequest request
    ) {

        if (request == null) {
            throw new BusinessValidationException(
                    "Await-parts request is required"
            );
        }

        if (request.getPartId() == null
                || request.getPartId() <= 0) {

            throw new BusinessValidationException(
                    "Valid part id is required"
            );
        }

        if (request.getQuantityRequired() == null) {

            throw new BusinessValidationException(
                    "Quantity required is mandatory"
            );
        }

        if (request.getQuantityRequired().signum() <= 0) {

            throw new BusinessValidationException(
                    "Quantity required must be greater than zero"
            );
        }

        if (request.getReason() == null
                || request.getReason().isBlank()) {

            throw new BusinessValidationException(
                    "Reason is required"
            );
        }

        if (request.getReason().trim().length() > 500) {

            throw new BusinessValidationException(
                    "Reason cannot exceed 500 characters"
            );
        }
    }

    private void validateAssignedTechnician(
            Booking booking
    ) {
        String loggedInUser =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        String assignedTechnician =
                booking.getTechnician()
                        .getAppUser().getUsername();

        if (!assignedTechnician.equals(
                loggedInUser
        )) {

            throw new GlobalExceptionHandler
                    .ConflictException(
                    "You are not assigned to this work order"
            );
        }
    }
}
