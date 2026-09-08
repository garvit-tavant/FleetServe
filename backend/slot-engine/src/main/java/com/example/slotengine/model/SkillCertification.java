package com.example.slotengine.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * One certification a technician holds, valid over a closed date range.
 *
 * <p>{@code validTo} of {@code null} means open-ended. Validity is inclusive at
 * both ends, matching the rule agreed for scheduling:
 * {@code validFrom <= date && (validTo == null || validTo >= date)}.
 */
public record SkillCertification(String skillCode, LocalDate validFrom, LocalDate validTo) {

    public SkillCertification {
        Objects.requireNonNull(skillCode, "skillCode");
        Objects.requireNonNull(validFrom, "validFrom");
    }

    public boolean coversDate(LocalDate date) {
        if (date.isBefore(validFrom)) {
            return false;
        }
        return validTo == null || !date.isAfter(validTo);
    }

    public boolean isFor(String requiredSkill) {
        return skillCode.equals(requiredSkill);
    }
}
