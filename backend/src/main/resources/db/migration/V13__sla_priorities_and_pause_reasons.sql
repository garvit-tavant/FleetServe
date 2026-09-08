-- Aligns the service-level model with US-4.1.
--
-- 1. Priorities become P1, P2, P3.
-- 2. Response and resolution targets are seeded as configurable data, not constants.
-- 3. Pause intervals carry a reason, so AWAITING_PARTS and DEPOT_UNREACHABLE
--    (see docs/open-questions.md entry 12) are recorded distinctly while being
--    subtracted identically by the calendar arithmetic.

ALTER TABLE sla_policy
    DROP CONSTRAINT ck_sla_policy_priority;

ALTER TABLE breakdown_request
    DROP CONSTRAINT ck_breakdown_priority;

-- Existing rows, if any, are remapped rather than deleted so that policy
-- versions pinned onto historical breakdowns survive the change.
UPDATE sla_policy
SET priority = CASE priority
                   WHEN 'CRITICAL' THEN 'P1'
                   WHEN 'HIGH' THEN 'P1'
                   WHEN 'MEDIUM' THEN 'P2'
                   WHEN 'LOW' THEN 'P3'
                   ELSE priority
    END
WHERE priority IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW');

UPDATE breakdown_request
SET priority = CASE priority
                   WHEN 'CRITICAL' THEN 'P1'
                   WHEN 'HIGH' THEN 'P1'
                   WHEN 'MEDIUM' THEN 'P2'
                   WHEN 'LOW' THEN 'P3'
                   ELSE priority
    END
WHERE priority IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW');

ALTER TABLE sla_policy
    ADD CONSTRAINT ck_sla_policy_priority
        CHECK (priority IN ('P1', 'P2', 'P3'));

ALTER TABLE breakdown_request
    ADD CONSTRAINT ck_breakdown_priority
        CHECK (priority IN ('P1', 'P2', 'P3'));

-- Targets are data. Changing a target inserts a new effective-dated row; it
-- never rewrites an existing one, so history is not retrospectively altered.
INSERT INTO sla_policy
(priority, response_target_minutes, resolution_target_minutes, calendar_basis, effective_from)
VALUES ('P1', 60, 240, 'WORKING_TIME', DATE '2020-01-01'),
       ('P2', 240, 480, 'WORKING_TIME', DATE '2020-01-01'),
       ('P3', 480, 1440, 'WORKING_TIME', DATE '2020-01-01')
ON CONFLICT (priority, effective_from) DO NOTHING;

ALTER TABLE awaiting_raised
    ADD COLUMN pause_reason VARCHAR(30) NOT NULL DEFAULT 'AWAITING_PARTS';

ALTER TABLE awaiting_raised
    ALTER COLUMN pause_reason DROP DEFAULT;

ALTER TABLE awaiting_raised
    ADD CONSTRAINT ck_awaiting_raised_reason
        CHECK (pause_reason IN ('AWAITING_PARTS', 'DEPOT_UNREACHABLE'));

-- A breakdown must not accumulate two concurrent pauses for the same reason,
-- otherwise the same wall-clock interval would be subtracted twice.
CREATE UNIQUE INDEX uk_awaiting_raised_open_reason
    ON awaiting_raised (breakdown_request_id, pause_reason)
    WHERE resolved_at IS NULL;

-- Pause lookup is per breakdown on every clock evaluation.
CREATE INDEX idx_awaiting_raised_breakdown
    ON awaiting_raised (breakdown_request_id, raised_at);

-- The sweep selects breakdowns that are still running their clocks.
CREATE INDEX idx_breakdown_open_status
    ON breakdown_request (status)
    WHERE status NOT IN ('RESOLVED', 'CANCELLED');
