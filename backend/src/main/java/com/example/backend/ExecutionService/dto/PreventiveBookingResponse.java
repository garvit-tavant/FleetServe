package com.example.backend.ExecutionService.dto;

import java.time.OffsetDateTime;

public class PreventiveBookingResponse {

    private Long bookingId;
    private Long workOrderId;
    private String workOrderNumber;

    private Long assetId;
    private Long maintenancePlanId;
    private Long workshopId;
    private Long bayId;
    private Long technicianId;

    private OffsetDateTime start;
    private OffsetDateTime end;

    private String bookingKind;
    private String bookingStatus;
    private String workOrderStatus;

    public PreventiveBookingResponse() {
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Long getWorkOrderId() {
        return workOrderId;
    }

    public void setWorkOrderId(Long workOrderId) {
        this.workOrderId = workOrderId;
    }

    public String getWorkOrderNumber() {
        return workOrderNumber;
    }

    public void setWorkOrderNumber(String workOrderNumber) {
        this.workOrderNumber = workOrderNumber;
    }

    public Long getAssetId() {
        return assetId;
    }

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }

    public Long getMaintenancePlanId() {
        return maintenancePlanId;
    }

    public void setMaintenancePlanId(Long maintenancePlanId) {
        this.maintenancePlanId = maintenancePlanId;
    }

    public Long getWorkshopId() {
        return workshopId;
    }

    public void setWorkshopId(Long workshopId) {
        this.workshopId = workshopId;
    }

    public Long getBayId() {
        return bayId;
    }

    public void setBayId(Long bayId) {
        this.bayId = bayId;
    }

    public Long getTechnicianId() {
        return technicianId;
    }

    public void setTechnicianId(Long technicianId) {
        this.technicianId = technicianId;
    }

    public OffsetDateTime getStart() {
        return start;
    }

    public void setStart(OffsetDateTime start) {
        this.start = start;
    }

    public OffsetDateTime getEnd() {
        return end;
    }

    public void setEnd(OffsetDateTime end) {
        this.end = end;
    }

    public String getBookingKind() {
        return bookingKind;
    }

    public void setBookingKind(String bookingKind) {
        this.bookingKind = bookingKind;
    }

    public String getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(String bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    public String getWorkOrderStatus() {
        return workOrderStatus;
    }

    public void setWorkOrderStatus(String workOrderStatus) {
        this.workOrderStatus = workOrderStatus;
    }
}