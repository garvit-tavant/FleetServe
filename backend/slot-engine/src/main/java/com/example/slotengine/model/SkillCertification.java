package com.example.slotengine.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * One certification a technician holds, valid over a closed date range.
 *
 * validTo == null means open-ended.
 */
public record SkillCertification(
        String skillCode,
        LocalDate validFrom,
        LocalDate validTo
) {

    public SkillCertification {
        Objects.requireNonNull(
                skillCode,
                "skillCode"
        );

        Objects.requireNonNull(
                validFrom,
                "validFrom"
        );

        if (skillCode.isBlank()) {
            throw new IllegalArgumentException(
                    "skillCode must not be blank"
            );
        }

        if (validTo != null
                && validTo.isBefore(validFrom)) {

            throw new IllegalArgumentException(
                    "validTo must not be before validFrom"
            );
        }
    }

    public boolean coversDate(
            LocalDate date
    ) {
        if (date.isBefore(validFrom)) {
            return false;
        }

        return validTo == null
                || !date.isAfter(validTo);
    }

    public boolean isFor(
            String requiredSkill
    ) {
        return skillCode.equals(requiredSkill);
    }
}