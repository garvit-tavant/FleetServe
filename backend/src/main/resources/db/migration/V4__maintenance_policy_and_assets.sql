CREATE TABLE asset_class
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY,
    code        VARCHAR(50)  NOT NULL,
    description VARCHAR(255) NOT NULL,

    CONSTRAINT pk_asset_class PRIMARY KEY (id),
    CONSTRAINT uk_asset_class_code UNIQUE (code),
    CONSTRAINT ck_asset_class_code_not_blank
        CHECK (btrim(code) <> '')
);

CREATE TABLE maintenance_plan
(
    id                       BIGINT GENERATED ALWAYS AS IDENTITY,
    code                     VARCHAR(50) NOT NULL,
    distance_interval_km     NUMERIC(12,3),
    time_interval_days       INTEGER,
    estimated_duration_minutes INTEGER NOT NULL,
    required_skill_code      VARCHAR(50),
    required_capability_code VARCHAR(50),

    CONSTRAINT pk_maintenance_plan PRIMARY KEY (id),
    CONSTRAINT uk_maintenance_plan_code UNIQUE (code),

    CONSTRAINT fk_plan_skill
        FOREIGN KEY (required_skill_code)
            REFERENCES skill (skill_code)
            ON DELETE RESTRICT,

    CONSTRAINT fk_plan_capability
        FOREIGN KEY (required_capability_code)
            REFERENCES capability (capability_code)
            ON DELETE RESTRICT,

    CONSTRAINT ck_plan_interval_present
        CHECK
            (
            distance_interval_km IS NOT NULL
                OR time_interval_days IS NOT NULL
            ),

    CONSTRAINT ck_plan_distance_interval
        CHECK
            (
            distance_interval_km IS NULL
                OR distance_interval_km > 0
            ),

    CONSTRAINT ck_plan_time_interval
        CHECK
            (
            time_interval_days IS NULL
                OR time_interval_days > 0
            ),

    CONSTRAINT ck_plan_duration
        CHECK (estimated_duration_minutes > 0)
);

CREATE TABLE asset_class_plan
(
    id BIGINT GENERATED ALWAYS AS IDENTITY,

    asset_class_id BIGINT NOT NULL,

    maintenance_plan_id BIGINT NOT NULL,

    CONSTRAINT pk_asset_class_plan
        PRIMARY KEY (id),

    CONSTRAINT uk_asset_class_plan
        UNIQUE (asset_class_id, maintenance_plan_id),

    CONSTRAINT fk_asset_class_plan_class
        FOREIGN KEY (asset_class_id)
            REFERENCES asset_class(id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_asset_class_plan_plan
        FOREIGN KEY (maintenance_plan_id)
            REFERENCES maintenance_plan(id)
            ON DELETE RESTRICT
);

CREATE TABLE asset
(
    id                     BIGINT GENERATED ALWAYS AS IDENTITY,
    vin                    VARCHAR(100)  NOT NULL,
    asset_class_id         BIGINT        NOT NULL,
    home_depot_id          BIGINT        NOT NULL,
    acquisition_date       DATE          NOT NULL,
    acquisition_odometer_km NUMERIC(12,3) NOT NULL,
    status                 VARCHAR(30)   NOT NULL,
    version                BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT pk_asset PRIMARY KEY (id),
    CONSTRAINT uk_asset_vin UNIQUE (vin),

    CONSTRAINT fk_asset_class
        FOREIGN KEY (asset_class_id)
            REFERENCES asset_class (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_asset_home_depot
        FOREIGN KEY (home_depot_id)
            REFERENCES depot (id)
            ON DELETE RESTRICT,

    CONSTRAINT ck_asset_acquisition_odometer
        CHECK (acquisition_odometer_km >= 0),

    CONSTRAINT ck_asset_status
        CHECK
            (
            status IN
            (
             'ACTIVE',
             'IN_SERVICE',
             'OUT_OF_SERVICE',
             'RETIRED'
                )
            ),

    CONSTRAINT ck_asset_version
        CHECK (version >= 0)
);

CREATE TABLE odometer_reading
(
    id             BIGINT GENERATED ALWAYS AS IDENTITY,
    asset_id       BIGINT        NOT NULL,
    reading_km     NUMERIC(12,3) NOT NULL,
    read_at        TIMESTAMPTZ   NOT NULL,
    source         VARCHAR(30)   NOT NULL,
    recorded_by_id BIGINT        NOT NULL,

    CONSTRAINT pk_odometer_reading PRIMARY KEY (id),

    CONSTRAINT fk_odometer_asset
        FOREIGN KEY (asset_id)
            REFERENCES asset (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_odometer_recorded_by
        FOREIGN KEY (recorded_by_id)
            REFERENCES app_user (id)
            ON DELETE RESTRICT,

    CONSTRAINT ck_odometer_reading
        CHECK ( reading_km >= 0 ),                                                                                                                              
    CONSTRAINT ck_odometer_source
        CHECK
            (
            source IN
            (
             'MANUAL',
             'TELEMATICS',
             'SERVICE',
             'IMPORT'
                )
            )
);

CREATE INDEX idx_odometer_asset_read_at_desc
    ON odometer_reading (asset_id, read_at DESC);