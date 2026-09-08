-- US-1.3: the DUE_SOON thresholds must be configurable as data, not constants in
-- code. They live on the plan because the sensible warning window depends on the
-- interval it qualifies: a 10 000 km service and a 90 day inspection should not
-- share one amber window.
--
-- A caller may still override them per request for what-if planning, but the
-- stored value is the default.

ALTER TABLE maintenance_plan
    ADD COLUMN due_soon_distance_km NUMERIC(12, 3),
    ADD COLUMN due_soon_days        INTEGER;

-- Seed the values that were previously hard-coded in the controller, so this
-- migration preserves existing behaviour exactly.
UPDATE maintenance_plan
SET due_soon_distance_km = 500
WHERE distance_interval_km IS NOT NULL
  AND due_soon_distance_km IS NULL;

UPDATE maintenance_plan
SET due_soon_days = 14
WHERE time_interval_days IS NOT NULL
  AND due_soon_days IS NULL;

-- A warning window only makes sense on an axis the plan actually measures, and
-- it must be shorter than the interval itself or every asset is permanently amber.
ALTER TABLE maintenance_plan
    ADD CONSTRAINT ck_plan_due_soon_distance
        CHECK (
            due_soon_distance_km IS NULL
                OR (
                distance_interval_km IS NOT NULL
                    AND due_soon_distance_km > 0
                    AND due_soon_distance_km < distance_interval_km
                )
            );

ALTER TABLE maintenance_plan
    ADD CONSTRAINT ck_plan_due_soon_days
        CHECK (
            due_soon_days IS NULL
                OR (
                time_interval_days IS NOT NULL
                    AND due_soon_days > 0
                    AND due_soon_days < time_interval_days
                )
            );

-- The due list filters by depot, so support that lookup.
CREATE INDEX idx_asset_home_depot_status
    ON asset (home_depot_id, status);
