package com.example.backend.SLA.service.impl;

import com.example.backend.ExecutionService.entity.Booking;
import com.example.backend.ExecutionService.repository.BookingRepository;
import com.example.backend.SLA.entity.SlaCheckpoint;
import com.example.backend.SLA.repository.SlaCheckpointRepository;
import com.example.backend.SLA.service.SlaCheckpointService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Booking-keyed SLA checkpoint maintenance.
 *
 * A checkpoint is created for EVERY booking, corrective or preventive, so the
 * awaiting-parts pause mechanism and MTTR work identically for maintenance-plan
 * jobs. Breach evaluation still only applies to corrective jobs - SlaCalculator
 * short-circuits when the booking has no breakdown request.
 */
@Service
@Transactional(readOnly = true)
public class SlaCheckpointServiceImpl implements SlaCheckpointService {

    private final SlaCheckpointRepository slaCheckpointRepository;
    private final BookingRepository bookingRepository;
    private final SlaCalculator slaCalculator;

    public SlaCheckpointServiceImpl(SlaCheckpointRepository slaCheckpointRepository,
                                    BookingRepository bookingRepository,
                                    SlaCalculator slaCalculator) {
        this.slaCheckpointRepository = slaCheckpointRepository;
        this.bookingRepository = bookingRepository;
        this.slaCalculator = slaCalculator;
    }

    @Override
    @Transactional
    public void recordResponse(Long bookingId, OffsetDateTime respondedAt) {
        // SlaCheckpoint uses @MapsId, so the shared PK is derived from the
        // Booking association - setting a raw id would not populate it.
        SlaCheckpoint checkpoint = slaCheckpointRepository.findById(bookingId)
                .orElseGet(() -> {
                    Booking booking = bookingRepository.findById(bookingId)
                            .orElseThrow(() -> new IllegalStateException("Booking not found: " + bookingId));
                    SlaCheckpoint created = new SlaCheckpoint();
                    created.setBooking(booking);
                    return created;
                });

        checkpoint.setRespondedAt(respondedAt);
        slaCheckpointRepository.save(checkpoint);
    }

    @Override
    @Transactional
    public void recordResolution(Long bookingId, OffsetDateTime resolvedAt) {
        SlaCheckpoint checkpoint = slaCheckpointRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalStateException("SlaCheckpoint not found for booking: " + bookingId));

        checkpoint.setResolvedAt(resolvedAt);
        slaCheckpointRepository.save(checkpoint);

        // Now that the job is closed, settle the response-breach flag. No-op
        // for preventive bookings (no target to breach).
        slaCalculator.isResponseBreach(bookingId);
    }

    @Override
    @Transactional
    public void recordAwaitingParts(Long bookingId, OffsetDateTime awaitingPartsAt) {
        slaCalculator.raiseAwaiting(bookingId, awaitingPartsAt);
    }

    @Override
    @Transactional
    public void recordAwaitingPartsResolved(Long bookingId, OffsetDateTime awaitingPartsResolvedAt) {
        // Workshop is resolved from the booking inside the calculator, so this
        // works for preventive bookings too.
        slaCalculator.resolveAwaiting(bookingId, awaitingPartsResolvedAt);
    }
}
