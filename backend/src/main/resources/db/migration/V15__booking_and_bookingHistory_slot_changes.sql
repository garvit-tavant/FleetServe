-- ==========================================
-- 1. Drop constraints depending on slot
-- ==========================================

ALTER TABLE booking
DROP CONSTRAINT IF EXISTS ex_booking_bay_slot;

ALTER TABLE booking
DROP CONSTRAINT IF EXISTS ex_booking_technician_slot;

ALTER TABLE booking
DROP CONSTRAINT IF EXISTS ck_booking_slot_non_empty;

ALTER TABLE booking
DROP CONSTRAINT IF EXISTS ck_booking_max_duration;


-- ==========================================
-- 2. Add start/end columns
-- ==========================================

ALTER TABLE booking
    ADD COLUMN start_at TIMESTAMPTZ;

ALTER TABLE booking
    ADD COLUMN end_at TIMESTAMPTZ;


-- ==========================================
-- 3. Backfill existing rows
-- ==========================================

UPDATE booking
SET
    start_at = lower(slot),
    end_at   = upper(slot);


-- ==========================================
-- 4. Make start/end mandatory
-- ==========================================

ALTER TABLE booking
    ALTER COLUMN start_at SET NOT NULL;

ALTER TABLE booking
    ALTER COLUMN end_at SET NOT NULL;


-- ==========================================
-- 5. Remove old slot column
-- ==========================================

ALTER TABLE booking
DROP COLUMN slot;


-- ==========================================
-- 6. Recreate slot as generated column
-- ==========================================

ALTER TABLE booking
    ADD COLUMN slot TSTZRANGE
        GENERATED ALWAYS AS
            (
            tstzrange(
                    start_at,
                    end_at,
                    '[)'
            )
            ) STORED;


-- ==========================================
-- 7. Validation constraints
-- ==========================================

ALTER TABLE booking
    ADD CONSTRAINT ck_booking_interval
        CHECK (
            end_at > start_at
            );


ALTER TABLE booking
    ADD CONSTRAINT ck_booking_max_duration
        CHECK (
            end_at - start_at <= INTERVAL '24 hours'
    );


ALTER TABLE booking
    ADD CONSTRAINT ck_booking_slot_non_empty
        CHECK (
            NOT isempty(slot)
                AND lower(slot) IS NOT NULL
                AND upper(slot) IS NOT NULL
                AND upper(slot) > lower(slot)
            );


-- ==========================================
-- 8. Recreate exclusion constraints
-- ==========================================

ALTER TABLE booking
    ADD CONSTRAINT ex_booking_bay_slot
    EXCLUDE USING gist
(
    bay_id WITH =,
    slot WITH &&
)
WHERE (
    status = 'CONFIRMED'
);


ALTER TABLE booking
    ADD CONSTRAINT ex_booking_technician_slot
    EXCLUDE USING gist
(
    technician_id WITH =,
    slot WITH &&
)
WHERE (
    status = 'CONFIRMED'
);

ALTER TABLE booking_history
DROP CONSTRAINT IF EXISTS ck_booking_history_previous_slot;

ALTER TABLE booking_history
DROP CONSTRAINT IF EXISTS ck_booking_history_new_slot;

ALTER TABLE booking_history
DROP COLUMN IF EXISTS previous_slot;

ALTER TABLE booking_history
DROP COLUMN IF EXISTS new_slot;

ALTER TABLE booking_history
    ADD COLUMN previous_start_at TIMESTAMPTZ;

ALTER TABLE booking_history
    ADD COLUMN previous_end_at TIMESTAMPTZ;

ALTER TABLE booking_history
    ADD COLUMN new_start_at TIMESTAMPTZ;

ALTER TABLE booking_history
    ADD COLUMN new_end_at TIMESTAMPTZ;

ALTER TABLE booking_history
    ADD CONSTRAINT ck_booking_history_previous_interval
        CHECK (
            previous_start_at IS NULL
                OR previous_end_at IS NULL
                OR previous_end_at > previous_start_at
            );

ALTER TABLE booking_history
    ADD CONSTRAINT ck_booking_history_new_interval
        CHECK (
            new_start_at IS NULL
                OR new_end_at IS NULL
                OR new_end_at > new_start_at
            );