package com.example.backend.ExecutionService.service.impl;

import com.example.backend.AssetManagamentService.exception.BusinessValidationException;
import com.example.backend.AssetManagamentService.exception.ResourceNotFoundException;
import com.example.backend.ExecutionService.dto.workorder.WorkOrderResponse;
import com.example.backend.ExecutionService.entity.Booking;
import com.example.backend.ExecutionService.entity.WorkOrder;
import com.example.backend.ExecutionService.mapper.WorkOrderMapper;
import com.example.backend.ExecutionService.repository.WorkOrderPartRequirementRepository;
import com.example.backend.ExecutionService.repository.WorkOrderRepository;
import com.example.backend.ExecutionService.service.WorkOrderResumeService;
import com.example.backend.ExecutionService.status.WorkOrderPartRequirementStatus;
import com.example.backend.ExecutionService.status.WorkOrderStatus;
import com.example.backend.ExecutionService.workflow.WorkOrderEvent;
import com.example.backend.ExecutionService.workflow.WorkOrderStateMachine;
import com.example.backend.SLA.service.SlaCheckpointService;
import com.example.backend.common.exception.GlobalExceptionHandler;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

@Service
public class WorkOrderResumeServiceImpl implements WorkOrderResumeService {

    private final WorkOrderRepository workOrderRepository;

    private final WorkOrderPartRequirementRepository
            requirementRepository;

    private final WorkOrderStateMachine
            workOrderStateMachine;

    private final SlaCheckpointService
            slaCheckpointService;

    private final WorkOrderMapper
            workOrderMapper;

    private final Clock
            clock;

    public WorkOrderResumeServiceImpl(WorkOrderRepository workOrderRepository, WorkOrderPartRequirementRepository requirementRepository, WorkOrderStateMachine workOrderStateMachine, SlaCheckpointService slaCheckpointService, WorkOrderMapper workOrderMapper, Clock clock) {
        this.workOrderRepository = workOrderRepository;
        this.requirementRepository = requirementRepository;
        this.workOrderStateMachine = workOrderStateMachine;
        this.slaCheckpointService = slaCheckpointService;
        this.workOrderMapper = workOrderMapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public WorkOrderResponse resumeWork(
            Long workOrderId
    ) {

        validateWorkOrderId(
                workOrderId
        );

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

        if (workOrder.getStatus()
                != WorkOrderStatus.AWAITING_PARTS) {

            throw new GlobalExceptionHandler
                    .ConflictException(
                    "Only AWAITING_PARTS work orders "
                            + "can be resumed"
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

        boolean pendingRequirementsExist =
                requirementRepository
                        .existsByWorkOrder_IdAndStatus(
                                workOrderId,
                                WorkOrderPartRequirementStatus.PENDING
                        );

        if (pendingRequirementsExist) {

            throw new GlobalExceptionHandler
                    .ConflictException(
                    "All pending part requirements "
                            + "must be resolved before "
                            + "resuming work"
            );
        }

        WorkOrderStatus nextStatus =
                workOrderStateMachine.nextState(
                        workOrder.getStatus(),
                        WorkOrderEvent.RESUME_WORK
                );

        workOrder.setStatus(
                nextStatus
        );

        slaCheckpointService.recordAwaitingPartsResolved(
                booking.getId(),
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
                        .getAppUser()
                        .getUsername();

        if (!assignedTechnician.equals(
                loggedInUser
        )) {

            throw new GlobalExceptionHandler
                    .ConflictException(
                    "You are not assigned to this work order"
            );
        }
    }

    private void validateWorkOrderId(
            Long workOrderId
    ) {

        if (workOrderId == null
                || workOrderId <= 0) {

            throw new BusinessValidationException(
                    "Work order ID must be a positive number"
            );
        }
    }
}
