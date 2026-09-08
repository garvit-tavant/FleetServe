package com.example.backend.SLA.dto;

/**
 * The state of both service-level clocks for one breakdown at a point in time.
 *
 * <p>Feeds the "soon to breach" view in US-4.1. Remaining minutes are negative
 * once a target has been exceeded.
 */
public record SlaClockSnapshot(
        Long breakdownId,
        com.example.backend.SLA.status.BreakdownPriority priority,
        String calendarBasis,
        long responseElapsedMinutes,
        int responseTargetMinutes,
        long responseRemainingMinutes,
        SlaClockStatus responseStatus,
        boolean responseClockStopped,
        long resolutionElapsedMinutes,
        int resolutionTargetMinutes,
        long resolutionRemainingMinutes,
        SlaClockStatus resolutionStatus,
        boolean resolutionClockStopped,
        long pausedMinutes) {

    public boolean responseBreached() {
        return responseStatus == SlaClockStatus.BREACHED;
    }

    public boolean resolutionBreached() {
        return resolutionStatus == SlaClockStatus.BREACHED;
    }
}
