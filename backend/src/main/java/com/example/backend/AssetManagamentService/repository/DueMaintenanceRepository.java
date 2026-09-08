package com.example.backend.AssetManagamentService.repository;

import com.example.backend.AssetManagamentService.entity.Asset;
import com.example.backend.AssetManagamentService.repository.projection.DueMaintenanceProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DueMaintenanceRepository
        extends Repository<Asset, Long> {

    /*
     * INV-5: next-due for a maintenance plan is the earlier of the distance-based
     * and the time-based threshold, for every plan configuration.
     *
     * A plan carries a distance interval, a time interval, or both
     * (ck_plan_interval_present guarantees at least one). Whichever threshold is
     * reached first makes the asset due, so OVERDUE is an OR across the two, and
     * a plan with only one interval is judged on that interval alone.
     *
     * The baseline for both clocks is the last COMPLETED service of that asset
     * under that plan. A preventive booking carries maintenance_plan_id, so the
     * service history is work_order joined through booking. An asset never
     * serviced under the plan falls back to its acquisition date and odometer
     * (confirmed decision, see docs/open-questions.md).
     *
     * The DUE_SOON windows come from the asset_class_plan pairing rather than
     * from constants, because one plan can apply to several classes with
     * different scheduling realities. A caller may override them per request for
     * what-if planning.
     *
     * today is passed in rather than read as now() inside the query, so the due
     * list is testable against a fixed clock and running it twice returns the
     * same answer (INV-8).
     */
    String DUE_LIST_BODY = """
            WITH latest_odometer AS (
                SELECT DISTINCT ON (o.asset_id)
                       o.asset_id,
                       o.reading_km
                FROM odometer_reading o
                ORDER BY o.asset_id, o.read_at DESC, o.id DESC
            ),
            last_service AS (
                SELECT DISTINCT ON (wo.asset_id, b.maintenance_plan_id)
                       wo.asset_id,
                       b.maintenance_plan_id,
                       wo.odometer_at_service,
                       (wo.completed_at AT TIME ZONE 'UTC')::date AS serviced_on
                FROM work_order wo
                JOIN booking b
                  ON b.id = wo.booking_id
                WHERE wo.status = 'COMPLETED'
                  AND wo.completed_at IS NOT NULL
                  AND b.maintenance_plan_id IS NOT NULL
                ORDER BY wo.asset_id, b.maintenance_plan_id, wo.completed_at DESC, wo.id DESC
            ),
            base AS (
                SELECT
                    a.id    AS assetid,
                    a.vin   AS vin,
                    ac.code AS assetclasscode,
                    mp.code AS maintenanceplancode,
                    COALESCE(lo.reading_km, a.acquisition_odometer_km) AS currentodometerkm,
                    CASE
                        WHEN mp.distance_interval_km IS NULL THEN NULL
                        ELSE COALESCE(ls.odometer_at_service, a.acquisition_odometer_km)
                             + mp.distance_interval_km
                    END AS nextduekm,
                    CASE
                        WHEN mp.time_interval_days IS NULL THEN NULL
                        ELSE COALESCE(ls.serviced_on, a.acquisition_date)
                             + mp.time_interval_days
                    END AS nextduedate,
                    COALESCE(CAST(:distanceSoonOverride AS numeric),
                             acp.due_soon_distance_km) AS duesoondistancekm,
                    COALESCE(CAST(:daysSoonOverride AS integer),
                             acp.due_soon_days) AS duesoondays
                FROM asset a
                JOIN asset_class ac
                  ON ac.id = a.asset_class_id
                JOIN asset_class_plan acp
                  ON acp.asset_class_id = ac.id
                JOIN maintenance_plan mp
                  ON mp.id = acp.maintenance_plan_id
                LEFT JOIN latest_odometer lo
                  ON lo.asset_id = a.id
                LEFT JOIN last_service ls
                  ON ls.asset_id = a.id
                 AND ls.maintenance_plan_id = mp.id
                WHERE a.status <> 'RETIRED'
                  AND (CAST(:depotId AS bigint) IS NULL
                       OR a.home_depot_id = CAST(:depotId AS bigint))
                  AND (CAST(:assetClassCode AS varchar) IS NULL
                       OR ac.code = CAST(:assetClassCode AS varchar))
            ),
            classified AS (
                SELECT
                    b.*,
                    CASE WHEN b.nextduekm IS NULL THEN NULL
                         ELSE b.nextduekm - b.currentodometerkm END AS kmremaining,
                    CASE WHEN b.nextduedate IS NULL THEN NULL
                         ELSE b.nextduedate - CAST(:today AS date) END AS daysremaining,
                    CASE
                        WHEN (b.nextduekm IS NOT NULL AND b.currentodometerkm >= b.nextduekm)
                          OR (b.nextduedate IS NOT NULL AND CAST(:today AS date) >= b.nextduedate)
                            THEN 0
                        WHEN (b.nextduekm IS NOT NULL
                              AND b.duesoondistancekm IS NOT NULL
                              AND b.nextduekm - b.currentodometerkm <= b.duesoondistancekm)
                          OR (b.nextduedate IS NOT NULL
                              AND b.duesoondays IS NOT NULL
                              AND b.nextduedate - CAST(:today AS date) <= b.duesoondays)
                            THEN 1
                        ELSE 2
                    END AS urgency
                FROM base b
            ),
            labelled AS (
                SELECT
                    c.*,
                    CASE c.urgency
                        WHEN 0 THEN 'OVERDUE'
                        WHEN 1 THEN 'DUE_SOON'
                        ELSE 'OK'
                    END AS duestatus
                FROM classified c
            )
            """;

    String DUE_LIST_FILTER = """
            WHERE (CAST(:dueOnly AS boolean) = false OR l.urgency < 2)
              AND (CAST(:dueStatus AS varchar) IS NULL
                   OR l.duestatus = CAST(:dueStatus AS varchar))
            """;

    @Query(
            value = DUE_LIST_BODY + """
                    SELECT
                        l.assetid             AS assetId,
                        l.vin                 AS vin,
                        l.assetclasscode      AS assetClassCode,
                        l.maintenanceplancode AS maintenancePlanCode,
                        l.nextduedate         AS nextDueDate,
                        l.nextduekm           AS nextDueKm,
                        l.currentodometerkm   AS currentOdometerKm,
                        l.kmremaining         AS kmRemaining,
                        l.daysremaining       AS daysRemaining,
                        l.duestatus           AS dueStatus
                    FROM labelled l
                    """ + DUE_LIST_FILTER + """
                    ORDER BY
                        l.urgency,
                        l.daysremaining NULLS LAST,
                        l.kmremaining NULLS LAST,
                        l.assetid
                    """,
            countQuery = DUE_LIST_BODY + """
                    SELECT COUNT(*)
                    FROM labelled l
                    """ + DUE_LIST_FILTER,
            nativeQuery = true
    )
    Page<DueMaintenanceProjection> findDueMaintenance(
            @Param("today") LocalDate today,
            @Param("distanceSoonOverride") BigDecimal distanceSoonOverride,
            @Param("daysSoonOverride") Integer daysSoonOverride,
            @Param("depotId") Long depotId,
            @Param("assetClassCode") String assetClassCode,
            @Param("dueStatus") String dueStatus,
            @Param("dueOnly") boolean dueOnly,
            Pageable pageable
    );
}
