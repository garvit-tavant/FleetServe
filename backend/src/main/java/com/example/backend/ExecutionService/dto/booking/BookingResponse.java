package com.example.backend.ExecutionService.dto.booking;

import java.time.OffsetDateTime;

public class BookingResponse {

    private Long id;

    private Long assetId;

    private Long workshopId;

    private Long bayId;

    private Long technicianId;

    private OffsetDateTime startAt;

    private OffsetDateTime endAt;

    private String kind;

    private Long maintenancePlanId;

    private Long breakdownRequestId;

    private String status;

    private Long workOrderId;

    private String workOrderNumber;

    private String workOrderStatus;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAssetId() {
        return assetId;
    }

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
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

    public OffsetDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(OffsetDateTime startAt) {
        this.startAt = startAt;
    }

    public OffsetDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(OffsetDateTime endAt) {
        this.endAt = endAt;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public Long getMaintenancePlanId() {
        return maintenancePlanId;
    }

    public void setMaintenancePlanId(Long maintenancePlanId) {
        this.maintenancePlanId = maintenancePlanId;
    }

    public Long getBreakdownRequestId() {
        return breakdownRequestId;
    }

    public void setBreakdownRequestId(Long breakdownRequestId) {
        this.breakdownRequestId = breakdownRequestId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getWorkOrderStatus() {
        return workOrderStatus;
    }

    public void setWorkOrderStatus(String workOrderStatus) {
        this.workOrderStatus = workOrderStatus;
    }
}
