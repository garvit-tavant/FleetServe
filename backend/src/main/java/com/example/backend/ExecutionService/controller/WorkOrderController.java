package com.example.backend.ExecutionService.controller;

import com.example.backend.ExecutionService.dto.workorder.CompleteWorkOrderRequest;
import com.example.backend.ExecutionService.dto.workorder.CreatePartRequirementRequest;
import com.example.backend.ExecutionService.dto.workorder.IssueWorkOrderPartRequest;
import com.example.backend.ExecutionService.dto.workorder.WorkOrderDetailsResponse;
import com.example.backend.ExecutionService.dto.workorder.WorkOrderResponse;
import com.example.backend.ExecutionService.dto.workorderlabour.WorkOrderLabourCreateRequest;
import com.example.backend.ExecutionService.service.WorkOrderAwaitPartsService;
import com.example.backend.ExecutionService.service.WorkOrderIssuePartService;
import com.example.backend.ExecutionService.service.WorkOrderResumeService;
import com.example.backend.ExecutionService.service.WorkOrderService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController {

    private final WorkOrderService workOrderService;
    private final WorkOrderAwaitPartsService workOrderAwaitPartsService;
    private final WorkOrderIssuePartService workOrderIssuePartsService;
    private final WorkOrderResumeService workOrderResumeService;

    public WorkOrderController(
            WorkOrderService workOrderService,
            WorkOrderAwaitPartsService workOrderAwaitPartsService,
            WorkOrderIssuePartService workOrderIssuePartsService,
            WorkOrderResumeService workOrderResumeService) {

        this.workOrderService =
                workOrderService;
        this.workOrderAwaitPartsService = workOrderAwaitPartsService;
        this.workOrderIssuePartsService = workOrderIssuePartsService;
        this.workOrderResumeService = workOrderResumeService;
    }

    @GetMapping
    public ResponseEntity<List<WorkOrderResponse>> getAllWorkOrders() {

        return ResponseEntity.ok(
                workOrderService.getAllWorkOrders());
    }

    @GetMapping("/{workOrderId}")
    public ResponseEntity<WorkOrderDetailsResponse> getWorkOrder(
            @PathVariable Long workOrderId) {

        return ResponseEntity.ok(
                workOrderService.getWorkOrder(workOrderId));
    }

    @PostMapping("/{workOrderId}/start")
    public ResponseEntity<WorkOrderResponse> startWorkOrder(
            @PathVariable Long workOrderId) {

        WorkOrderResponse response =
                workOrderService.startWorkOrder(
                        workOrderId);

        return ResponseEntity.ok(
                response);
    }

    @PostMapping("/{workOrderId}/complete")
    public ResponseEntity<WorkOrderResponse> completeWorkOrder(
            @PathVariable Long workOrderId,
            @RequestBody @Valid CompleteWorkOrderRequest request,
            @RequestHeader("Idempotency-Key")
            String idempotencyKey) {

        WorkOrderResponse response =
                workOrderService.completeWorkOrder(
                        workOrderId,
                        request,
                        idempotencyKey);

        return ResponseEntity.ok(
                response);
    }

    @PostMapping("/{workOrderId}/await-parts")
    public WorkOrderResponse awaitParts(
            @PathVariable Long workOrderId,
            @RequestBody CreatePartRequirementRequest request
    ) {
        return workOrderAwaitPartsService.awaitParts(
                workOrderId,
                request
        );
    }

    @PostMapping("/{workOrderId}/parts/issue")
    public WorkOrderResponse issuePart(
            @PathVariable Long workOrderId,
            @RequestBody IssueWorkOrderPartRequest request
    ) {
        return workOrderIssuePartsService.issuePart(
                workOrderId,
                request
        );
    }

    @PostMapping("/{workOrderId}/resume")
    public WorkOrderResponse resumeWork(
            @PathVariable Long workOrderId
    ) {
        return workOrderResumeService.resumeWork(
                workOrderId
        );
    }

    @PostMapping("/{workOrderId}/labour")
    public ResponseEntity<WorkOrderDetailsResponse> addLabour(
            @PathVariable Long workOrderId,
            @RequestBody @Valid WorkOrderLabourCreateRequest request
    ) {
        WorkOrderDetailsResponse response =
                workOrderService.addLabour(workOrderId, request);

        return ResponseEntity.ok(response);
    }
}