CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE booking
(
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    asset_id            BIGINT      NOT NULL,
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

ALTER TABLE breakdown_request
    ADD COLUMN resulting_booking_id BIGINT;

ALTER TABLE breakdown_request
    ADD CONSTRAINT uk_breakdown_resulting_booking
        UNIQUE (resulting_booking_id);

ALTER TABLE breakdown_request
    ADD CONSTRAINT fk_breakdown_resulting_booking
        FOREIGN KEY (resulting_booking_id)
            REFERENCES booking (id)
            ON DELETE RESTRICT;