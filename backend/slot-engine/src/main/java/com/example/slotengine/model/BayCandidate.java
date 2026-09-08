package com.example.slotengine.model;

import java.util.Objects;
import java.util.Set;

/**
 * A bay offered to the engine as a possible home for the job.
 *
 * <p>Mirrors {@code service_bay} plus its {@code bay_capability} rows, but is a
 * plain value object so the engine never touches persistence.
 */
public record BayCandidate(long bayId, boolean active, Set<String> capabilities) {

    public BayCandidate {
        capabilities = capabilities == null ? Set.of() : Set.copyOf(capabilities);
    }

    public boolean hasCapability(String requiredCapability) {
        return requiredCapability == null || capabilities.contains(requiredCapability);
    }
}
