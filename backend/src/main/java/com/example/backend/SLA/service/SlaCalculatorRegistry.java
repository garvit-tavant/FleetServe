package com.example.backend.SLA.service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.backend.AssetManagamentService.exception.BusinessValidationException;

/**
 * Resolves the {@link SlaCalculator} for a policy's {@code calendar_basis}.
 *
 * <p>Implementations are injected by type, so adding a new basis requires only a
 * new {@code @Component} implementing {@link SlaCalculator} and an
 * {@code sla_policy} row naming it. No existing class changes, which is what the
 * blind extension test at the demo checks.
 */
@Component
public class SlaCalculatorRegistry {

    private final Map<String, SlaCalculator> byBasis;

    public SlaCalculatorRegistry(List<SlaCalculator> calculators) {
        Map<String, SlaCalculator> registry = new HashMap<>();
        for (SlaCalculator calculator : calculators) {
            String key = normalise(calculator.basis());
            SlaCalculator existing = registry.putIfAbsent(key, calculator);
            if (existing != null) {
                throw new IllegalStateException(
                        "Two SlaCalculator beans claim basis '" + calculator.basis() + "': "
                                + existing.getClass().getName() + " and "
                                + calculator.getClass().getName());
            }
        }
        this.byBasis = Map.copyOf(registry);
    }

    public SlaCalculator forBasis(String basis) {
        SlaCalculator calculator = byBasis.get(normalise(basis));
        if (calculator == null) {
            throw new BusinessValidationException(
                    "No SlaCalculator registered for calendar basis '" + basis
                            + "'. Registered: " + byBasis.keySet());
        }
        return calculator;
    }

    /** Used when a breakdown has no workshop yet, so no calendar can be applied. */
    public SlaCalculator elapsedFallback() {
        return forBasis(ElapsedHoursSlaCalculator.BASIS);
    }

    private static String normalise(String basis) {
        return basis == null ? "" : basis.trim().toUpperCase(Locale.ROOT);
    }
}
