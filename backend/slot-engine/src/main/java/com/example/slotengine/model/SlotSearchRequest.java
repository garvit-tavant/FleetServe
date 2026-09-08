package com.example.slotengine.model;

import java.util.List;
import java.util.Objects;

/**
 * The complete input to a slot search.
 *
 * <p>Everything the engine needs is passed in, which is what keeps the engine a
 * pure function: no repository, no clock, no I/O.
 *
 * <p>{@code requiredCapability} or {@code requiredSkill} may be null, meaning
 * the job places no constraint on that axis.
 */
public record SlotSearchRequest(
        int durationMinutes,
        String requiredSkill,
        String requiredCapability,
        List<BayCandidate> candidateBays,
        List<TechnicianCandidate> candidateTechnicians,
        WorkingCalendar workingCalendar,
        List<ExistingBooking> existingBookings,
        SearchHorizon searchHorizon,
        int maxResults) {

    public SlotSearchRequest {
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException(
                    "durationMinutes must be positive: " + durationMinutes);
        }
        if (maxResults < 0) {
            throw new IllegalArgumentException(
                    "maxResults must not be negative: " + maxResults);
        }
        Objects.requireNonNull(workingCalendar, "workingCalendar");
        Objects.requireNonNull(searchHorizon, "searchHorizon");

        candidateBays = candidateBays == null ? List.of() : List.copyOf(candidateBays);
        candidateTechnicians =
                candidateTechnicians == null ? List.of() : List.copyOf(candidateTechnicians);
        existingBookings = existingBookings == null ? List.of() : List.copyOf(existingBookings);
    }
}
