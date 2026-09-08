CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE booking
(
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    asset_id            BIGINT      NOT NULL,
    workshop_id         BIGINT      NOT NULL,
    bay_id              BIGINT      NOT NULL,
    technician_id       BIGINT      NOT NULL,
    slot                TSTZRANGE   NOT NULL,
    kind                VARCHAR(20) NOT NULL,
    maintenance_plan_id BIGINT,
    breakdown_request_id BIGINT,
    status              VARCHAR(30) NOT NULL DEFAULT 'HELD',
    version             BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT pk_booking PRIMARY KEY (id),

    CONSTRAINT fk_booking_asset
        FOREIGN KEY (asset_id)
            REFERENCES asset (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_workshop_id
        FOREIGN KEY (workshop_id)
            REFERENCES workshop (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_booking_bay
        FOREIGN KEY (bay_id)
            REFERENCES service_bay (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_booking_technician
        FOREIGN KEY (technician_id)
            REFERENCES technician (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_booking_plan
        FOREIGN KEY (maintenance_plan_id)
            REFERENCES maintenance_plan (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_booking_breakdown
        FOREIGN KEY (breakdown_request_id)
            REFERENCES breakdown_request (id)
            ON DELETE RESTRICT,

    CONSTRAINT ck_booking_kind
        CHECK (kind IN ('PREVENTIVE', 'CORRECTIVE')),

    CONSTRAINT ck_booking_status
        CHECK
            (
            status IN
            (
             'HELD',
             'CONFIRMED',
             'CANCELLED',
             'COMPLETED'
                )
            ),

    CONSTRAINT ck_booking_slot_non_empty
        CHECK
            (
            NOT isempty(slot)
                AND lower(slot) IS NOT NULL
                AND upper(slot) IS NOT NULL
                AND upper(slot) > lower(slot)
            ),

    -- Proposed team decision:
    -- Maximum individual booking duration = 24 hours.
    CONSTRAINT ck_booking_max_duration
        CHECK
            (
            upper(slot) - lower(slot) <= INTERVAL '24 hours'
),

    CONSTRAINT ck_booking_reference_by_kind
        CHECK
        (
            (
                kind = 'PREVENTIVE'
                AND maintenance_plan_id IS NOT NULL
                AND breakdown_request_id IS NULL
            )
            OR
            (
                kind = 'CORRECTIVE'
                AND maintenance_plan_id IS NULL
                AND breakdown_request_id IS NOT NULL
            )
        ),

    CONSTRAINT ck_booking_version
        CHECK (version >= 0)
);

ALTER TABLE booking
    ADD CONSTRAINT ex_booking_bay_slot
    EXCLUDE USING GIST
    (
        bay_id WITH =,
        slot WITH &&
    )
    WHERE
    (
        status IN ('HELD', 'CONFIRMED')
    );

ALTER TABLE booking
    ADD CONSTRAINT ex_booking_technician_slot
    EXCLUDE USING GIST
    (
        technician_id WITH =,
        slot WITH &&
    )
    WHERE
    (
        status IN ('HELD', 'CONFIRMED')
    );

-- The link between a breakdown and its booking is stored ONCE, on
-- booking.breakdown_request_id. A mirrored breakdown_request.resulting_booking_id
-- would duplicate the same fact and allow the two sides to disagree, so it is
-- deliberately not created.
--
-- A breakdown may be re-booked after a cancellation, so uniqueness is enforced
-- only over bookings that are still live.
CREATE UNIQUE INDEX uk_booking_active_breakdown
    ON booking (breakdown_request_id)
    WHERE breakdown_request_id IS NOT NULL
      AND status IN ('HELD', 'CONFIRMED', 'COMPLETED');


-- Keyed on booking_id, NOT breakdown_request_id: a repair job can originate
-- either from a breakdown request (corrective) or from a maintenance plan
-- (preventive), and both need the same clock tracking - responded/resolved
-- timestamps and awaiting-parts pauses - so that MTTR covers all repairs
-- rather than only breakdowns.
--
-- Response/resolution *breach* flags only ever apply to corrective bookings,
-- since SLA targets live on breakdown_request.sla_policy_id; for preventive
-- bookings they simply stay FALSE. The breakdown request (and therefore the
-- SLA policy) remains reachable via booking.breakdown_request_id when needed.
CREATE TABLE sla_checkpoint
(
    booking_id          BIGINT       NOT NULL,
    responded_at        TIMESTAMPTZ  DEFAULT NULL,
    resolved_at         TIMESTAMPTZ  DEFAULT NULL,
    response_breach     BOOLEAN      DEFAULT FALSE,
    resolution_breach   BOOLEAN      DEFAULT FALSE,

    -- Precomputed running total of all *closed* awaiting-parts/approval pauses,
    -- in minutes (per whatever calendar basis was applied at close time).
    -- Avoids re-summing a full pause-history table on every SLA check.
    accumulated_awaiting_minutes BIGINT NOT NULL DEFAULT 0,

    -- Start time of the currently open awaiting pause, or NULL if the job is
    -- not currently paused. Only one pause can be open at a time; raising
    -- again while already open is rejected by the application layer.
    last_awaiting_raised_at TIMESTAMPTZ DEFAULT NULL,

    CONSTRAINT pk_sla_checkpoint PRIMARY KEY (booking_id),

    CONSTRAINT fk_sla_checkpoint_booking
        FOREIGN KEY (booking_id)
            REFERENCES booking (id)
            ON DELETE RESTRICT,

    CONSTRAINT ck_sla_checkpoint_responded_before_resolved
        CHECK
            (
                resolved_at IS NULL
                OR (responded_at IS NOT NULL AND resolved_at > responded_at)
            ),

    CONSTRAINT ck_sla_checkpoint_accumulated_awaiting_non_negative
        CHECK (accumulated_awaiting_minutes >= 0)
);

