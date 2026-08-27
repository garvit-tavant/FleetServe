CREATE TABLE booking_history
(
    id               BIGINT GENERATED ALWAYS AS IDENTITY,
    booking_id       BIGINT       NOT NULL,
    action           VARCHAR(50)  NOT NULL,
    previous_slot    TSTZRANGE,
    new_slot         TSTZRANGE,
    actor_id         BIGINT       NOT NULL,
    reason           VARCHAR(1000),
    occurred_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_booking_history PRIMARY KEY (id),

    CONSTRAINT fk_booking_history_booking
        FOREIGN KEY (booking_id)
            REFERENCES booking (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_booking_history_actor
        FOREIGN KEY (actor_id)
            REFERENCES app_user (id)
            ON DELETE RESTRICT,

    CONSTRAINT ck_booking_history_action
        CHECK
            (
            action IN
            (
            'CREATED',
            'CONFIRMED',
            'RESCHEDULED',
            'CANCELLED',
            'COMPLETED'
            )
),

    CONSTRAINT ck_booking_history_previous_slot
        CHECK
        (
            previous_slot IS NULL
            OR
            (
                NOT isempty(previous_slot)
                AND upper(previous_slot) > lower(previous_slot)
            )
        ),

    CONSTRAINT ck_booking_history_new_slot
        CHECK
        (
            new_slot IS NULL
            OR
            (
                NOT isempty(new_slot)
                AND upper(new_slot) > lower(new_slot)
            )
        )
);

CREATE TABLE work_order
(
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    work_order_number   VARCHAR(50)   NOT NULL,
    booking_id          BIGINT        NOT NULL,
    asset_id            BIGINT        NOT NULL,
    status              VARCHAR(30)   NOT NULL DEFAULT 'SCHEDULED',
    started_at          TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ,
    odometer_at_service NUMERIC(12,3),
    total_cost          NUMERIC(12,2) NOT NULL DEFAULT 0,
    idempotency_key     VARCHAR(100)  NOT NULL,
    version             BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT pk_work_order PRIMARY KEY (id),
    CONSTRAINT uk_work_order_number UNIQUE (work_order_number),
    CONSTRAINT uk_work_order_booking UNIQUE (booking_id),
    CONSTRAINT uk_work_order_idempotency_key UNIQUE (idempotency_key),

    CONSTRAINT fk_work_order_booking
        FOREIGN KEY (booking_id)
            REFERENCES booking (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_work_order_asset
        FOREIGN KEY (asset_id)
            REFERENCES asset (id)
            ON DELETE RESTRICT,

    CONSTRAINT ck_work_order_status
        CHECK
            (
            status IN
            (
             'SCHEDULED',
             'IN_PROGRESS',
             'AWAITING_PARTS',
             'COMPLETED',
             'CANCELLED'
                )
            ),

    CONSTRAINT ck_work_order_odometer
        CHECK
            (
            odometer_at_service IS NULL
                OR odometer_at_service >= 0
            ),

    CONSTRAINT ck_work_order_total_cost
        CHECK (total_cost >= 0),

    CONSTRAINT ck_work_order_time_order
        CHECK
            (
            completed_at IS NULL
                OR
            (
                started_at IS NOT NULL
                    AND completed_at >= started_at
                )
            ),

    CONSTRAINT ck_work_order_completed_fields
        CHECK
            (
            status <> 'COMPLETED'
                OR
            (
                started_at IS NOT NULL
                    AND completed_at IS NOT NULL
                    AND odometer_at_service IS NOT NULL
                )
            ),

    CONSTRAINT ck_work_order_version
        CHECK (version >= 0)
);

CREATE TABLE work_order_labour
(
    id             BIGINT GENERATED ALWAYS AS IDENTITY,
    work_order_id  BIGINT        NOT NULL,
    technician_id  BIGINT        NOT NULL,
    hours          NUMERIC(5,2)  NOT NULL,
    rate_applied   NUMERIC(12,2) NOT NULL,

    CONSTRAINT pk_work_order_labour PRIMARY KEY (id),

    CONSTRAINT fk_work_order_labour_order
        FOREIGN KEY (work_order_id)
            REFERENCES work_order (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_work_order_labour_technician
        FOREIGN KEY (technician_id)
            REFERENCES technician (id)
            ON DELETE RESTRICT,

    CONSTRAINT ck_work_order_labour_hours
        CHECK (hours > 0),

    CONSTRAINT ck_work_order_labour_rate
        CHECK (rate_applied >= 0)
);