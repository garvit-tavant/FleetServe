package com.example.backend.ExecutionService.dto.booking;

import java.time.OffsetDateTime;

public class BookingCancellationResponse {

    private Long bookingId;

    private String bookingStatus;

    private Long workOrderId;

    private String workOrderStatus;

    private String bookingKind;

    private String cancellationReason;

    private OffsetDateTime cancelledAt;

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(
            Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(
            String bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    public Long getWorkOrderId() {
        return workOrderId;
    }

    public void setWorkOrderId(
            Long workOrderId) {
        this.workOrderId = workOrderId;
    }

    public String getWorkOrderStatus() {
        return workOrderStatus;
    }

    public void setWorkOrderStatus(
            String workOrderStatus) {
        this.workOrderStatus = workOrderStatus;
    }

    public String getBookingKind() {
        return bookingKind;
    }

    public void setBookingKind(
            String bookingKind) {
        this.bookingKind = bookingKind;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(
            String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public OffsetDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(
            OffsetDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }
}