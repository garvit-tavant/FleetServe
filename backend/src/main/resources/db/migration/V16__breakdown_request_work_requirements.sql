-- Breakdown triage now records what the job actually needs, so scheduling reads
-- it from the record rather than from whoever fills in the booking form.
--
-- The estimated duration is derived from skill.time, but it is stored rather
-- than looked up on every read. Pinning it follows the same reasoning as
-- pinning sla_policy_id at intake: editing a skill's standard time later must
-- not retrospectively change the duration of work already triaged and booked.

ALTER TABLE breakdown_request
    ADD COLUMN required_skill_code       VARCHAR(50),
    ADD COLUMN required_capability_code  VARCHAR(50),
    ADD COLUMN estimated_duration_minutes INTEGER;

ALTER TABLE breakdown_request
    ADD CONSTRAINT fk_breakdown_required_skill
        FOREIGN KEY (required_skill_code)
            REFERENCES skill (skill_code)
            ON DELETE RESTRICT;

ALTER TABLE breakdown_request
    ADD CONSTRAINT fk_breakdown_required_capability
        FOREIGN KEY (required_capability_code)
            REFERENCES capability (capability_code)
            ON DELETE RESTRICT;

-- A booking may not span two working days and a single booking is capped at
-- 24 hours (ck_booking_max_duration), so an estimate beyond that could never be
-- scheduled and is rejected at source.
ALTER TABLE breakdown_request
    ADD CONSTRAINT ck_breakdown_estimated_duration
        CHECK (
            estimated_duration_minutes IS NULL
                OR (estimated_duration_minutes > 0
                    AND estimated_duration_minutes <= 1440)
            );

-- Backfill any breakdown already triaged with a skill, so existing rows carry
-- the same standard time a new one would be given.
UPDATE breakdown_request b
SET estimated_duration_minutes = s.time
FROM skill s
WHERE s.skill_code = b.required_skill_code
  AND b.estimated_duration_minutes IS NULL
  AND s.time > 0
  AND s.time <= 1440;

-- A breakdown is serviced by a workshop in its own depot, so the booking path
-- joins breakdown -> depot -> workshop on every attempt.
CREATE INDEX idx_workshop_depot
    ON workshop (depot_id, id);
