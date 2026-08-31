package com.example.backend.AssetManagamentService.repository;

import com.example.backend.AssetManagamentService.entity.Asset;
import com.example.backend.AssetManagamentService.repository.projection.DueMaintenanceProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface DueMaintenanceRepository
        extends Repository<Asset, Long> {

    @Query(
            value = """
                    /*
                     * Add the final SQL after the Work Order service-history
                     * table/query contract is frozen.
                     *
                     * The query must join:
                     * asset
                     * asset_class
                     * asset_class_plan
                     * maintenance_plan
                     * latest odometer_reading
                     * last COMPLETED service for asset and plan
                     *
                     * It must derive:
                     * next_due_km
                     * next_due_date
                     * due_status
                     */
                    SELECT
                        a.id AS assetId,
                        a.vin AS vin,
                        ac.code AS assetClassCode,
                        mp.code AS maintenancePlanCode,
                        NULL::date AS nextDueDate,
                        NULL::numeric AS nextDueKm,
                        'OK' AS dueStatus
                    FROM asset a
                    JOIN asset_class ac
                      ON ac.id = a.asset_class_id
                    JOIN asset_class_plan acp
                      ON acp.asset_class_id = ac.id
                    JOIN maintenance_plan mp
                      ON mp.id = acp.maintenance_plan_id
                    WHERE a.status <> 'RETIRED'
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM asset a
                    JOIN asset_class ac
                      ON ac.id = a.asset_class_id
                    JOIN asset_class_plan acp
                      ON acp.asset_class_id = ac.id
                    JOIN maintenance_plan mp
                      ON mp.id = acp.maintenance_plan_id
                    WHERE a.status <> 'RETIRED'
                    """,
            nativeQuery = true
    )
    Page<DueMaintenanceProjection> findDueMaintenance(
            @Param("distanceSoonThreshold")
            BigDecimal distanceSoonThreshold,

            @Param("timeSoonThresholdDays")
            Integer timeSoonThresholdDays,

            Pageable pageable
    );
}