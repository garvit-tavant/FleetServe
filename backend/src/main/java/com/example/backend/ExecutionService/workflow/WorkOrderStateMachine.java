package com.example.backend.ExecutionService.workflow;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.backend.AssetManagamentService.exception.BusinessValidationException;
import com.example.backend.ExecutionService.status.WorkOrderStatus;

@Component
public class WorkOrderStateMachine {

    private static final Map<StateTransition, WorkOrderStatus>
            VALID_TRANSITIONS =
            Map.of(

                    new StateTransition(
                            WorkOrderStatus.SCHEDULED,
                            WorkOrderEvent.START_WORK),
                    WorkOrderStatus.IN_PROGRESS,

                    new StateTransition(
                            WorkOrderStatus.SCHEDULED,
                            WorkOrderEvent.CANCEL),
                    WorkOrderStatus.CANCELLED,

                    new StateTransition(
                            WorkOrderStatus.IN_PROGRESS,
                            WorkOrderEvent.WAIT_FOR_PARTS),
                    WorkOrderStatus.AWAITING_PARTS,

                    new StateTransition(
                            WorkOrderStatus.IN_PROGRESS,
                            WorkOrderEvent.COMPLETE_WORK),
                    WorkOrderStatus.COMPLETED,

                    new StateTransition(
                            WorkOrderStatus.IN_PROGRESS,
                            WorkOrderEvent.CANCEL),
                    WorkOrderStatus.CANCELLED,

                    new StateTransition(
                            WorkOrderStatus.AWAITING_PARTS,
                            WorkOrderEvent.RESUME_WORK),
                    WorkOrderStatus.IN_PROGRESS,

                    new StateTransition(
                            WorkOrderStatus.AWAITING_PARTS,
                            WorkOrderEvent.CANCEL),
                    WorkOrderStatus.CANCELLED
            );

    public static WorkOrderStatus nextState(
            WorkOrderStatus currentState,
            WorkOrderEvent event
    ) {

        WorkOrderStatus nextState =
                VALID_TRANSITIONS.get(
                        new StateTransition(
                                currentState,
                                event
                        )
                );

        if (nextState == null) {

            throw new BusinessValidationException(
                    "Invalid work-order transition: "
                            + currentState
                            + " -> "
                            + event
            );
        }

        return nextState;
    }
}