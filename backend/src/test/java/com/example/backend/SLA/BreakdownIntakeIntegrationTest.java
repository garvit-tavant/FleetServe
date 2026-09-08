package com.example.backend.SLA;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.AssetManagamentService.exception.BusinessValidationException;
import com.example.backend.AssetManagamentService.exception.ResourceNotFoundException;
import com.example.backend.SLA.dto.breakdown.BreakdownResponse;
import com.example.backend.SLA.dto.breakdown.RaiseBreakdownRequest;
import com.example.backend.SLA.service.BreakdownIntakeService;
import com.example.backend.SLA.status.BreakdownPriority;
import com.example.backend.SLA.status.BreakdownStatus;

/**
 * Breakdown intake (US-4.1): raising a breakdown and having a service-level
 * target applied to it.
 */
@SpringBootTest
@Transactional
class BreakdownIntakeIntegrationTest {

    private static final String SKILL = "OIL_SERVICE";
    private static final String CAPABILITY = "GENERAL_SERVICE";

    @Autowired
    private BreakdownIntakeService breakdownIntakeService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    private long assetId;
    private long assetDepotId;
    private long reporterId;

    @BeforeEach
    void seed() {
        assetDepotId = jdbcTemplate.queryForObject(
                "SELECT id FROM depot ORDER BY id LIMIT 1", Long.class);

        jdbcTemplate.update(
                "INSERT INTO asset_class (code, description) VALUES ('BD_T_CLASS','intake test')");
        Long classId = jdbcTemplate.queryForObject(
                "SELECT id FROM asset_class WHERE code = 'BD_T_CLASS'", Long.class);

        jdbcTemplate.update(
                "INSERT INTO asset (vin, asset_class_id, home_depot_id, acquisition_date, "
                        + "acquisition_odometer_km, status) "
                        + "VALUES ('BD-T-VIN-1', ?, ?, DATE '2024-01-01', 0, 'ACTIVE')",
                classId, assetDepotId);
        assetId = jdbcTemplate.queryForObject(
                "SELECT id FROM asset WHERE vin = 'BD-T-VIN-1'", Long.class);

        jdbcTemplate.update(
                "INSERT INTO app_user (username, password_hash, is_active) "
                        + "VALUES ('bd-t-supervisor','x',TRUE) ON CONFLICT (username) DO NOTHING");
        reporterId = jdbcTemplate.queryForObject(
                "SELECT id FROM app_user WHERE username = 'bd-t-supervisor'", Long.class);
    }

    private RaiseBreakdownRequest request(BreakdownPriority priority) {
        return new RaiseBreakdownRequest(
                assetId, priority, "Engine will not start", SKILL, CAPABILITY, null);
    }

    @Test
    @DisplayName("A raised breakdown starts as REPORTED with its policy pinned")
    void raisedBreakdownStartsReported() {
        BreakdownResponse response =
                breakdownIntakeService.raiseBreakdown(request(BreakdownPriority.P2), reporterId);

        assertNotNull(response.id());
        assertEquals(BreakdownStatus.REPORTED, response.status(), "the default state");
        assertEquals(BreakdownPriority.P2, response.priority());
        assertEquals(reporterId, response.reportedById());
        assertEquals(assetDepotId, response.depotId(), "the depot comes from the asset");
        assertEquals("BD-T-VIN-1", response.assetVin());
        assertNotNull(response.reportedAt());

        // P2 is seeded at 240 minute response, 480 minute resolution.
        assertNotNull(response.slaPolicyId(), "a policy version is pinned at intake");
        assertEquals(240, response.responseTargetMinutes());
        assertEquals(480, response.resolutionTargetMinutes());
    }

    @Test
    @DisplayName("Raising a breakdown opens its service-level checkpoint")
    void checkpointRowIsCreated() {
        BreakdownResponse response =
                breakdownIntakeService.raiseBreakdown(request(BreakdownPriority.P1), reporterId);

        entityManager.flush();

        Integer checkpoints = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM sla_checkpoint WHERE breakdown_request_id = ?",
                Integer.class, response.id());

        // Without this row every service-level UPDATE would match zero rows and
        // no breach would ever be recorded.
        assertEquals(1, checkpoints, "a checkpoint must exist for the clocks to write to");

        Boolean responseBreach = jdbcTemplate.queryForObject(
                "SELECT response_breach FROM sla_checkpoint WHERE breakdown_request_id = ?",
                Boolean.class, response.id());
        assertEquals(false, responseBreach, "a new breakdown has not breached");
    }

    @Test
    @DisplayName("Each priority pins the policy in force for it")
    void eachPriorityPinsItsOwnPolicy() {
        BreakdownResponse p1 =
                breakdownIntakeService.raiseBreakdown(request(BreakdownPriority.P1), reporterId);
        BreakdownResponse p3 =
                breakdownIntakeService.raiseBreakdown(request(BreakdownPriority.P3), reporterId);

        assertEquals(60, p1.responseTargetMinutes(), "P1 is the tightest target");
        assertEquals(480, p3.responseTargetMinutes(), "P3 is the loosest");
        assertTrue(!p1.slaPolicyId().equals(p3.slaPolicyId()),
                "different priorities pin different policy rows");
    }

    @Test
    @DisplayName("The estimated duration falls back to the skill's standard time")
    void durationDerivedFromSkill() {
        BreakdownResponse response =
                breakdownIntakeService.raiseBreakdown(request(BreakdownPriority.P2), reporterId);

        // OIL_SERVICE is seeded with a standard time of 60 minutes.
        assertEquals(60, response.estimatedDurationMinutes());
        assertEquals(SKILL, response.requiredSkillCode());
        assertEquals(CAPABILITY, response.requiredCapabilityCode());
    }

    @Test
    @DisplayName("An explicit estimate overrides the skill's standard time")
    void explicitDurationWins() {
        BreakdownResponse response = breakdownIntakeService.raiseBreakdown(
                new RaiseBreakdownRequest(
                        assetId, BreakdownPriority.P1, "Gearbox seized", SKILL, CAPABILITY, 180),
                reporterId);

        assertEquals(180, response.estimatedDurationMinutes());
    }

    @Test
    @DisplayName("A breakdown may be raised before triage has decided the details")
    void triageFieldsAreOptional() {
        BreakdownResponse response = breakdownIntakeService.raiseBreakdown(
                new RaiseBreakdownRequest(
                        assetId, BreakdownPriority.P1, "Making a noise", null, null, null),
                reporterId);

        assertEquals(BreakdownStatus.REPORTED, response.status());
        assertNull(response.requiredSkillCode());
        assertNull(response.estimatedDurationMinutes());
    }

    @Test
    @DisplayName("A retired asset cannot have a breakdown raised")
    void retiredAssetIsRefused() {
        jdbcTemplate.update("UPDATE asset SET status = 'RETIRED' WHERE id = ?", assetId);

        assertThrows(BusinessValidationException.class,
                () -> breakdownIntakeService.raiseBreakdown(
                        request(BreakdownPriority.P1), reporterId));
    }

    @Test
    @DisplayName("An unknown asset, skill or reporter is refused")
    void unknownReferencesAreRefused() {
        assertThrows(ResourceNotFoundException.class,
                () -> breakdownIntakeService.raiseBreakdown(
                        new RaiseBreakdownRequest(999_999_999L, BreakdownPriority.P1,
                                "x", null, null, null),
                        reporterId));

        assertThrows(ResourceNotFoundException.class,
                () -> breakdownIntakeService.raiseBreakdown(
                        new RaiseBreakdownRequest(assetId, BreakdownPriority.P1,
                                "x", "NO_SUCH_SKILL", null, null),
                        reporterId));

        assertThrows(ResourceNotFoundException.class,
                () -> breakdownIntakeService.raiseBreakdown(
                        request(BreakdownPriority.P1), 999_999_999L));
    }

    @Test
    @DisplayName("The list defaults to the REPORTED triage queue")
    void listFiltersByStatus() {
        BreakdownResponse reported =
                breakdownIntakeService.raiseBreakdown(request(BreakdownPriority.P2), reporterId);
        BreakdownResponse resolved =
                breakdownIntakeService.raiseBreakdown(request(BreakdownPriority.P3), reporterId);

        jdbcTemplate.update(
                "UPDATE breakdown_request SET status = 'RESOLVED' WHERE id = ?", resolved.id());
        entityManager.clear();

        List<Long> reportedIds = breakdownIntakeService
                .listBreakdowns(BreakdownStatus.REPORTED, PageRequest.of(0, 100))
                .map(BreakdownResponse::id)
                .toList();

        assertTrue(reportedIds.contains(reported.id()));
        assertTrue(!reportedIds.contains(resolved.id()), "a resolved breakdown has left the queue");

        List<Long> resolvedIds = breakdownIntakeService
                .listBreakdowns(BreakdownStatus.RESOLVED, PageRequest.of(0, 100))
                .map(BreakdownResponse::id)
                .toList();
        assertTrue(resolvedIds.contains(resolved.id()));
    }

    @Test
    @DisplayName("A null status lists every state")
    void nullStatusListsEverything() {
        BreakdownResponse raised =
                breakdownIntakeService.raiseBreakdown(request(BreakdownPriority.P2), reporterId);
        jdbcTemplate.update(
                "UPDATE breakdown_request SET status = 'CANCELLED' WHERE id = ?", raised.id());
        entityManager.clear();

        List<Long> allIds = breakdownIntakeService
                .listBreakdowns(null, PageRequest.of(0, 100))
                .map(BreakdownResponse::id)
                .toList();

        assertTrue(allIds.contains(raised.id()));
    }

    @Test
    @DisplayName("The list is paginated and capped server-side")
    void listIsCapped() {
        Page<BreakdownResponse> page = breakdownIntakeService
                .listBreakdowns(BreakdownStatus.REPORTED, PageRequest.of(0, 5000));

        assertEquals(BreakdownIntakeService.MAX_PAGE_SIZE, page.getPageable().getPageSize());
    }

    @Test
    @DisplayName("A raised breakdown can be fetched back by its id")
    void breakdownIsRetrievable() {
        BreakdownResponse raised =
                breakdownIntakeService.raiseBreakdown(request(BreakdownPriority.P2), reporterId);

        BreakdownResponse fetched = breakdownIntakeService.getBreakdown(raised.id());

        assertEquals(raised.id(), fetched.id());
        assertEquals(BreakdownStatus.REPORTED, fetched.status());
        assertEquals("Engine will not start", fetched.description());
    }
}
