package com.example.backend.ExecutionService.dto;

import java.time.OffsetDateTime;

/**
 * Row for US-4.2 MTTR aggregation (SlaService.MeanTimeToRepair()).
 * Populated via a JPQL constructor-expression query
 * (WorkOrderRepository.findCompletedWork()) that joins Asset explicitly,
 * since WorkOrder.assetId is a raw column (no @ManyToOne association).
 */
public class CompletedWork {

    private final Long bookingId;
    private final Long workshopId;
    private final Long assetClassId;
    private final String assetClassCode;
    private final OffsetDateTime startedAt;
    private final OffsetDateTime completedAt;

    public CompletedWork(Long bookingId, Long workshopId, Long assetClassId, String assetClassCode,
                          OffsetDateTime startedAt, OffsetDateTime completedAt) {
        this.bookingId = bookingId;
        this.workshopId = workshopId;
        this.assetClassId = assetClassId;
        this.assetClassCode = assetClassCode;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public Long getWorkshopId() {
        return workshopId;
    }

    public Long getAssetClassId() {
        return assetClassId;
    }

    public String getAssetClassCode() {
        return assetClassCode;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }
}

