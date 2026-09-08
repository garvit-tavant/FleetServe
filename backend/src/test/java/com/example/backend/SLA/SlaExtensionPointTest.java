package com.example.backend.SLA;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.backend.AssetManagamentService.exception.BusinessValidationException;
import com.example.backend.SLA.calendar.PauseInterval;
import com.example.backend.SLA.calendar.WorkshopCalendar;
import com.example.backend.SLA.service.ElapsedHoursSlaCalculator;
import com.example.backend.SLA.service.SlaCalculator;
import com.example.backend.SLA.service.SlaCalculatorRegistry;
import com.example.backend.SLA.service.WorkingCalendarSlaCalculator;

/**
 * The blind extension test rehearsed.
 *
 * <p>At the final demo an engineering lead adds a new service-level measurement
 * basis. It must work through one new class plus reference data, with no edit to
 * any existing class. This test stands in for that new class.
 */
class SlaExtensionPointTest {

    /**
     * A basis invented entirely here. Nothing in main/ knows it exists, which is
     * the point: registration is by interface, not by a hard-coded switch.
     */
    static class DoubleTimeSlaCalculator implements SlaCalculator {

        @Override
        public String basis() {
            return "DOUBLE_TIME";
        }

        @Override
        public long elapsedMinutes(
                Instant from,
                Instant to,
                WorkshopCalendar calendar,
                Collection<PauseInterval> pauses) {
            return java.time.Duration.between(from, to).toMinutes() * 2;
        }
    }

    @Test
    @DisplayName("A brand new calculator is discovered without touching existing classes")
    void newCalculatorIsDiscovered() {
        SlaCalculatorRegistry registry = new SlaCalculatorRegistry(List.of(
                new WorkingCalendarSlaCalculator(),
                new ElapsedHoursSlaCalculator(),
                new DoubleTimeSlaCalculator()));

        SlaCalculator resolved = registry.forBasis("DOUBLE_TIME");

        assertEquals("DOUBLE_TIME", resolved.basis());
        assertEquals(120, resolved.elapsedMinutes(
                Instant.parse("2026-01-05T09:00:00Z"),
                Instant.parse("2026-01-05T10:00:00Z"),
                null, List.of()));
    }

    @Test
    @DisplayName("Basis lookup is case and whitespace insensitive")
    void lookupIsNormalised() {
        SlaCalculatorRegistry registry = new SlaCalculatorRegistry(
                List.of(new WorkingCalendarSlaCalculator()));

        assertSame(
                registry.forBasis(WorkingCalendarSlaCalculator.BASIS),
                registry.forBasis("  working_time  "));
    }

    @Test
    @DisplayName("An unknown basis fails loudly rather than defaulting silently")
    void unknownBasisIsRejected() {
        SlaCalculatorRegistry registry = new SlaCalculatorRegistry(
                List.of(new WorkingCalendarSlaCalculator()));

        assertThrows(BusinessValidationException.class, () -> registry.forBasis("NO_SUCH_BASIS"));
    }

    @Test
    @DisplayName("Two calculators claiming one basis is a startup failure, not a coin toss")
    void duplicateBasisIsRejected() {
        assertThrows(IllegalStateException.class, () -> new SlaCalculatorRegistry(
                List.of(new WorkingCalendarSlaCalculator(), new WorkingCalendarSlaCalculator())));
    }
}
