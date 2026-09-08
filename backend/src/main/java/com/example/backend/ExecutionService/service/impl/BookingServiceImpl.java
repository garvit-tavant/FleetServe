package com.example.backend.ExecutionService.service.impl;

import com.example.backend.ExecutionService.entity.Booking;
import com.example.backend.ExecutionService.repository.BookingRepository;
import com.example.backend.ExecutionService.service.BookingService;
import com.example.backend.ExecutionService.service.WorkOrderService;
import com.example.backend.SLA.entity.BreakdownRequest;
import com.example.backend.SLA.repository.BreakdownRequestRepository;
import com.example.backend.SLA.service.SlaCheckpointService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Booking is the pivot point that must keep several things in sync the
 * moment a job is scheduled:
 *
 * - Booking itself (this table).
 * - BreakdownRequest.status -> "BOOKED", but only for corrective bookings
 *   (kind = "CORRECTIVE" with a BreakdownRequest attached); preventive
 *   bookings (kind = "PREVENTIVE", linked to a maintenance plan instead)
 *   have no BreakdownRequest and skip every SLA-related step below.
 * - SlaCheckpoint.respondedAt, via SlaCheckpointService.recordResponse: the
 *   SLA "response" clock stops the instant a booking exists for the
 *   breakdown - not later when a technician starts the job (see the
 *   WorkOrderServiceImpl class javadoc for why those two events are kept
 *   deliberately distinct).
 * - WorkOrder, via WorkOrderService.createForBooking: every booking gets
 *   exactly one WorkOrder row (uk_work_order_booking), created here rather
 *   than left to whichever downstream code happens to remember to do it.
 *
 * All of this happens in one transaction: either a booking is fully wired up
 * (breakdown request updated, SLA checkpoint responded-to, work order
 * created), or none of it is - callers never see a booking that exists
 * without its work order, or a "responded" SLA clock without a real booking
 * behind it.
 */
@Service
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final BreakdownRequestRepository breakdownRequestRepository;
    private final WorkOrderService workOrderService;
    private final SlaCheckpointService slaCheckpointService;

    public BookingServiceImpl(BookingRepository bookingRepository,
                               BreakdownRequestRepository breakdownRequestRepository,
                               WorkOrderService workOrderService,
                               SlaCheckpointService slaCheckpointService) {
        this.bookingRepository = bookingRepository;
        this.breakdownRequestRepository = breakdownRequestRepository;
        this.workOrderService = workOrderService;
        this.slaCheckpointService = slaCheckpointService;
    }

    @Override
    @Transactional
    public void createBooking(Booking booking) {
        OffsetDateTime respondedAt = OffsetDateTime.now();

        BreakdownRequest breakdownRequest = booking.getBreakdownRequest();
        boolean isCorrective = "CORRECTIVE".equals(booking.getKind()) && breakdownRequest != null;

        if (isCorrective) {
            // Booking.breakdownRequest has no cascade, so the status change
            // needs its own explicit save - saving the Booking alone would
            // not persist this.
            breakdownRequest.setStatus("BOOKED");
            breakdownRequestRepository.save(breakdownRequest);
        }

        Booking saved = bookingRepository.save(booking);

        // SLA "response" clock stops here for EVERY booking, preventive or
        // corrective: the checkpoint is keyed on booking_id, so preventive
        // (maintenance-plan) jobs also get their clock and awaiting-parts
        // pauses tracked, and count towards MTTR. Only breach evaluation
        // needs a breakdown request, and SlaCalculator skips it when absent.
        slaCheckpointService.recordResponse(saved.getId(), respondedAt);

        // Every booking (preventive or corrective) gets exactly one
        // WorkOrder; createForBooking enforces uk_work_order_booking and
        // derives asset_id/status/total_cost/version/work_order_number
        // itself so callers never have to assemble a WorkOrder by hand.
        workOrderService.createForBooking(saved.getId());
    }

}
