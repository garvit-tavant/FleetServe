-- Moves the DUE_SOON warning windows from maintenance_plan onto the
-- asset_class_plan pairing.
--
-- Rationale: one maintenance plan can attach to several asset classes. A
-- 10 000 km service may justify a 500 km warning on a van but a much wider one
-- on a heavy vehicle that is harder to get into a bay. The interval is a
-- property of the policy; the warning window is a property of applying that
-- policy to a particular class.
--
-- Cost of the move: a CHECK constraint cannot reference another table, so the
-- rule "the window must be shorter than the interval it qualifies" can no longer
-- be a CHECK. It is enforced by triggers instead, following the same pattern
-- already used for append-only protection in V9, so the guarantee stays in the
-- database rather than dropping to Java-only validation.

ALTER TABLE asset_class_plan
    ADD COLUMN due_soon_distance_km NUMERIC(12, 3),
    ADD COLUMN due_soon_days        INTEGER;

-- Carry existing windows across to every pairing that uses the plan, so this
-- migration does not change any current due-list answer.
UPDATE asset_class_plan acp
SET due_soon_distance_km = mp.due_soon_distance_km,
    due_soon_days        = mp.due_soon_days
FROM maintenance_plan mp
WHERE mp.id = acp.maintenance_plan_id;

ALTER TABLE maintenance_plan
    DROP CONSTRAINT ck_plan_due_soon_distance,
    DROP CONSTRAINT ck_plan_due_soon_days;

ALTER TABLE maintenance_plan
    DROP COLUMN due_soon_distance_km,
    DROP COLUMN due_soon_days;

-- Positivity is expressible as a CHECK because it needs no other table.
ALTER TABLE asset_class_plan
    ADD CONSTRAINT ck_asset_class_plan_due_soon_positive
        CHECK (
            (due_soon_distance_km IS NULL OR due_soon_distance_km > 0)
                AND (due_soon_days IS NULL OR due_soon_days > 0)
            );

-- A window only means something on an axis the plan actually measures, and it
-- must be shorter than the interval, otherwise every asset sits permanently
-- amber and the traffic light stops carrying information.
CREATE OR REPLACE FUNCTION validate_due_soon_window()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
DECLARE
    plan_distance NUMERIC(12, 3);
    plan_days     INTEGER;
BEGIN
    SELECT mp.distance_interval_km, mp.time_interval_days
    INTO plan_distance, plan_days
    FROM maintenance_plan mp
    WHERE mp.id = NEW.maintenance_plan_id;

    IF NEW.due_soon_distance_km IS NOT NULL THEN
        IF plan_distance IS NULL THEN
            RAISE EXCEPTION
                'Plan % has no distance interval, so a distance warning window is meaningless',
                NEW.maintenance_plan_id;
        END IF;
        IF NEW.due_soon_distance_km >= plan_distance THEN
            RAISE EXCEPTION
                'due_soon_distance_km (%) must be shorter than the plan distance interval (%)',
                NEW.due_soon_distance_km, plan_distance;
        END IF;
    END IF;

    IF NEW.due_soon_days IS NOT NULL THEN
        IF plan_days IS NULL THEN
            RAISE EXCEPTION
                'Plan % has no time interval, so a time warning window is meaningless',
                NEW.maintenance_plan_id;
        END IF;
        IF NEW.due_soon_days >= plan_days THEN
            RAISE EXCEPTION
                'due_soon_days (%) must be shorter than the plan time interval (%)',
                NEW.due_soon_days, plan_days;
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_asset_class_plan_due_soon
    BEFORE INSERT OR UPDATE
    ON asset_class_plan
    FOR EACH ROW
EXECUTE FUNCTION validate_due_soon_window();

-- The rule can also be broken from the other side, by shrinking an interval
-- under a window that already exists, so guard that direction too.
CREATE OR REPLACE FUNCTION validate_interval_against_due_soon()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
DECLARE
    offending INTEGER;
BEGIN
    SELECT count(*)
    INTO offending
    FROM asset_class_plan acp
    WHERE acp.maintenance_plan_id = NEW.id
      AND (
        (acp.due_soon_distance_km IS NOT NULL
            AND (NEW.distance_interval_km IS NULL
                OR acp.due_soon_distance_km >= NEW.distance_interval_km))
            OR (acp.due_soon_days IS NOT NULL
            AND (NEW.time_interval_days IS NULL
                OR acp.due_soon_days >= NEW.time_interval_days))
        );

    IF offending > 0 THEN
        RAISE EXCEPTION
            'Cannot change plan % : % existing due-soon window(s) would be left longer than the interval',
            NEW.id, offending;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_maintenance_plan_interval_guard
    BEFORE UPDATE
    ON maintenance_plan
    FOR EACH ROW
EXECUTE FUNCTION validate_interval_against_due_soon();
