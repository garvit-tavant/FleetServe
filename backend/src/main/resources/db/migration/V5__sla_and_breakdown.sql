CREATE TABLE sla_policy
(
    id                        BIGINT GENERATED ALWAYS AS IDENTITY,
    priority                  VARCHAR(20) NOT NULL,
    response_target_minutes   INTEGER     NOT NULL,
    resolution_target_minutes INTEGER     NOT NULL,
    calendar_basis            VARCHAR(20) NOT NULL,
    effective_from            DATE        NOT NULL,
    effective_to              DATE,

    CONSTRAINT pk_sla_policy PRIMARY KEY (id),

    CONSTRAINT uk_sla_policy_version
        UNIQUE (priority, effective_from),

    CONSTRAINT ck_sla_policy_priority
        CHECK
            (
            priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')
            ),

    CONSTRAINT ck_sla_response_target
        CHECK (response_target_minutes > 0),

    CONSTRAINT ck_sla_resolution_target
        CHECK
            (
            resolution_target_minutes >= response_target_minutes
            ),

    CONSTRAINT ck_sla_calendar_basis
        CHECK
            (
            calendar_basis IN ('CALENDAR_TIME', 'WORKING_TIME')
            ),

    CONSTRAINT ck_sla_effective_dates
        CHECK
            (
            effective_to IS NULL
                OR effective_to >= effective_from
            )
);

CREATE TABLE breakdown_request
(
    id             BIGINT GENERATED ALWAYS AS IDENTITY,
    asset_id       BIGINT       NOT NULL,
    depot_id       BIGINT       NOT NULL,
    reported_by_id BIGINT       NOT NULL,
    reported_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    priority       VARCHAR(20)  NOT NULL,
    description    VARCHAR(2000) NOT NULL,
    status         VARCHAR(30)  NOT NULL DEFAULT 'REPORTED',
    sla_policy_id  BIGINT       NOT NULL,
    -- No resulting_booking_id here: the breakdown <-> booking link is owned by
    -- booking.breakdown_request_id (see V6).

    CONSTRAINT pk_breakdown_request PRIMARY KEY (id),

    CONSTRAINT fk_breakdown_asset
        FOREIGN KEY (asset_id)
            REFERENCES asset (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_breakdown_depot
        FOREIGN KEY (depot_id)
            REFERENCES depot (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_breakdown_reported_by
        FOREIGN KEY (reported_by_id)
            REFERENCES app_user (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_breakdown_sla_policy
        FOREIGN KEY (sla_policy_id)
            REFERENCES sla_policy (id)
            ON DELETE RESTRICT,

    CONSTRAINT ck_breakdown_priority
        CHECK
            (
            priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')
            ),

    CONSTRAINT ck_breakdown_status
        CHECK
            (
            status IN
            (
             'REPORTED',
             'TRIAGED',
             'BOOKED',
             'IN_PROGRESS',
             'RESOLVED',
             'CANCELLED'
                )
            ),

    CONSTRAINT ck_breakdown_description_not_blank
        CHECK (btrim(description) <> '')
);


CREATE TABLE sla_checkpoint
(
  
    breakdown_request_id BIGINT       NOT NULL,
    responded_at        TIMESTAMPTZ  DEFAULT NULL,
    resolved_at         TIMESTAMPTZ  DEFAULT NULL,
    response_breach     BOOLEAN DEFAULT FALSE,
    resolution_breach    BOOLEAN DEFAULT FALSE,

    CONSTRAINT pk_sla_checkpoint PRIMARY KEY (breakdown_request_id),

    CONSTRAINT fk_sla_checkpoint_breakdown_request
        FOREIGN KEY (breakdown_request_id)
            REFERENCES breakdown_request (id)
            ON DELETE RESTRICT,


    CONSTRAINT ck_sla_checkpoint_responded_before_resolved
        CHECK
            (
                resolved_at IS NULL
                OR (responded_at IS NOT NULL AND resolved_at > responded_at)
            )
);


CREATE TABLE awaiting_raised
(   id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    breakdown_request_id BIGINT       NOT NULL,
    raised_at         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at         TIMESTAMPTZ  DEFAULT NULL,

    CONSTRAINT pk_awaiting_raised PRIMARY KEY (id),

    CONSTRAINT fk_awaiting_raised_breakdown_request
        FOREIGN KEY (breakdown_request_id)
            REFERENCES breakdown_request (id)
            ON DELETE RESTRICT,

    CONSTRAINT ck_awaiting_raised_resolved_after_raised
        CHECK (resolved_at IS NULL OR resolved_at > raised_at)
);