package com.example.backend.SLA;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.CapacityAndSchedulingService.repository.HolidayRepository;
import com.example.backend.SLA.calendar.WorkshopCalendar;
import com.example.backend.SLA.calendar.WorkshopCalendarProvider;
import com.example.backend.SLA.dto.SlaEvaluationRow;
import com.example.backend.SLA.repository.BreakdownRequestRepository;
import com.example.backend.SLA.service.SlaCalculatorRegistry;
import com.example.backend.SLA.service.WorkingCalendarSlaCalculator;

/**
 * Exercises the service-level queries and calendar loading against the real
 * migrated schema.
 */
@SpringBootTest
@Transactional
class SlaRepositoryIntegrationTest {

    @Autowired
    private WorkshopCalendarProvider calendarProvider;

    @Autowired
    private HolidayRepository holidayRepository;

    @Autowired
    private BreakdownRequestRepository breakdownRequestRepository;

    @Autowired
    private SlaCalculatorRegistry calculatorRegistry;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long seededWorkshopId() {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM workshop WHERE code = 'WS-BLR-01'", Long.class);
    }

    private long insertBreakdown(String priority, String status) {
        long workshopId = seededWorkshopId();
        long depotId = jdbcTemplate.queryForObject(
                "SELECT depot_id FROM workshop WHERE id = ?", Long.class, workshopId);
        long assetId = jdbcTemplate.queryForObject(
                "SELECT id FROM asset ORDER BY id LIMIT 1", Long.class);

        jdbcTemplate.update(
                "INSERT INTO app_user (username, password_hash, is_active) VALUES (?, ?, TRUE) "
                        + "ON CONFLICT (username) DO NOTHING",
                "sla-test-reporter", "x");
        long reporterId = jdbcTemplate.queryForObject(
                "SELECT id FROM app_user WHERE username = ?", Long.class, "sla-test-reporter");

        long policyId = jdbcTemplate.queryForObject(
                "SELECT id FROM sla_policy WHERE priority = ? ORDER BY effective_from DESC LIMIT 1",
                Long.class, priority);

        jdbcTemplate.update(
                "INSERT INTO breakdown_request (asset_id, depot_id, reported_by_id, priority, "
                        + "description, status, sla_policy_id) VALUES (?, ?, ?, ?, 'engine warning', ?, ?)",
                assetId, depotId, reporterId, priority, status, policyId);

        return jdbcTemplate.queryForObject(
                "SELECT max(id) FROM breakdown_request", Long.class);
    }

    @Test
    @DisplayName("Migration leaves the schema on the P1/P2/P3 vocabulary with seeded targets")
    void prioritiesAreConfigurableData() {
        String checkClause = jdbcTemplate.queryForObject(
                "SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = 'ck_breakdown_priority'",
                String.class);

        assertNotNull(checkClause);
        assertTrue(checkClause.contains("'P1'") && checkClause.contains("'P2'")
                        && checkClause.contains("'P3'"),
                "expected P1/P2/P3, got: " + checkClause);
        assertFalse(checkClause.contains("'CRITICAL'"), "old vocabulary should be gone");

        Integer policies = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM sla_policy WHERE priority IN ('P1','P2','P3')", Integer.class);
        assertEquals(3, policies, "response and resolution targets are seeded as data, not constants");
    }

    @Test
    @DisplayName("A full weekly calendar loads without a unique-result failure")
    void weeklyCalendarLoads() {
        long workshopId = seededWorkshopId();

        Integer calendarRows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM working_calendar WHERE workshop_id = ?", Integer.class, workshopId);
        assertEquals(6, calendarRows, "fixture seeds days 1-6");

        WorkshopCalendar calendar = calendarProvider.load(
                workshopId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertNotNull(calendar.zone());
        assertEquals(6, calendar.shifts().size(), "every seeded open day is available");
        assertTrue(calendar.shiftOn(LocalDate.of(2026, 1, 5)).isPresent(), "Monday is open");
        assertTrue(calendar.shifts().containsKey(DayOfWeek.SATURDAY), "day 6 maps to Saturday");
        assertFalse(calendar.shifts().containsKey(DayOfWeek.SUNDAY), "day 7 was never seeded");
    }

    @Test
    @DisplayName("Workshop-specific and global holidays are both returned")
    void holidayLookupIncludesGlobalHolidays() {
        long workshopId = seededWorkshopId();

        jdbcTemplate.update(
                "INSERT INTO holiday (workshop_id, holiday_date, description) "
                        + "VALUES (NULL, DATE '2026-09-16', 'Synthetic global holiday')");

        List<LocalDate> holidays = holidayRepository.findHolidayDatesBetween(
                workshopId, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertTrue(holidays.contains(LocalDate.of(2026, 9, 15)), "workshop holiday from fixture");
        assertTrue(holidays.contains(LocalDate.of(2026, 9, 16)), "global holiday must not be dropped");
    }

    @Test
    @DisplayName("An open breakdown with no booking is still evaluated")
    void unbookedBreakdownIsEvaluated() {
        long breakdownId = insertBreakdown("P2", "REPORTED");

        List<SlaEvaluationRow> open = breakdownRequestRepository.findOpenForEvaluation();

        SlaEvaluationRow row = open.stream()
                .filter(r -> r.breakdownId().equals(breakdownId))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "a REPORTED breakdown with no booking must still be evaluated"));

        assertEquals(com.example.backend.SLA.status.BreakdownPriority.P2, row.priority());
        assertNotNull(row.responseTargetMinutes(), "policy targets are joined in");
        assertEquals(null, row.workshopId(), "no booking yet, so no workshop");
    }

    @Test
    @DisplayName("Terminal breakdowns drop out of the evaluation sweep")
    void resolvedBreakdownIsExcluded() {
        long resolvedId = insertBreakdown("P1", "RESOLVED");
        long cancelledId = insertBreakdown("P3", "CANCELLED");

        List<Long> ids = breakdownRequestRepository.findOpenForEvaluation().stream()
                .map(SlaEvaluationRow::breakdownId)
                .toList();

        assertFalse(ids.contains(resolvedId), "RESOLVED is terminal");
        assertFalse(ids.contains(cancelledId), "CANCELLED is terminal, spelled with two Ls");
    }

    @Test
    @DisplayName("Every seeded calendar basis resolves to a calculator")
    void everySeededBasisResolves() {
        List<String> bases = jdbcTemplate.queryForList(
                "SELECT DISTINCT calendar_basis FROM sla_policy", String.class);

        assertFalse(bases.isEmpty());
        bases.forEach(basis -> assertNotNull(calculatorRegistry.forBasis(basis),
                "no SlaCalculator registered for seeded basis " + basis));

        assertEquals(WorkingCalendarSlaCalculator.BASIS,
                calculatorRegistry.forBasis(WorkingCalendarSlaCalculator.BASIS).basis());
    }
}
