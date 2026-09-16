package com.example.backend.ExecutionService.dto.workorder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class WorkOrderResponse {

    private Long id;

    private String workOrderNumber;

    private Long bookingId;

    private String status;

    private OffsetDateTime startedAt;

    private OffsetDateTime completedAt;

    private BigDecimal odometerAtService;

    private BigDecimal totalCost;

    // getters setters


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getWorkOrderNumber() {
        return workOrderNumber;
    }

    public void setWorkOrderNumber(String workOrderNumber) {
        this.workOrderNumber = workOrderNumber;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public BigDecimal getOdometerAtService() {
        return odometerAtService;
    }

    public void setOdometerAtService(BigDecimal odometerAtService) {
        this.odometerAtService = odometerAtService;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }
}