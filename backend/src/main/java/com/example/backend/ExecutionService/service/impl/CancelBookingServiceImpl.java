package com.example.backend.ExecutionService.service.impl;

import java.time.Clock;
import java.time.OffsetDateTime;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.AssetManagamentService.exception.BusinessValidationException;
import com.example.backend.AssetManagamentService.exception.ResourceNotFoundException;
import com.example.backend.ExecutionService.dto.booking.BookingCancellationResponse;
import com.example.backend.ExecutionService.dto.booking.CancelBookingRequest;
import com.example.backend.ExecutionService.entity.Booking;
import com.example.backend.ExecutionService.entity.BookingHistory;
import com.example.backend.ExecutionService.entity.WorkOrder;
import com.example.backend.ExecutionService.repository.BookingHistoryRepository;
import com.example.backend.ExecutionService.repository.BookingRepository;
import com.example.backend.ExecutionService.repository.WorkOrderRepository;
import com.example.backend.ExecutionService.service.CancelBookingService;
import com.example.backend.ExecutionService.status.BookingKind;
import com.example.backend.ExecutionService.status.BookingStatus;
import com.example.backend.ExecutionService.status.WorkOrderStatus;
import com.example.backend.ExecutionService.workflow.BreakdownEvent;
import com.example.backend.ExecutionService.workflow.BreakdownRequestStateMachine;
import com.example.backend.ExecutionService.workflow.WorkOrderEvent;
import com.example.backend.ExecutionService.workflow.WorkOrderStateMachine;
import com.example.backend.SLA.entity.BreakdownRequest;
import com.example.backend.SLA.repository.BreakdownRequestRepository;
import com.example.backend.SecurityService.entity.AppUser;
import com.example.backend.SecurityService.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class CancelBookingServiceImpl
        implements CancelBookingService {

    private static final int MAX_REASON_LENGTH = 1000;

    private static final String CANCELLATION_ACTION =
            "CANCELLED";

    private final BookingRepository bookingRepository;
    private final WorkOrderRepository workOrderRepository;
    private final BookingHistoryRepository bookingHistoryRepository;
    private final BreakdownRequestRepository breakdownRequestRepository;
    private final UserRepository userRepository;
    private final WorkOrderStateMachine workOrderStateMachine;
    private final BreakdownRequestStateMachine breakdownRequestStateMachine;
    private final Clock clock;

    public CancelBookingServiceImpl(
            BookingRepository bookingRepository,
            WorkOrderRepository workOrderRepository,
            BookingHistoryRepository bookingHistoryRepository,
            BreakdownRequestRepository breakdownRequestRepository,
            UserRepository userRepository,
            WorkOrderStateMachine workOrderStateMachine,
            BreakdownRequestStateMachine breakdownRequestStateMachine,
            Clock clock) {

        this.bookingRepository =
                bookingRepository;

        this.workOrderRepository =
                workOrderRepository;

        this.bookingHistoryRepository =
                bookingHistoryRepository;

        this.breakdownRequestRepository =
                breakdownRequestRepository;

        this.userRepository =
                userRepository;

        this.workOrderStateMachine =
                workOrderStateMachine;

        this.breakdownRequestStateMachine =
                breakdownRequestStateMachine;

        this.clock =
                clock;
    }

    @Override
    @Transactional
    public BookingCancellationResponse cancelBooking(
            Long bookingId,
            CancelBookingRequest request) {

        validateRequest(
                bookingId,
                request);

        Booking booking =
                bookingRepository
                        .findByIdForCancellation(bookingId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Booking not found with id: "
                                                + bookingId));

        validateBookingCanBeCancelled(
                booking);

        WorkOrder workOrder =
                getRequiredWorkOrder(
                        booking);

        validateWorkOrderCanBeCancelled(
                workOrder);

        AppUser actor =
                getAuthenticatedActiveUser();

        String cancellationReason =
                request.getReason().trim();

        OffsetDateTime cancelledAt =
                OffsetDateTime.now(clock);

        /*
         * Validate state transitions before changing any entity.
         */
        WorkOrderStatus nextWorkOrderStatus =
                workOrderStateMachine.nextState(
                        workOrder.getStatus(),
                        WorkOrderEvent.CANCEL);

        /*
         * Capture the booking's slot before changing its state.
         * BookingHistory now stores timestamps rather than Range.
         */
        OffsetDateTime previousStartAt =
                booking.getStartAt();

        OffsetDateTime previousEndAt =
                booking.getEndAt();

        booking.setStatus(
                BookingStatus.CANCELLED);

        workOrder.setStatus(
                nextWorkOrderStatus);

        /*
         * If this is a corrective booking, synchronize the
         * associated BreakdownRequest:
         *
         * BOOKED + CANCEL -> CANCELLED
         */
        if (booking.getKind()
                == BookingKind.CORRECTIVE) {

            cancelCorrectiveBreakdown(
                    booking);
        }

        Booking savedBooking =
                bookingRepository.save(
                        booking);

        WorkOrder savedWorkOrder =
                workOrderRepository.save(
                        workOrder);

        BookingHistory bookingHistory =
                buildCancellationHistory(
                        savedBooking,
                        actor,
                        cancellationReason,
                        cancelledAt,
                        previousStartAt,
                        previousEndAt);

        BookingHistory savedHistory =
                bookingHistoryRepository.save(
                        bookingHistory);

        return buildCancellationResponse(
                savedBooking,
                savedWorkOrder,
                savedHistory);
    }

    private void validateRequest(
            Long bookingId,
            CancelBookingRequest request) {

        if (bookingId == null) {
            throw new IllegalArgumentException(
                    "Booking ID is required");
        }

        if (bookingId <= 0L) {
            throw new IllegalArgumentException(
                    "Booking ID must be greater than zero");
        }

        if (request == null) {
            throw new IllegalArgumentException(
                    "Cancellation request is required");
        }

        if (request.getReason() == null
                || request.getReason().isBlank()) {

            throw new BusinessValidationException(
                    "Cancellation reason is required");
        }

        String trimmedReason =
                request.getReason().trim();

        if (trimmedReason.length()
                > MAX_REASON_LENGTH) {

            throw new BusinessValidationException(
                    "Cancellation reason cannot exceed "
                            + MAX_REASON_LENGTH
                            + " characters");
        }
    }

    private void validateBookingCanBeCancelled(
            Booking booking) {

        if (booking.getStatus() == null) {
            throw new BusinessValidationException(
                    "Booking "
                            + booking.getId()
                            + " has no status");
        }

        if (booking.getStatus()
                == BookingStatus.CANCELLED) {

            throw new BusinessValidationException(
                    "Booking "
                            + booking.getId()
                            + " is already cancelled");
        }

        if (booking.getStatus()
                == BookingStatus.COMPLETED) {

            throw new BusinessValidationException(
                    "A completed booking cannot be cancelled");
        }

        if (booking.getStatus()
                != BookingStatus.CONFIRMED) {

            throw new BusinessValidationException(
                    "Only a CONFIRMED booking can be cancelled. "
                            + "Current status is "
                            + booking.getStatus());
        }

        if (booking.getStartAt() == null||booking.getEndAt()==null) {

            throw new BusinessValidationException(
                    "Booking "
                            + booking.getId()
                            + " has an incomplete slot");
        }

        OffsetDateTime start =
                booking.getStartAt();

        OffsetDateTime end =
                booking.getEndAt();

        if (start==null||end==null||!end.isAfter(start)) {

            throw new BusinessValidationException(
                    "Booking "
                            + booking.getId()
                            + " has an invalid slot interval");
        }

        if (booking.getKind() == null) {
            throw new BusinessValidationException(
                    "Booking "
                            + booking.getId()
                            + " has no booking kind");
        }
    }

    private WorkOrder getRequiredWorkOrder(
            Booking booking) {

        if (booking.getWorkOrder() != null) {
            return booking.getWorkOrder();
        }

        return workOrderRepository
                .findByBooking_Id(
                        booking.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Work order not found for booking "
                                        + booking.getId()));
    }

    private void validateWorkOrderCanBeCancelled(
            WorkOrder workOrder) {

        if (workOrder.getStatus() == null) {
            throw new BusinessValidationException(
                    "Work order "
                            + workOrder.getId()
                            + " has no status");
        }

        if (workOrder.getStatus()
                == WorkOrderStatus.CANCELLED) {

            throw new BusinessValidationException(
                    "Work order "
                            + workOrder.getId()
                            + " is already cancelled");
        }

        if (workOrder.getStatus()
                == WorkOrderStatus.COMPLETED) {

            throw new BusinessValidationException(
                    "The booking cannot be cancelled because "
                            + "its work order is completed");
        }

        if (workOrder.getStatus()
                == WorkOrderStatus.IN_PROGRESS) {

            throw new BusinessValidationException(
                    "The booking cannot be cancelled because "
                            + "work has already started. Use the "
                            + "work-order cancellation workflow instead.");
        }

        if (workOrder.getStatus()
                == WorkOrderStatus.AWAITING_PARTS) {

            throw new BusinessValidationException(
                    "The booking cannot be cancelled because "
                            + "its work order is awaiting parts. Use the "
                            + "work-order cancellation workflow instead.");
        }

        if (workOrder.getStatus()
                != WorkOrderStatus.SCHEDULED) {

            throw new BusinessValidationException(
                    "Only a booking with a SCHEDULED work order "
                            + "can be cancelled. Current work-order status is "
                            + workOrder.getStatus());
        }
    }

    private void cancelCorrectiveBreakdown(
            Booking booking) {

        BreakdownRequest breakdownRequest =
                booking.getBreakdownRequest();

        if (breakdownRequest == null) {
            throw new BusinessValidationException(
                    "Corrective booking "
                            + booking.getId()
                            + " does not contain a breakdown request");
        }

        /*
         * Corrective booking creation should have moved the
         * breakdown from REPORTED to BOOKED.
         */
        com.example.backend.SLA.dto.BreakdownStatus nextStatus =
                breakdownRequestStateMachine.nextState(
                        breakdownRequest.getStatus(),
                        BreakdownEvent.CANCEL);

        breakdownRequest.setStatus(
                nextStatus);

        /*
         * Booking is the owning side of the relationship.
         * Do not remove the booking link during cancellation.
         * The project rule says a cancelled breakdown cannot be
         * booked again.
         */
        breakdownRequestRepository.save(
                breakdownRequest);
    }

    private BookingHistory buildCancellationHistory(
            Booking booking,
            AppUser actor,
            String cancellationReason,
            OffsetDateTime cancelledAt,
            OffsetDateTime previousStartAt,
            OffsetDateTime previousEndAt) {

        BookingHistory history =
                new BookingHistory();

        history.setBooking(
                booking);

        history.setAction(
                CANCELLATION_ACTION);

        /*
         * Cancellation does not change the historical slot.
         *
         * previous_* records the slot released by cancellation.
         * new_* is null because no replacement slot exists.
         */
        history.setPreviousStartAt(previousStartAt);
        history.setPreviousEndAt(previousEndAt);

        history.setNewStartAt(null);
        history.setNewEndAt(null);

        history.setActor(
                actor);

        history.setReason(
                cancellationReason);

        history.setOccurredAt(
                cancelledAt);

        return history;
    }

    private AppUser getAuthenticatedActiveUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication
                instanceof AnonymousAuthenticationToken) {

            throw new BusinessValidationException(
                    "An authenticated user is required "
                            + "to cancel a booking");
        }

        String username =
                authentication.getName();

        if (username == null
                || username.isBlank()) {

            throw new BusinessValidationException(
                    "Authenticated username is unavailable");
        }

        AppUser appUser =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Authenticated user was not found: "
                                                + username));

        if (!Boolean.TRUE.equals(
                appUser.getIsActive())) {

            throw new BusinessValidationException(
                    "Inactive users cannot cancel bookings");
        }

        return appUser;
    }

    private BookingCancellationResponse
    buildCancellationResponse(
            Booking booking,
            WorkOrder workOrder,
            BookingHistory bookingHistory) {

        BookingCancellationResponse response =
                new BookingCancellationResponse();

        response.setBookingId(
                booking.getId());

        response.setBookingStatus(
                booking.getStatus().name());

        response.setWorkOrderId(
                workOrder.getId());

        response.setWorkOrderStatus(
                workOrder.getStatus().name());

        response.setBookingKind(
                booking.getKind().name());

        response.setCancellationReason(
                bookingHistory.getReason());

        response.setCancelledAt(
                bookingHistory.getOccurredAt());

        return response;
    }
}