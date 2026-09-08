package com.example.backend.ExecutionService.service;

import com.example.backend.ExecutionService.entity.WorkOrder;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface WorkOrderService {

    WorkOrder save(WorkOrder workOrder);

    Optional<WorkOrder> findById(Long id);

    /**
     * Creates the WorkOrder row for an existing Booking, filling in every
     * NOT NULL column that can be derived rather than requiring the caller to
     * supply them all (which is easy to get wrong given how many defaults-only
     * columns work_order has):
     *
     * - booking / asset_id: taken from the Booking (asset_id must match, per
     *   fk_work_order_asset, so it is never passed in separately).
     * - work_order_number: generated here (unique, uk_work_order_number).
     * - idempotency_key: generated here (unique, uk_work_order_idempotency_key);
     *   this is distinct from the Idempotency-Key header used on the
     *   completion endpoint (US-3.4) - that header is compared/stored
     *   separately when completion is implemented.
     * - status: SCHEDULED (matches column default, set explicitly to avoid
     *   relying on DB default in code that reads the entity back before a
     *   flush).
     * - total_cost: BigDecimal.ZERO (matches column default, ck_work_order_total_cost >= 0).
     * - version: 0L (matches column default, ck_work_order_version >= 0).
     * - started_at / completed_at / odometer_at_service: left null; enforced
     *   together only at COMPLETED by ck_work_order_completed_fields, which
     *   start()/complete() populate later.
     *
     * Throws if a work order already exists for this booking (mirrors
     * uk_work_order_booking) or if the booking does not exist.
     */
    WorkOrder createForBooking(Long bookingId);

    /**
     * Starts a work order: sets status to IN_PROGRESS and records startedAt.
     * Also synchronises the linked SlaCheckpoint.respondedAt (application-level
     * enforcement, no DB constraint) so the "time to respond" clock and the
     * work order's own timeline never disagree.
     */
    WorkOrder start(Long workOrderId);

    /**
     * Transitions a work order into AWAITING_PARTS (from IN_PROGRESS) and
     * opens the SLA "awaiting" pause for the linked breakdown request (via
     * SlaCheckpointService.recordAwaitingParts), so the resolution clock
     * stops counting elapsed time while blocked on parts/approval. No-op on
     * the SLA side for preventive bookings (no BreakdownRequest attached).
     */
    WorkOrder markAwaitingParts(Long workOrderId);

    /**
     * Transitions a work order out of AWAITING_PARTS back to IN_PROGRESS, and
     * closes the SLA "awaiting" pause (via
     * SlaCheckpointService.recordAwaitingPartsResolved), adding the elapsed
     * pause duration to the precomputed running total so the resolution clock
     * resumes counting from here. No-op on the SLA side for preventive
     * bookings.
     */
    WorkOrder resumeFromAwaitingParts(Long workOrderId);

  
    /**
     * Completes a work order, enforcing the Idempotency-Key contract (US-3.4,
     * INV-6): a replayed request with the same key returns the original
     * result without recomputing cost, writing duplicate movements, or
     * re-closing the maintenance plan cycle.
     *
     * Behaviour:
     * - If the work order is not yet COMPLETED: completes it normally and
     *   stores requestIdempotencyKey as the work order's idempotency_key
     *   (overwriting the creation-time placeholder set by createForBooking).
     * - If the work order is already COMPLETED and requestIdempotencyKey
     *   matches the stored key: this is a replay - returns the existing
     *   WorkOrder unchanged, no side effects re-run.
     * - If the work order is already COMPLETED and requestIdempotencyKey
     *   differs (or is null): throws IllegalStateException - this is a
     *   conflicting completion attempt, not a legitimate replay, and must not
     *   be silently accepted.
     */
    WorkOrder complete(Long workOrderId, OffsetDateTime completedAt, String requestIdempotencyKey);
}
