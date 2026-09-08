package com.example.backend.ExecutionService.service.impl;

import com.example.backend.ExecutionService.entity.Booking;
import com.example.backend.ExecutionService.entity.WorkOrder;
import com.example.backend.ExecutionService.repository.BookingRepository;
import com.example.backend.ExecutionService.repository.WorkOrderRepository;
import com.example.backend.ExecutionService.service.WorkOrderService;
import com.example.backend.SLA.entity.BreakdownRequest;
import com.example.backend.SLA.repository.SlaCheckpointRepository;
import com.example.backend.SLA.service.SlaCheckpointService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * WorkOrder and SlaCheckpoint independently store timestamps that represent
 * events in the corrective-repair lifecycle, but they are NOT the same event:
 *
 * - SlaCheckpoint.respondedAt = when the breakdown was acknowledged/scheduled
 *   (a booking was created for it). This is set by the booking-creation flow
 *   (US-2.2), not here - a work order starting is an operational event that
 *   happens later, after the ticket was already "responded to".
 * - SlaCheckpoint.resolvedAt = when the repair was completed. This
 *   legitimately corresponds to WorkOrder.completedAt, so WorkOrder remains
 *   the source of truth for that one field only, propagated one-way below.
 *
 * WorkOrder.start() therefore does not touch SlaCheckpoint at all.
 */
@Service
@Transactional(readOnly = true)
public class WorkOrderServiceImpl implements WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final SlaCheckpointRepository slaCheckpointRepository;
    private final BookingRepository bookingRepository;
    private final SlaCheckpointService slaCheckpointService;

    public WorkOrderServiceImpl(WorkOrderRepository workOrderRepository,
                                 SlaCheckpointRepository slaCheckpointRepository,
                                 BookingRepository bookingRepository,
                                 SlaCheckpointService slaCheckpointService) {
        this.workOrderRepository = workOrderRepository;
        this.slaCheckpointRepository = slaCheckpointRepository;
        this.bookingRepository = bookingRepository;
        this.slaCheckpointService = slaCheckpointService;
    }

    @Override
    @Transactional
    public WorkOrder save(WorkOrder workOrder) {
        return workOrderRepository.save(workOrder);
    }

    @Override
    public Optional<WorkOrder> findById(Long id) {
        return workOrderRepository.findById(id);
    }

    @Override
    @Transactional
    public WorkOrder createForBooking(Long bookingId) {
        if (workOrderRepository.existsByBooking_Id(bookingId)) {
            throw new IllegalStateException("Work order already exists for booking: " + bookingId);
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        WorkOrder workOrder = new WorkOrder();
        workOrder.setBooking(booking);
        workOrder.setAssetId(booking.getAssetId());
        workOrder.setStatus("SCHEDULED");
        workOrder.setTotalCost(BigDecimal.ZERO);
        workOrder.setVersion(0L);
        workOrder.setWorkOrderNumber(generateWorkOrderNumber());
        // idempotencyKey intentionally left null: it is the client-supplied
        // Idempotency-Key header from the completion request (US-3.4), which
        // does not exist yet at creation time. Now nullable at the DB level
        // (see V7__work_orders.sql), required only once status = COMPLETED.

        return workOrderRepository.save(workOrder);
    }

    @Override
    @Transactional
    public WorkOrder start(Long workOrderId) {
        WorkOrder workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Work order not found: " + workOrderId));

        OffsetDateTime now = OffsetDateTime.now();
        workOrder.setStatus("IN_PROGRESS");
        workOrder.setStartedAt(now);
        workOrderRepository.save(workOrder);

        return workOrder;
    }

    @Override
    @Transactional
    public WorkOrder markAwaitingParts(Long workOrderId) {
        WorkOrder workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Work order not found: " + workOrderId));

        workOrder.setStatus("AWAITING_PARTS");
        workOrderRepository.save(workOrder);

        slaCheckpointService.recordAwaitingParts(bookingIdFor(workOrder), OffsetDateTime.now());

        return workOrder;
    }

    @Override
    @Transactional
    public WorkOrder resumeFromAwaitingParts(Long workOrderId) {
        WorkOrder workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Work order not found: " + workOrderId));

        workOrder.setStatus("IN_PROGRESS");
        workOrderRepository.save(workOrder);

        slaCheckpointService.recordAwaitingPartsResolved(bookingIdFor(workOrder), OffsetDateTime.now());

        return workOrder;
    }

    /**
     * SlaCheckpoint is keyed on booking_id, and every work order has exactly
     * one booking - corrective or preventive - so awaiting-parts pauses apply
     * uniformly and no breakdown-request lookup is needed any more.
     */
    private static Long bookingIdFor(WorkOrder workOrder) {
        Booking booking = workOrder.getBooking();
        if (booking == null) {
            throw new IllegalStateException(
                    "Work order " + workOrder.getId() + " has no booking; cannot track its SLA clock.");
        }
        return booking.getId();
    }



    @Override
    @Transactional
    public WorkOrder complete(Long workOrderId, OffsetDateTime completedAt, String requestIdempotencyKey) {
        return completeInternal(workOrderId, completedAt, requestIdempotencyKey, true);
    }

    /**
     * Shared completion logic. When enforceIdempotency is true (the HTTP path):
     * - an already-COMPLETED work order whose stored idempotencyKey matches
     *   requestIdempotencyKey is a replay: return the existing row untouched,
     *   no re-save, no SlaCheckpoint re-propagation, no duplicate side effects.
     * - an already-COMPLETED work order whose key does NOT match is a
     *   conflicting request: reject rather than silently redoing anything.
     * When false (internal/test overload), no comparison is performed and the
     * work order is simply (re)completed - callers accept that responsibility.
     */
    private WorkOrder completeInternal(Long workOrderId, OffsetDateTime completedAt,
                                        String requestIdempotencyKey, boolean enforceIdempotency) {
        WorkOrder workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Work order not found: " + workOrderId));

        if (enforceIdempotency && "COMPLETED".equals(workOrder.getStatus())) {
            boolean isReplay = requestIdempotencyKey != null
                    && requestIdempotencyKey.equals(workOrder.getIdempotencyKey());
            if (isReplay) {
                return workOrder; // replay: return original result, no side effects re-run
            }
            throw new IllegalStateException(
                    "Work order " + workOrderId + " is already completed under a different Idempotency-Key");
        }

        OffsetDateTime when = completedAt != null ? completedAt : OffsetDateTime.now();
        workOrder.setStatus("COMPLETED");
        workOrder.setCompletedAt(when);
        if (requestIdempotencyKey != null) {
            // Overwrites the creation-time placeholder (see createForBooking) with
            // the key that actually completed this work order, so future replays
            // can be recognised.
            workOrder.setIdempotencyKey(requestIdempotencyKey);
        }
        workOrderRepository.save(workOrder);

        propagateResolvedAt(workOrder.getBooking().getId(), when);

        return workOrder;
    }

    /**
     * One-way sync: WorkOrder.completedAt -> SlaCheckpoint.resolvedAt.
     * Unconditionally overwrites the checkpoint (if one exists for a corrective
     * booking, i.e. was created earlier when the booking was scheduled); the
     * checkpoint never dictates the work order's value.
     */
    private void propagateResolvedAt(Long bookingId, OffsetDateTime completedAt) {
        slaCheckpointRepository.findByBookingId(bookingId).ifPresent(checkpoint -> {
            checkpoint.setResolvedAt(completedAt);
            slaCheckpointRepository.save(checkpoint);
        });
    }

    /**
     * Generates a unique-enough work_order_number. Not sequential/human
     * friendly; if a readable format ("WO-2026-000123") is needed later,
     * replace this with a DB sequence and format it here instead.
     */
    private static String generateWorkOrderNumber() {
        return "WO-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }

}
