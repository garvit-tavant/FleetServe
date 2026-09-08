package com.example.backend.ExecutionService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.AssetManagamentService.exception.BusinessValidationException;
import com.example.backend.AssetManagamentService.exception.DuplicateResourceException;
import com.example.backend.AssetManagamentService.exception.ResourceNotFoundException;
import com.example.backend.ExecutionService.dto.booking.BookBreakdownSlotRequest;
import com.example.backend.ExecutionService.dto.booking.BookPreventiveSlotRequest;
import com.example.backend.ExecutionService.dto.booking.BookingResponse;
import com.example.backend.ExecutionService.repository.BookingRepository;
import com.example.backend.ExecutionService.service.SlotBookingService;

/**
 * End-to-end cover for the two booking flows: preventive work from a due
 * maintenance plan, and corrective work from a breakdown request.
 *
 * <p>Each test seeds its own workshop so fixture data cannot interfere, and runs
 * inside a rolled-back transaction.
 */
@SpringBootTest
@Transactional
class SlotBookingIntegrationTest {

    private static final String SKILL = "OIL_SERVICE";
    private static final String CAPABILITY = "GENERAL_SERVICE";
    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    /**
     * A Monday comfortably in the future.
     *
     * <p>Computed rather than hard-coded: an earlier version of this test pinned
     * a literal date that quietly slipped into the past, which hid the fact that
     * the service was happily booking slots months before today.
     */
    private static final LocalDate MONDAY = LocalDate.now(java.time.Clock.systemUTC())
            .plusDays(30)
            .with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.MONDAY));

    @Autowired
    private SlotBookingService slotBookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private com.example.backend.SLA.service.BreakdownIntakeService breakdownIntakeService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    private long workshopId;
    private long bayId;
    private long technicianId;
    private long assetId;
    private long planId;
    private long depotId;
    private long reporterId;

    @BeforeEach
    void seed() {
        depotId = jdbcTemplate.queryForObject(
                "SELECT id FROM depot ORDER BY id LIMIT 1", Long.class);

        jdbcTemplate.update(
                "INSERT INTO workshop (code, depot_id, time_zone) "
                        + "VALUES ('WS-BOOK-T', ?, 'Asia/Kolkata')", depotId);
        workshopId = jdbcTemplate.queryForObject(
                "SELECT id FROM workshop WHERE code = 'WS-BOOK-T'", Long.class);

        jdbcTemplate.update(
                "INSERT INTO service_bay (workshop_id, bay_code) VALUES (?, 'BAY-BOOK-T')",
                workshopId);
        bayId = jdbcTemplate.queryForObject(
                "SELECT id FROM service_bay WHERE bay_code = 'BAY-BOOK-T'", Long.class);
        jdbcTemplate.update(
                "INSERT INTO bay_capability (bay_id, capability_code) VALUES (?, ?)",
                bayId, CAPABILITY);

        // Open 08:00-17:00 Monday to Friday.
        for (int day = 1; day <= 5; day++) {
            jdbcTemplate.update(
                    "INSERT INTO working_calendar (workshop_id, day_of_week, open_time, close_time) "
                            + "VALUES (?, ?, TIME '08:00', TIME '17:00')", workshopId, day);
        }

        jdbcTemplate.update(
                "INSERT INTO app_user (username, password_hash, is_active) "
                        + "VALUES ('book-t-tech','x',TRUE) ON CONFLICT (username) DO NOTHING");
        Long techUserId = jdbcTemplate.queryForObject(
                "SELECT id FROM app_user WHERE username = 'book-t-tech'", Long.class);
        jdbcTemplate.update(
                "INSERT INTO technician (app_user_id, workshop_id, hourly_rate) VALUES (?,?,500)",
                techUserId, workshopId);
        technicianId = jdbcTemplate.queryForObject(
                "SELECT id FROM technician WHERE app_user_id = ?", Long.class, techUserId);
        jdbcTemplate.update(
                "INSERT INTO technician_skill (technician_id, skill_code, valid_from) "
                        + "VALUES (?, ?, DATE '2020-01-01')", technicianId, SKILL);

        jdbcTemplate.update(
                "INSERT INTO asset_class (code, description) VALUES ('BOOK_T_CLASS','booking test')");
        Long classId = jdbcTemplate.queryForObject(
                "SELECT id FROM asset_class WHERE code = 'BOOK_T_CLASS'", Long.class);

        jdbcTemplate.update(
                "INSERT INTO maintenance_plan (code, distance_interval_km, time_interval_days, "
                        + "estimated_duration_minutes, required_skill_code, required_capability_code) "
                        + "VALUES ('BOOK_T_PLAN', 10000, 180, 60, ?, ?)", SKILL, CAPABILITY);
        planId = jdbcTemplate.queryForObject(
                "SELECT id FROM maintenance_plan WHERE code = 'BOOK_T_PLAN'", Long.class);
        jdbcTemplate.update(
                "INSERT INTO asset_class_plan (asset_class_id, maintenance_plan_id) VALUES (?,?)",
                classId, planId);

        jdbcTemplate.update(
                "INSERT INTO asset (vin, asset_class_id, home_depot_id, acquisition_date, "
                        + "acquisition_odometer_km, status) "
                        + "VALUES ('BOOK-T-VIN-1', ?, ?, DATE '2024-01-01', 0, 'ACTIVE')",
                classId, depotId);
        assetId = jdbcTemplate.queryForObject(
                "SELECT id FROM asset WHERE vin = 'BOOK-T-VIN-1'", Long.class);

        reporterId = jdbcTemplate.queryForObject(
                "SELECT id FROM app_user ORDER BY id LIMIT 1", Long.class);
    }

    private long newBreakdown(String status) {
        return newBreakdown(status, SKILL, CAPABILITY, null, depotId);
    }

    /**
     * Triage now records the work requirements on the breakdown itself, so the
     * booking call carries only the workshop.
     */
    private long newBreakdown(
            String status,
            String skillCode,
            String capabilityCode,
            Integer durationMinutes,
            long breakdownDepotId) {
        Long policyId = jdbcTemplate.queryForObject(
                "SELECT id FROM sla_policy WHERE priority = 'P2' ORDER BY effective_from DESC LIMIT 1",
                Long.class);
        jdbcTemplate.update(
                "INSERT INTO breakdown_request (asset_id, depot_id, reported_by_id, priority, "
                        + "description, status, sla_policy_id, required_skill_code, "
                        + "required_capability_code, estimated_duration_minutes) "
                        + "VALUES (?,?,?, 'P2', 'will not start', ?, ?, ?, ?, ?)",
                assetId, breakdownDepotId, reporterId, status, policyId,
                skillCode, capabilityCode, durationMinutes);
        return jdbcTemplate.queryForObject("SELECT max(id) FROM breakdown_request", Long.class);
    }

    private BookPreventiveSlotRequest preventiveRequest() {
        return new BookPreventiveSlotRequest(assetId, planId, workshopId, MONDAY);
    }

    private BookBreakdownSlotRequest breakdownRequest() {
        return new BookBreakdownSlotRequest(workshopId, MONDAY);
    }

    @Test
    @DisplayName("A due maintenance plan is booked into the earliest slot")
    void preventiveBookingTakesEarliestSlot() {
        BookingResponse response = slotBookingService.bookPreventiveSlot(preventiveRequest());

        assertNotNull(response.bookingId());
        assertEquals("PREVENTIVE", response.kind());
        assertEquals("HELD", response.status());
        assertEquals(planId, response.maintenancePlanId());
        assertNull(response.breakdownRequestId(), "preventive work has no breakdown");
        assertEquals(bayId, response.bayId());
        assertEquals(technicianId, response.technicianId());

        assertEquals(MONDAY, response.slotStart().atZoneSameInstant(ZONE).toLocalDate());
        assertEquals(8, response.slotStart().atZoneSameInstant(ZONE).getHour(),
                "the plan's 60 minute job takes the 08:00 opening slot");
        assertEquals(9, response.slotEnd().atZoneSameInstant(ZONE).getHour());
    }

    @Test
    @DisplayName("A breakdown is booked and moves to BOOKED")
    void breakdownBookingMovesStatus() {
        long breakdownId = newBreakdown("REPORTED");

        BookingResponse response =
                slotBookingService.bookBreakdownSlot(breakdownId, breakdownRequest());

        assertEquals("CORRECTIVE", response.kind());
        assertEquals(breakdownId, response.breakdownRequestId());
        assertNull(response.maintenancePlanId(), "corrective work has no plan");

        // The status change is a JPA write, so flush before reading it back
        // through plain JDBC, which does not see the persistence context.
        entityManager.flush();
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM breakdown_request WHERE id = ?", String.class, breakdownId);
        assertEquals("BOOKED", status);
    }

    @Test
    @DisplayName("The second booking takes the next free slot, not the same one")
    void secondBookingDoesNotOverlapTheFirst() {
        BookingResponse first = slotBookingService.bookPreventiveSlot(preventiveRequest());

        long breakdownId = newBreakdown("REPORTED");
        BookingResponse second =
                slotBookingService.bookBreakdownSlot(breakdownId, breakdownRequest());

        assertTrue(!second.slotStart().isBefore(first.slotEnd()),
                "second booking at " + second.slotStart()
                        + " must not start before the first ends at " + first.slotEnd());
    }

    @Test
    @DisplayName("No slot in the next 14 days is reported as not booked")
    void noSlotWithinHorizonIsReported() {
        // Shut the workshop for the whole horizon.
        jdbcTemplate.update("DELETE FROM working_calendar WHERE workshop_id = ?", workshopId);

        BusinessValidationException error = assertThrows(BusinessValidationException.class,
                () -> slotBookingService.bookPreventiveSlot(preventiveRequest()));

        assertTrue(error.getMessage().contains("No slot was booked"),
                "message should say the slot was not booked: " + error.getMessage());
        assertTrue(error.getMessage().contains("14 days"),
                "message should state the horizon searched: " + error.getMessage());
    }

    @Test
    @DisplayName("A job longer than the working day is never booked")
    void jobLongerThanWorkingDayIsRefused() {
        // The day is nine hours; triage estimated ten.
        long breakdownId = newBreakdown("REPORTED", SKILL, CAPABILITY, 600, depotId);

        assertThrows(BusinessValidationException.class,
                () -> slotBookingService.bookBreakdownSlot(breakdownId, breakdownRequest()));
    }

    @Test
    @DisplayName("The estimated duration is derived from the skill's standard time and pinned")
    void durationIsDerivedFromSkillTimeAndPinned() {
        // OIL_SERVICE is seeded with a standard time of 60 minutes.
        long breakdownId = newBreakdown("REPORTED", SKILL, CAPABILITY, null, depotId);

        BookingResponse response =
                slotBookingService.bookBreakdownSlot(breakdownId, breakdownRequest());

        assertEquals(60, java.time.Duration.between(
                response.slotStart(), response.slotEnd()).toMinutes(),
                "the slot should be as long as the skill's standard time");

        entityManager.flush();
        Integer pinned = jdbcTemplate.queryForObject(
                "SELECT estimated_duration_minutes FROM breakdown_request WHERE id = ?",
                Integer.class, breakdownId);
        assertEquals(60, pinned,
                "the derived estimate is written back so a later edit to skill.time cannot move it");
    }

    @Test
    @DisplayName("A stored estimate wins over the skill's standard time")
    void storedEstimateOverridesSkillTime() {
        // OIL_SERVICE is 60 minutes as standard, but triage estimated 120.
        long breakdownId = newBreakdown("REPORTED", SKILL, CAPABILITY, 120, depotId);

        BookingResponse response =
                slotBookingService.bookBreakdownSlot(breakdownId, breakdownRequest());

        assertEquals(120, java.time.Duration.between(
                response.slotStart(), response.slotEnd()).toMinutes());
    }

    @Test
    @DisplayName("A breakdown with neither an estimate nor a skill must be triaged first")
    void untriagedBreakdownIsRefused() {
        long breakdownId = newBreakdown("REPORTED", null, null, null, depotId);

        BusinessValidationException error = assertThrows(BusinessValidationException.class,
                () -> slotBookingService.bookBreakdownSlot(breakdownId, breakdownRequest()));
        assertTrue(error.getMessage().contains("triage it before booking"),
                "message should ask for triage: " + error.getMessage());
    }

    @Test
    @DisplayName("A workshop in another depot cannot service the breakdown")
    void workshopMustServeTheReportingDepot() {
        Long otherDepotId = jdbcTemplate.queryForObject(
                "SELECT id FROM depot WHERE id <> ? ORDER BY id LIMIT 1", Long.class, depotId);
        long breakdownId = newBreakdown("REPORTED", SKILL, CAPABILITY, 60, otherDepotId);

        BusinessValidationException error = assertThrows(BusinessValidationException.class,
                () -> slotBookingService.bookBreakdownSlot(breakdownId, breakdownRequest()));
        assertTrue(error.getMessage().contains("cannot service a breakdown"),
                "message should name the depot mismatch: " + error.getMessage());
    }

    @Test
    @DisplayName("Any workshop inside the reporting depot may service the breakdown")
    void anyWorkshopInTheDepotIsAccepted() {
        // A second workshop in the same depot, fully equipped.
        jdbcTemplate.update(
                "INSERT INTO workshop (code, depot_id, time_zone) "
                        + "VALUES ('WS-BOOK-T2', ?, 'Asia/Kolkata')", depotId);
        Long secondWorkshopId = jdbcTemplate.queryForObject(
                "SELECT id FROM workshop WHERE code = 'WS-BOOK-T2'", Long.class);

        jdbcTemplate.update(
                "INSERT INTO service_bay (workshop_id, bay_code) VALUES (?, 'BAY-BOOK-T2')",
                secondWorkshopId);
        Long secondBayId = jdbcTemplate.queryForObject(
                "SELECT id FROM service_bay WHERE bay_code = 'BAY-BOOK-T2'", Long.class);
        jdbcTemplate.update(
                "INSERT INTO bay_capability (bay_id, capability_code) VALUES (?, ?)",
                secondBayId, CAPABILITY);
        for (int day = 1; day <= 5; day++) {
            jdbcTemplate.update(
                    "INSERT INTO working_calendar (workshop_id, day_of_week, open_time, close_time) "
                            + "VALUES (?, ?, TIME '08:00', TIME '17:00')", secondWorkshopId, day);
        }
        jdbcTemplate.update(
                "INSERT INTO app_user (username, password_hash, is_active) "
                        + "VALUES ('book-t-tech2','x',TRUE) ON CONFLICT (username) DO NOTHING");
        Long techUserId2 = jdbcTemplate.queryForObject(
                "SELECT id FROM app_user WHERE username = 'book-t-tech2'", Long.class);
        jdbcTemplate.update(
                "INSERT INTO technician (app_user_id, workshop_id, hourly_rate) VALUES (?,?,500)",
                techUserId2, secondWorkshopId);
        Long secondTechId = jdbcTemplate.queryForObject(
                "SELECT id FROM technician WHERE app_user_id = ?", Long.class, techUserId2);
        jdbcTemplate.update(
                "INSERT INTO technician_skill (technician_id, skill_code, valid_from) "
                        + "VALUES (?, ?, DATE '2020-01-01')", secondTechId, SKILL);

        long breakdownId = newBreakdown("REPORTED", SKILL, CAPABILITY, 60, depotId);

        BookingResponse response = slotBookingService.bookBreakdownSlot(
                breakdownId, new BookBreakdownSlotRequest(secondWorkshopId, MONDAY));

        assertEquals(secondBayId, response.bayId());
        assertEquals(secondWorkshopId, response.workshopId());
    }

    @Test
    @DisplayName("A plan that does not apply to the asset class is refused")
    void planMustApplyToTheAssetClass() {
        jdbcTemplate.update(
                "INSERT INTO maintenance_plan (code, time_interval_days, estimated_duration_minutes) "
                        + "VALUES ('BOOK_T_OTHER', 90, 60)");
        Long otherPlanId = jdbcTemplate.queryForObject(
                "SELECT id FROM maintenance_plan WHERE code = 'BOOK_T_OTHER'", Long.class);

        BookPreventiveSlotRequest wrongPlan =
                new BookPreventiveSlotRequest(assetId, otherPlanId, workshopId, MONDAY);

        BusinessValidationException error = assertThrows(BusinessValidationException.class,
                () -> slotBookingService.bookPreventiveSlot(wrongPlan));
        assertTrue(error.getMessage().contains("does not apply"));
    }

    @Test
    @DisplayName("A retired asset cannot be booked")
    void retiredAssetIsRefused() {
        jdbcTemplate.update("UPDATE asset SET status = 'RETIRED' WHERE id = ?", assetId);

        assertThrows(BusinessValidationException.class,
                () -> slotBookingService.bookPreventiveSlot(preventiveRequest()));
    }

    @Test
    @DisplayName("The same plan is not booked twice for one asset")
    void duplicatePreventiveBookingIsRefused() {
        slotBookingService.bookPreventiveSlot(preventiveRequest());

        assertThrows(DuplicateResourceException.class,
                () -> slotBookingService.bookPreventiveSlot(preventiveRequest()));
    }

    @Test
    @DisplayName("A breakdown already booked is not booked again")
    void duplicateBreakdownBookingIsRefused() {
        long breakdownId = newBreakdown("REPORTED");
        slotBookingService.bookBreakdownSlot(breakdownId, breakdownRequest());

        assertThrows(DuplicateResourceException.class,
                () -> slotBookingService.bookBreakdownSlot(breakdownId, breakdownRequest()));
    }

    @Test
    @DisplayName("A breakdown past triage can no longer be booked")
    void terminalBreakdownIsRefused() {
        long resolved = newBreakdown("RESOLVED");

        BusinessValidationException error = assertThrows(BusinessValidationException.class,
                () -> slotBookingService.bookBreakdownSlot(resolved, breakdownRequest()));
        assertTrue(error.getMessage().contains("can no longer be booked"));
    }

    @Test
    @DisplayName("An unknown breakdown is not found rather than booked")
    void unknownBreakdownIsNotFound() {
        assertThrows(ResourceNotFoundException.class,
                () -> slotBookingService.bookBreakdownSlot(999_999_999L, breakdownRequest()));
    }

    @Test
    @DisplayName("The database, not Java, refuses an overlapping booking on the same bay")
    void databaseRefusesOverlappingBooking() {
        BookingResponse first = slotBookingService.bookPreventiveSlot(preventiveRequest());
        long breakdownId = newBreakdown("REPORTED");

        // Insert straight over the held slot, bypassing the engine entirely.
        assertThrows(DataIntegrityViolationException.class, () ->
                        bookingRepository.insertBooking(
                                assetId, workshopId, bayId, technicianId,
                                first.slotStart(), first.slotEnd(),
                                "CORRECTIVE", null, breakdownId, "HELD"),
                "the exclusion constraint on (bay, slot) must reject this");
    }

    @Test
    @DisplayName("Only live bookings block a slot, so cancelling one frees it again")
    void cancelledBookingFreesTheSlot() {
        BookingResponse first = slotBookingService.bookPreventiveSlot(preventiveRequest());
        jdbcTemplate.update(
                "UPDATE booking SET status = 'CANCELLED' WHERE id = ?", first.bookingId());

        long breakdownId = newBreakdown("REPORTED");
        BookingResponse second =
                slotBookingService.bookBreakdownSlot(breakdownId, breakdownRequest());

        assertEquals(first.slotStart(), second.slotStart(),
                "the cancelled booking no longer occupies the slot");
    }

    @Test
    @DisplayName("A slot in the past is never booked")
    void pastSearchStartIsRefused() {
        LocalDate yesterday = LocalDate.now(java.time.Clock.systemUTC()).minusDays(1);

        BusinessValidationException error = assertThrows(BusinessValidationException.class,
                () -> slotBookingService.bookPreventiveSlot(new BookPreventiveSlotRequest(
                        assetId, planId, workshopId, yesterday)));

        assertTrue(error.getMessage().contains("past"),
                "message should say the date is in the past: " + error.getMessage());
    }

    @Test
    @DisplayName("A breakdown slot in the past is never booked either")
    void pastBreakdownSearchStartIsRefused() {
        LocalDate lastMonth = LocalDate.now(java.time.Clock.systemUTC()).minusMonths(1);
        long breakdownId = newBreakdown("REPORTED", SKILL, CAPABILITY, 60, depotId);

        assertThrows(BusinessValidationException.class,
                () -> slotBookingService.bookBreakdownSlot(
                        breakdownId, new BookBreakdownSlotRequest(workshopId, lastMonth)));
    }

    @Test
    @DisplayName("Today is still an acceptable search start")
    void todayIsAcceptable() {
        LocalDate today = LocalDate.now(java.time.Clock.systemUTC());

        // Whether a slot exists today depends on the weekday, but the request
        // must not be rejected merely for starting today.
        try {
            slotBookingService.bookPreventiveSlot(new BookPreventiveSlotRequest(
                    assetId, planId, workshopId, today));
        } catch (BusinessValidationException error) {
            assertTrue(error.getMessage().contains("No slot was booked"),
                    "today must not be refused as a past date: " + error.getMessage());
        }
    }

    @Test
    @DisplayName("The full chain works: raise a breakdown, then book the id it returns")
    void raiseThenBookChain() {
        // Step 1: the depot supervisor raises it. This is the step that was
        // missing, which left the booking endpoint unreachable through the API.
        var raised = breakdownIntakeService.raiseBreakdown(
                new com.example.backend.SLA.dto.breakdown.RaiseBreakdownRequest(
                        assetId,
                        com.example.backend.SLA.status.BreakdownPriority.P1,
                        "Engine will not start",
                        SKILL,
                        CAPABILITY,
                        null),
                reporterId);

        assertEquals(com.example.backend.SLA.status.BreakdownStatus.REPORTED, raised.status());
        assertEquals(60, raised.estimatedDurationMinutes(), "derived from the skill");

        // Step 2: the coordinator books the id that step 1 returned.
        BookingResponse booking = slotBookingService.bookBreakdownSlot(
                raised.id(), breakdownRequest());

        assertEquals("CORRECTIVE", booking.kind());
        assertEquals(raised.id(), booking.breakdownRequestId());
        assertEquals(60, java.time.Duration.between(
                booking.slotStart(), booking.slotEnd()).toMinutes(),
                "the duration pinned at intake drives the slot length");

        entityManager.flush();
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM breakdown_request WHERE id = ?", String.class, raised.id());
        assertEquals("BOOKED", status);
    }

    @Test
    @DisplayName("A booking is readable through JPA, including its range bounds")
    void bookingIsReadableThroughJpa() {
        BookingResponse response = slotBookingService.bookPreventiveSlot(preventiveRequest());

        var booking = bookingRepository.findById(response.bookingId()).orElseThrow();

        assertNotNull(booking.getSlotStart(), "lower(slot) must be readable");
        assertNotNull(booking.getSlotEnd(), "upper(slot) must be readable");
        assertTrue(booking.getSlotEnd().isAfter(booking.getSlotStart()));
        assertTrue(booking.isPreventive());

        List<com.example.backend.SLA.entity.Booking> live =
                bookingRepository.findLiveBookingsForAsset(assetId);
        assertEquals(1, live.size());
    }
}
