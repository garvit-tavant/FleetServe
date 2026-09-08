package com.example.slotengine.model;

import java.util.List;
import java.util.Objects;

public record SlotSearchRequest(
        int durationMinutes,
        String requiredSkill,
        String requiredCapability,
        List<BayCandidate> candidateBays,
        List<TechnicianCandidate> candidateTechnicians,
        WorkingCalendar workingCalendar,
        List<ExistingBooking> existingBookings,
        SearchHorizon searchHorizon,
        int maxResults
) {

    public SlotSearchRequest {

        if (durationMinutes <= 0) {
            throw new IllegalArgumentException(
                    "durationMinutes must be positive: "
                            + durationMinutes
            );
        }

        /*
         * FleetServe rule:
         *
         * All durations must be multiples
         * of 15 minutes.
         */
        if (durationMinutes % 15 != 0) {
            throw new IllegalArgumentException(
                    "durationMinutes must be a multiple of 15"
            );
        }

        if (maxResults < 0) {
            throw new IllegalArgumentException(
                    "maxResults must not be negative: "
                            + maxResults
            );
        }

        Objects.requireNonNull(
                workingCalendar,
                "workingCalendar"
        );

        Objects.requireNonNull(
                searchHorizon,
                "searchHorizon"
        );

        candidateBays =
                candidateBays == null
                        ? List.of()
                        : List.copyOf(candidateBays);

        candidateTechnicians =
                candidateTechnicians == null
                        ? List.of()
                        : List.copyOf(candidateTechnicians);

        existingBookings =
                existingBookings == null
                        ? List.of()
                        : List.copyOf(existingBookings);

        validateUniqueBayIds(
                candidateBays
        );

        validateUniqueTechnicianIds(
                candidateTechnicians
        );
    }

    private static void validateUniqueBayIds(
            List<BayCandidate> bays
    ) {
        long uniqueCount =
                bays.stream()
                        .map(BayCandidate::bayId)
                        .distinct()
                        .count();

        if (uniqueCount != bays.size()) {
            throw new IllegalArgumentException(
                    "candidateBays contains duplicate bay IDs"
            );
        }
    }

    private static void validateUniqueTechnicianIds(
            List<TechnicianCandidate> technicians
    ) {
        long uniqueCount =
                technicians.stream()
                        .map(
                                TechnicianCandidate::technicianId
                        )
                        .distinct()
                        .count();

        if (uniqueCount != technicians.size()) {
            throw new IllegalArgumentException(
                    "candidateTechnicians contains duplicate technician IDs"
            );
        }
    }
}