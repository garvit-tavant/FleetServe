package com.example.backend.SLA.dto;

/**
 * US-4.2 result row: mean time to repair grouped by asset class.
 * Populated by Java-side aggregation in SlaService.MeanTimeToRepair() over
 * CompletedWork rows, reusing SlaTimeCalculationStrategy (WORKING_TIME) so
 * this figure stays consistent with SlaCalculator's live breach detection.
 */
public class MeanTimeToRepairReport {
    private final Long assetClassId;
    private final String assetClassCode;
    private final Double meanTimeToRepairMinutes;
    private final Long sampleSize;

    public MeanTimeToRepairReport(Long assetClassId, String assetClassCode,
                                   Double meanTimeToRepairMinutes, Long sampleSize) {
        this.assetClassId = assetClassId;
        this.assetClassCode = assetClassCode;
        this.meanTimeToRepairMinutes = meanTimeToRepairMinutes;
        this.sampleSize = sampleSize;
    }

    public Long getAssetClassId() {
        return assetClassId;
    }

    public String getAssetClassCode() {
        return assetClassCode;
    }

    public Double getMeanTimeToRepairMinutes() {
        return meanTimeToRepairMinutes;
    }

    public Long getSampleSize() {
        return sampleSize;
    }
}
