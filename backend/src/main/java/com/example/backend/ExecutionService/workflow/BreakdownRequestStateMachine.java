package com.example.backend.ExecutionService.workflow;

import java.util.Map;

import com.example.backend.SLA.dto.BreakdownStatus;
import org.springframework.stereotype.Component;

import com.example.backend.AssetManagamentService.exception.BusinessValidationException;

@Component
public class BreakdownRequestStateMachine {

    private static final Map<BreakdownTransition, BreakdownStatus>
            VALID_TRANSITIONS =
            Map.of(

                    new BreakdownTransition(
                            BreakdownStatus.REPORTED,
                            BreakdownEvent.BOOK_BREAKDOWN),
                    BreakdownStatus.BOOKED,

                    new BreakdownTransition(
                            BreakdownStatus.REPORTED,
                            BreakdownEvent.CANCEL),
                    BreakdownStatus.CANCELLED,

                    new BreakdownTransition(
                            BreakdownStatus.BOOKED,
                            BreakdownEvent.RESOLVE),
                    BreakdownStatus.RESOLVED,

                    new BreakdownTransition(
                            BreakdownStatus.BOOKED,
                            BreakdownEvent.CANCEL),
                    BreakdownStatus.CANCELLED
            );

    public BreakdownStatus nextState(
            BreakdownStatus currentState,
            BreakdownEvent event
    ) {

        BreakdownStatus nextState =
                VALID_TRANSITIONS.get(
                        new BreakdownTransition(
                                currentState,
                                event
                        )
                );

        if (nextState == null) {
            throw new BusinessValidationException(
                    "Invalid breakdown-request transition: "
                            + currentState
                            + " -> "
                            + event
            );
        }

        return nextState;
    }
}