package com.example.slotengine.model;

import java.time.LocalDate;
import java.util.List;

/**
 * A technician offered to the engine, with the certifications they hold.
 *
 * <p>Because a job may not span two working days, a certification that covers
 * the job's date necessarily covers the whole slot, so validity is checked per
 * date rather than per instant.
 */
public record TechnicianCandidate(
        long technicianId,
        boolean active,
        List<SkillCertification> certifications) {

    public TechnicianCandidate {
        certifications = certifications == null ? List.of() : List.copyOf(certifications);
    }

    /** True when the technician holds the required skill on that date. */
    public boolean isCertifiedFor(String requiredSkill, LocalDate date) {
        if (requiredSkill == null) {
            return true;
        }
        for (SkillCertification certification : certifications) {
            if (certification.isFor(requiredSkill) && certification.coversDate(date)) {
                return true;
            }
        }
        return false;
    }

    /** True when the technician holds the required skill on at least one date. */
    public boolean holdsSkill(String requiredSkill) {
        if (requiredSkill == null) {
            return true;
        }
        return certifications.stream().anyMatch(c -> c.isFor(requiredSkill));
    }
}
