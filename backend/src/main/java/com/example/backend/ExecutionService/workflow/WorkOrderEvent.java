package com.example.backend.ExecutionService.workflow;

public enum WorkOrderEvent {

    START_WORK,

    WAIT_FOR_PARTS,

    RESUME_WORK,

    COMPLETE_WORK,

    CANCEL
}