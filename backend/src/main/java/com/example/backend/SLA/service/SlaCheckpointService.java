package com.example.backend.SLA.service;

import java.time.OffsetDateTime;

/**
 * Owns creation/updates of SlaCheckpoint rows for lifecycle events that are
 * NOT already covered by WorkOrderServiceImpl (which owns
 * SlaCheckpoint.resolvedAt, propagated one-way from WorkOrder.completedAt).
 *
 * Everything here is keyed on booking_id: a checkpoint exists for EVERY
 * booking, corrective (breakdown-driven) or preventive (maintenance-plan
 * driven), so awaiting-parts pauses and MTTR are measured uniformly. Only
 * breach evaluation needs a breakdown request, and that is handled downstream
 * in SlaCalculator.
 */
public interface SlaCheckpointService {

    /**
     * Records that the job has been responded to - i.e. a Booking was scheduled
     * for it. Creates the SlaCheckpoint row if absent (the normal case, since
     * nothing else creates it), otherwise overwrites respondedAt. Call from the
     * booking-creation flow (US-2.2) once the Booking is persisted.
     */
    void recordResponse(Long bookingId, OffsetDateTime respondedAt);

    /**
     * Records that the job has been resolved (repair completed).
     */
    void recordResolution(Long bookingId, OffsetDateTime resolvedAt);

    /**
     * Opens an awaiting-parts pause on the SLA clock. Throws if a pause is
     * already open for this booking.
     */
    void recordAwaitingParts(Long bookingId, OffsetDateTime awaitingPartsAt);

    /**
     * Closes the open awaiting-parts pause, folding its duration into the
     * checkpoint's accumulated awaiting minutes. Throws if no pause is open.
     */
    void recordAwaitingPartsResolved(Long bookingId, OffsetDateTime awaitingPartsResolvedAt);
}
