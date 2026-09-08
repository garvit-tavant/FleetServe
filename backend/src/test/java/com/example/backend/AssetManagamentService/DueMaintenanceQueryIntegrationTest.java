package com.example.backend.AssetManagamentService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.AssetManagamentService.repository.DueMaintenanceRepository;
import com.example.backend.AssetManagamentService.repository.projection.DueMaintenanceProjection;

/**
 * INV-5: next-due equals the earlier of the distance-based and the time-based
 * threshold, for every plan configuration.
 *
 * <p>Hand-computed matrix required by US-1.3. The evaluation date is fixed at
 * 2026-03-01. Plan intervals are 10000 km and/or 180 days, with stored DUE_SOON
 * windows of 500 km and 14 days.
 */
@SpringBootTest
@Transactional
class DueMaintenanceQueryIntegrationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 3, 1);

    @Autowired
    private DueMaintenanceRepository dueMaintenanceRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long depotId;
    private long otherDepotId;
    private long reporterId;

    @BeforeEach
    void seed() {
        List<Long> depots = jdbcTemplate.queryForList(
                "SELECT id FROM depot ORDER BY id LIMIT 2", Long.class);
        depotId = depots.get(0);
        otherDepotId = depots.size() > 1 ? depots.get(1) : depots.get(0);

        jdbcTemplate.update(
                "INSERT INTO app_user (username, password_hash, is_active) VALUES (?, ?, TRUE) "
                        + "ON CONFLICT (username) DO NOTHING",
                "due-test-user", "x");
        reporterId = jdbcTemplate.queryForObject(
                "SELECT id FROM app_user WHERE username = ?", Long.class, "due-test-user");

        classWithPlan("DUE_T_DIST", "DUE_T_PLAN_DIST", new BigDecimal("10000"), null, new BigDecimal("500"), null);
        classWithPlan("DUE_T_TIME", "DUE_T_PLAN_TIME", null, 180, null, 14);
        classWithPlan("DUE_T_BOTH", "DUE_T_PLAN_BOTH", new BigDecimal("10000"), 180, new BigDecimal("500"), 14);
    }

    private void classWithPlan(String classCode, String planCode,
                               BigDecimal distanceKm, Integer days,
                               BigDecimal soonKm, Integer soonDays) {
        jdbcTemplate.update(
                "INSERT INTO asset_class (code, description) VALUES (?, ?)",
                classCode, classCode);
        jdbcTemplate.update(
                "INSERT INTO maintenance_plan (code, distance_interval_km, time_interval_days, "
                        + "estimated_duration_minutes) VALUES (?, ?, ?, 60)",
                planCode, distanceKm, days);
        // The DUE_SOON windows belong to the pairing, not the plan.
        jdbcTemplate.update(
                "INSERT INTO asset_class_plan (asset_class_id, maintenance_plan_id, "
                        + "due_soon_distance_km, due_soon_days) "
                        + "SELECT ac.id, mp.id, ?, ? FROM asset_class ac, maintenance_plan mp "
                        + "WHERE ac.code = ? AND mp.code = ?",
                soonKm, soonDays, classCode, planCode);
    }

    private void asset(String vin, String classCode, LocalDate acquired,
                       BigDecimal acquisitionKm, BigDecimal latestReadingKm) {
        asset(vin, classCode, acquired, acquisitionKm, latestReadingKm, depotId);
    }

    private void asset(String vin, String classCode, LocalDate acquired,
                       BigDecimal acquisitionKm, BigDecimal latestReadingKm, long depot) {
        jdbcTemplate.update(
                "INSERT INTO asset (vin, asset_class_id, home_depot_id, acquisition_date, "
                        + "acquisition_odometer_km, status) "
                        + "SELECT ?, ac.id, ?, ?, ?, 'ACTIVE' FROM asset_class ac WHERE ac.code = ?",
                vin, depot, acquired, acquisitionKm, classCode);

        if (latestReadingKm != null) {
            jdbcTemplate.update(
                    "INSERT INTO odometer_reading (asset_id, reading_km, read_at, source, recorded_by_id) "
                            + "SELECT a.id, ?, TIMESTAMPTZ '2026-02-28 10:00:00+00', 'MANUAL', ? "
                            + "FROM asset a WHERE a.vin = ?",
                    latestReadingKm, reporterId, vin);
        }
    }

    /**
     * Records a completed preventive service, which becomes the new baseline for
     * that asset and plan. Requires a booking, because the plan link lives there.
     */
    private void completedService(String vin, String planCode,
                                  LocalDate servicedOn, BigDecimal odometerAtService) {
        Long assetId = jdbcTemplate.queryForObject(
                "SELECT id FROM asset WHERE vin = ?", Long.class, vin);
        Long workshopId = jdbcTemplate.queryForObject(
                "SELECT id FROM workshop ORDER BY id LIMIT 1", Long.class);
        Long bayId = jdbcTemplate.queryForObject(
                "SELECT id FROM service_bay WHERE workshop_id = ? ORDER BY id LIMIT 1",
                Long.class, workshopId);
        Long technicianId = ensureTechnician(workshopId);

        jdbcTemplate.update(
                "INSERT INTO booking (asset_id, workshop_id, bay_id, technician_id, slot, kind, "
                        + "maintenance_plan_id, status) "
                        + "SELECT ?, ?, ?, ?, "
                        + "tstzrange(CAST(? AS timestamptz), CAST(? AS timestamptz)), "
                        + "'PREVENTIVE', mp.id, 'COMPLETED' "
                        + "FROM maintenance_plan mp WHERE mp.code = ?",
                assetId, workshopId, bayId, technicianId,
                servicedOn.atTime(8, 0) + "+00",
                servicedOn.atTime(10, 0) + "+00",
                planCode);

        Long bookingId = jdbcTemplate.queryForObject(
                "SELECT max(id) FROM booking", Long.class);

        jdbcTemplate.update(
                "INSERT INTO work_order (work_order_number, booking_id, asset_id, status, "
                        + "started_at, completed_at, odometer_at_service, idempotency_key) "
                        + "VALUES (?, ?, ?, 'COMPLETED', CAST(? AS timestamptz), CAST(? AS timestamptz), ?, ?)",
                "WO-" + vin + "-" + planCode,
                bookingId, assetId,
                servicedOn.atTime(8, 0) + "+00",
                servicedOn.atTime(10, 0) + "+00",
                odometerAtService,
                "IDEM-" + vin + "-" + planCode);
    }

    /** The reference fixture seeds no technicians, so the test supplies one. */
    private Long ensureTechnician(Long workshopId) {
        List<Long> existing = jdbcTemplate.queryForList(
                "SELECT id FROM technician WHERE workshop_id = ? ORDER BY id LIMIT 1",
                Long.class, workshopId);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }

        jdbcTemplate.update(
                "INSERT INTO app_user (username, password_hash, is_active) VALUES (?, ?, TRUE) "
                        + "ON CONFLICT (username) DO NOTHING",
                "due-test-technician", "x");
        Long technicianUserId = jdbcTemplate.queryForObject(
                "SELECT id FROM app_user WHERE username = ?", Long.class, "due-test-technician");

        jdbcTemplate.update(
                "INSERT INTO technician (app_user_id, workshop_id, hourly_rate) VALUES (?, ?, 100) "
                        + "ON CONFLICT (app_user_id) DO NOTHING",
                technicianUserId, workshopId);

        return jdbcTemplate.queryForObject(
                "SELECT id FROM technician WHERE app_user_id = ?", Long.class, technicianUserId);
    }

    private Map<String, DueMaintenanceProjection> runDueList() {
        return dueMaintenanceRepository.findDueMaintenance(
                        TODAY, null, null, null, null, null, false, PageRequest.of(0, 100))
                .getContent().stream()
                .filter(r -> r.getVin().startsWith("DUE-T-"))
                .collect(Collectors.toMap(DueMaintenanceProjection::getVin, r -> r));
    }

    @Test
    @DisplayName("Distance-only plans are judged solely on kilometres")
    void distanceOnlyPlan() {
        asset("DUE-T-D1", "DUE_T_DIST", LocalDate.of(2026, 1, 1), BigDecimal.ZERO, BigDecimal.ZERO);
        asset("DUE-T-D2", "DUE_T_DIST", LocalDate.of(2026, 1, 1), BigDecimal.ZERO, new BigDecimal("9600"));
        asset("DUE-T-D3", "DUE_T_DIST", LocalDate.of(2026, 1, 1), BigDecimal.ZERO, new BigDecimal("10000"));
        asset("DUE-T-D4", "DUE_T_DIST", LocalDate.of(2026, 1, 1), BigDecimal.ZERO, new BigDecimal("12000"));

        Map<String, DueMaintenanceProjection> due = runDueList();

        assertEquals("OK", due.get("DUE-T-D1").getDueStatus());
        assertEquals(0, new BigDecimal("10000").compareTo(due.get("DUE-T-D1").getKmRemaining()));

        assertEquals("DUE_SOON", due.get("DUE-T-D2").getDueStatus());
        assertEquals("OVERDUE", due.get("DUE-T-D3").getDueStatus());

        assertEquals("OVERDUE", due.get("DUE-T-D4").getDueStatus());
        assertEquals(0, new BigDecimal("-2000").compareTo(due.get("DUE-T-D4").getKmRemaining()));

        assertNotNull(due.get("DUE-T-D1").getNextDueKm());
        assertNull(due.get("DUE-T-D1").getNextDueDate());
        assertNull(due.get("DUE-T-D1").getDaysRemaining());
    }

    @Test
    @DisplayName("Time-only plans are judged solely on elapsed days")
    void timeOnlyPlan() {
        asset("DUE-T-T1", "DUE_T_TIME", LocalDate.of(2026, 1, 1), BigDecimal.ZERO, BigDecimal.ZERO);
        asset("DUE-T-T2", "DUE_T_TIME", LocalDate.of(2025, 9, 10), BigDecimal.ZERO, BigDecimal.ZERO);
        asset("DUE-T-T3", "DUE_T_TIME", LocalDate.of(2025, 9, 1), BigDecimal.ZERO, BigDecimal.ZERO);

        Map<String, DueMaintenanceProjection> due = runDueList();

        assertEquals("OK", due.get("DUE-T-T1").getDueStatus());
        assertEquals(LocalDate.of(2026, 6, 30), due.get("DUE-T-T1").getNextDueDate());
        assertEquals(121, due.get("DUE-T-T1").getDaysRemaining());

        assertEquals("DUE_SOON", due.get("DUE-T-T2").getDueStatus());
        assertEquals(8, due.get("DUE-T-T2").getDaysRemaining());

        assertEquals("OVERDUE", due.get("DUE-T-T3").getDueStatus());
        assertEquals(-1, due.get("DUE-T-T3").getDaysRemaining());

        assertNull(due.get("DUE-T-T1").getNextDueKm());
        assertNull(due.get("DUE-T-T1").getKmRemaining());
    }

    @Test
    @DisplayName("With both intervals, whichever threshold is reached first decides")
    void earlierOfTheTwoThresholdsWins() {
        asset("DUE-T-B1", "DUE_T_BOTH", LocalDate.of(2026, 1, 1), BigDecimal.ZERO, new BigDecimal("9600"));
        asset("DUE-T-B2", "DUE_T_BOTH", LocalDate.of(2025, 9, 1), BigDecimal.ZERO, new BigDecimal("100"));
        asset("DUE-T-B3", "DUE_T_BOTH", LocalDate.of(2026, 1, 1), BigDecimal.ZERO, new BigDecimal("100"));

        Map<String, DueMaintenanceProjection> due = runDueList();

        assertEquals("DUE_SOON", due.get("DUE-T-B1").getDueStatus());
        assertTrue(due.get("DUE-T-B1").getDaysRemaining() > 14,
                "the date clock alone would have said OK");

        assertEquals("OVERDUE", due.get("DUE-T-B2").getDueStatus());
        assertTrue(due.get("DUE-T-B2").getKmRemaining().compareTo(BigDecimal.ZERO) > 0,
                "the distance clock alone would have said OK");

        assertEquals("OK", due.get("DUE-T-B3").getDueStatus());
    }

    @Test
    @DisplayName("An asset with no service history uses acquisition as the baseline")
    void neverServicedUsesAcquisitionBaseline() {
        asset("DUE-T-N1", "DUE_T_DIST", LocalDate.of(2026, 1, 1), new BigDecimal("9800"), null);

        DueMaintenanceProjection row = runDueList().get("DUE-T-N1");

        assertEquals(0, new BigDecimal("9800").compareTo(row.getCurrentOdometerKm()));
        assertEquals(0, new BigDecimal("10000").compareTo(row.getKmRemaining()));
        assertEquals("OK", row.getDueStatus());
    }

    @Test
    @DisplayName("An asset serviced today resets both clocks from the service, not acquisition")
    void servicedTodayResetsTheBaseline() {
        // Acquired long ago and far past both thresholds on acquisition baseline.
        asset("DUE-T-S1", "DUE_T_BOTH", LocalDate.of(2024, 1, 1), BigDecimal.ZERO, new BigDecimal("50000"));

        assertEquals("OVERDUE", runDueList().get("DUE-T-S1").getDueStatus(),
                "precondition: overdue when measured from acquisition");

        completedService("DUE-T-S1", "DUE_T_PLAN_BOTH", TODAY, new BigDecimal("50000"));

        DueMaintenanceProjection row = runDueList().get("DUE-T-S1");

        assertEquals("OK", row.getDueStatus(), "servicing today clears the backlog");
        assertEquals(LocalDate.of(2026, 8, 28), row.getNextDueDate(), "2026-03-01 plus 180 days");
        assertEquals(0, new BigDecimal("60000").compareTo(row.getNextDueKm()),
                "50000 at service plus the 10000 km interval");
        assertEquals(0, new BigDecimal("10000").compareTo(row.getKmRemaining()));
    }

    @Test
    @DisplayName("DUE_SOON windows come from the class-plan pairing, and a request may override them")
    void thresholdsAreDataAndOverridable() {
        asset("DUE-T-C1", "DUE_T_DIST", LocalDate.of(2026, 1, 1), BigDecimal.ZERO, new BigDecimal("9000"));

        // 1000 km remain, outside the stored 500 km window.
        assertEquals("OK", runDueList().get("DUE-T-C1").getDueStatus());

        // Widening the window on the pairing turns the same asset amber.
        jdbcTemplate.update(
                "UPDATE asset_class_plan SET due_soon_distance_km = 1500 "
                        + "WHERE maintenance_plan_id = (SELECT id FROM maintenance_plan WHERE code = ?)",
                "DUE_T_PLAN_DIST");
        assertEquals("DUE_SOON", runDueList().get("DUE-T-C1").getDueStatus(),
                "threshold is data: changing the row changes the answer");

        // A per-request override takes precedence over the stored value.
        DueMaintenanceProjection overridden = dueMaintenanceRepository.findDueMaintenance(
                        TODAY, new BigDecimal("100"), null, null, null, null, false,
                        PageRequest.of(0, 100))
                .getContent().stream()
                .filter(r -> "DUE-T-C1".equals(r.getVin()))
                .findFirst().orElseThrow();

        assertEquals("OK", overridden.getDueStatus(), "a narrower override wins");
    }

    @Test
    @DisplayName("The same plan can carry different windows for different asset classes")
    void windowsArePerPairingNotPerPlan() {
        // A second class reusing the very same plan, with a much wider window.
        jdbcTemplate.update(
                "INSERT INTO asset_class (code, description) VALUES ('DUE_T_HEAVY', 'heavy')");
        jdbcTemplate.update(
                "INSERT INTO asset_class_plan (asset_class_id, maintenance_plan_id, due_soon_distance_km) "
                        + "SELECT ac.id, mp.id, 2000 FROM asset_class ac, maintenance_plan mp "
                        + "WHERE ac.code = 'DUE_T_HEAVY' AND mp.code = 'DUE_T_PLAN_DIST'");

        // Identical odometer position, 1000 km short of the same 10000 km interval.
        asset("DUE-T-P1", "DUE_T_DIST", LocalDate.of(2026, 1, 1), BigDecimal.ZERO, new BigDecimal("9000"));
        asset("DUE-T-P2", "DUE_T_HEAVY", LocalDate.of(2026, 1, 1), BigDecimal.ZERO, new BigDecimal("9000"));

        Map<String, DueMaintenanceProjection> due = runDueList();

        assertEquals("OK", due.get("DUE-T-P1").getDueStatus(), "500 km window: not yet amber");
        assertEquals("DUE_SOON", due.get("DUE-T-P2").getDueStatus(), "2000 km window: already amber");
    }

    @Test
    @DisplayName("A window longer than its own interval is refused by the database")
    void windowLongerThanIntervalIsRefused() {
        jdbcTemplate.update(
                "INSERT INTO asset_class (code, description) VALUES ('DUE_T_BAD', 'bad')");

        assertThrows(DataAccessException.class, () -> jdbcTemplate.update(
                        "INSERT INTO asset_class_plan (asset_class_id, maintenance_plan_id, due_soon_distance_km) "
                                + "SELECT ac.id, mp.id, 999999 FROM asset_class ac, maintenance_plan mp "
                                + "WHERE ac.code = 'DUE_T_BAD' AND mp.code = 'DUE_T_PLAN_DIST'"),
                "a warning window wider than the interval would leave every asset permanently amber");
    }

    @Test
    @DisplayName("A distance window on a time-only plan is refused by the database")
    void windowOnUnmeasuredAxisIsRefused() {
        jdbcTemplate.update(
                "INSERT INTO asset_class (code, description) VALUES ('DUE_T_AXIS', 'axis')");

        assertThrows(DataAccessException.class, () -> jdbcTemplate.update(
                        "INSERT INTO asset_class_plan (asset_class_id, maintenance_plan_id, due_soon_distance_km) "
                                + "SELECT ac.id, mp.id, 100 FROM asset_class ac, maintenance_plan mp "
                                + "WHERE ac.code = 'DUE_T_AXIS' AND mp.code = 'DUE_T_PLAN_TIME'"),
                "the plan measures no distance, so a distance window is meaningless");
    }

    @Test
    @DisplayName("The list filters by depot, asset class and status")
    void filtersApply() {
        asset("DUE-T-F1", "DUE_T_DIST", LocalDate.of(2026, 1, 1), BigDecimal.ZERO, new BigDecimal("12000"));
        asset("DUE-T-F2", "DUE_T_TIME", LocalDate.of(2026, 1, 1), BigDecimal.ZERO, BigDecimal.ZERO);
        asset("DUE-T-F3", "DUE_T_DIST", LocalDate.of(2026, 1, 1), BigDecimal.ZERO,
                new BigDecimal("12000"), otherDepotId);

        List<String> byClass = vins(dueMaintenanceRepository.findDueMaintenance(
                TODAY, null, null, null, "DUE_T_TIME", null, false, PageRequest.of(0, 100))
                .getContent());
        assertTrue(byClass.contains("DUE-T-F2"));
        assertFalse(byClass.contains("DUE-T-F1"), "other classes are excluded");

        List<String> byStatus = vins(dueMaintenanceRepository.findDueMaintenance(
                TODAY, null, null, null, null, "OVERDUE", false, PageRequest.of(0, 100))
                .getContent());
        assertTrue(byStatus.contains("DUE-T-F1"));
        assertFalse(byStatus.contains("DUE-T-F2"), "OK rows are excluded");

        if (otherDepotId != depotId) {
            List<String> byDepot = vins(dueMaintenanceRepository.findDueMaintenance(
                    TODAY, null, null, depotId, null, null, false, PageRequest.of(0, 100))
                    .getContent());
            assertTrue(byDepot.contains("DUE-T-F1"));
            assertFalse(byDepot.contains("DUE-T-F3"), "other depots are excluded");
        }
    }

    private static List<String> vins(List<DueMaintenanceProjection> rows) {
        return rows.stream()
                .map(DueMaintenanceProjection::getVin)
                .filter(v -> v != null && v.startsWith("DUE-T-"))
                .toList();
    }

    @Test
    @DisplayName("Retired assets never appear, and dueOnly filters out compliant rows")
    void retiredExcludedAndDueOnlyFilters() {
        asset("DUE-T-R1", "DUE_T_DIST", LocalDate.of(2026, 1, 1), BigDecimal.ZERO, BigDecimal.ZERO);
        asset("DUE-T-R2", "DUE_T_DIST", LocalDate.of(2026, 1, 1), BigDecimal.ZERO, new BigDecimal("12000"));
        jdbcTemplate.update("UPDATE asset SET status = 'RETIRED' WHERE vin = 'DUE-T-R2'");

        Map<String, DueMaintenanceProjection> all = runDueList();
        assertTrue(all.containsKey("DUE-T-R1"));
        assertFalse(all.containsKey("DUE-T-R2"), "retired assets disappear from due lists");

        List<String> dueOnlyVins = vins(dueMaintenanceRepository.findDueMaintenance(
                TODAY, null, null, null, null, null, true, PageRequest.of(0, 100))
                .getContent());
        assertFalse(dueOnlyVins.contains("DUE-T-R1"), "an OK row is filtered out by dueOnly");
    }

    @Test
    @DisplayName("Running the due list twice returns identical results (INV-8)")
    void dueListIsIdempotent() {
        asset("DUE-T-I1", "DUE_T_BOTH", LocalDate.of(2025, 9, 1), BigDecimal.ZERO, new BigDecimal("9600"));

        Map<String, String> first = runDueList().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getDueStatus()));
        Map<String, String> second = runDueList().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getDueStatus()));

        assertEquals(first, second);
    }
}
