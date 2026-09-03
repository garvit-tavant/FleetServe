CREATE TABLE depot
(
    id        BIGINT GENERATED ALWAYS AS IDENTITY,
    code      VARCHAR(30)  NOT NULL,
    region    VARCHAR(100) NOT NULL,
    is_active BOOLEAN      NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_depot PRIMARY KEY (id),
    CONSTRAINT uk_depot_code UNIQUE (code),
    CONSTRAINT ck_depot_code_not_blank
        CHECK (btrim(code) <> ''),
    CONSTRAINT ck_depot_region_not_blank
        CHECK (btrim(region) <> '')
);

CREATE TABLE workshop
(
    id        BIGINT GENERATED ALWAYS AS IDENTITY,
    code      VARCHAR(30)  NOT NULL,
    depot_id  BIGINT       NOT NULL,
    time_zone VARCHAR(100) NOT NULL,
    is_active BOOLEAN      NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_workshop PRIMARY KEY (id),
    CONSTRAINT uk_workshop_code UNIQUE (code),

    CONSTRAINT fk_workshop_depot
        FOREIGN KEY (depot_id)
            REFERENCES depot (id)
            ON DELETE RESTRICT,

    CONSTRAINT ck_workshop_code_not_blank
        CHECK (btrim(code) <> ''),
    CONSTRAINT ck_workshop_time_zone_not_blank
        CHECK (btrim(time_zone) <> '')
);

CREATE TABLE service_bay
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY,
    workshop_id BIGINT      NOT NULL,
    bay_code    VARCHAR(30) NOT NULL,
    is_active   BOOLEAN     NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_service_bay PRIMARY KEY (id),
    CONSTRAINT uk_service_bay_code
        UNIQUE (bay_code),

    CONSTRAINT fk_service_bay_workshop
        FOREIGN KEY (workshop_id)
            REFERENCES workshop (id)
            ON DELETE RESTRICT,

    CONSTRAINT ck_service_bay_code_not_blank
        CHECK (btrim(bay_code) <> '')
);

CREATE TABLE capability
(
    capability_code VARCHAR(50) NOT NULL,
    name            VARCHAR(100) NOT NULL,
    description     VARCHAR(500),
    version         BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_capability
        PRIMARY KEY (capability_code),

    CONSTRAINT uk_capability_name
        UNIQUE (name),

    CONSTRAINT ck_capability_code_not_blank
        CHECK (btrim(capability_code) <> ''),

    CONSTRAINT ck_capability_name_not_blank
        CHECK (btrim(name) <> '')
);

CREATE TABLE bay_capability
(
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    bay_id BIGINT NOT NULL,
    capability_code VARCHAR(50) NOT NULL,

    CONSTRAINT pk_bay_capability
        PRIMARY KEY (id),

    CONSTRAINT uk_bay_capability
        UNIQUE (bay_id, capability_code),

    CONSTRAINT fk_bay_capability_bay
        FOREIGN KEY (bay_id)
            REFERENCES service_bay(id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_bay_capability_capability
        FOREIGN KEY (capability_code)
            REFERENCES capability(capability_code)
            ON DELETE RESTRICT
);

CREATE TABLE skill
(
    skill_code  VARCHAR(50)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255) NOT NULL,
    time        BIGINT       NOT NULL,
    version         BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_skill PRIMARY KEY (skill_code),
    CONSTRAINT time_not_zero CHECK(time>0),
    CONSTRAINT ck_skill_code_not_blank
        CHECK (btrim(skill_code) <> ''),
    CONSTRAINT ck_skill_description_not_blank
        CHECK (btrim(description) <> ''),
    CONSTRAINT ck_skill_name_not_blank
        CHECK (btrim(name) <> '')
);

CREATE TABLE technician
(
    id           BIGINT GENERATED ALWAYS AS IDENTITY,
    app_user_id  BIGINT        NOT NULL,
    workshop_id  BIGINT        NOT NULL,
    hourly_rate  NUMERIC(12,2) NOT NULL,
    is_active    BOOLEAN       NOT NULL DEFAULT TRUE,
    version         BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_technician PRIMARY KEY (id),
    CONSTRAINT uk_technician_app_user UNIQUE (app_user_id),

    CONSTRAINT fk_technician_app_user
        FOREIGN KEY (app_user_id)
            REFERENCES app_user (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_technician_workshop
        FOREIGN KEY (workshop_id)
            REFERENCES workshop (id)
            ON DELETE RESTRICT,

    CONSTRAINT ck_technician_hourly_rate
        CHECK (hourly_rate >= 0)
);

CREATE TABLE technician_skill
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY,
    technician_id BIGINT      NOT NULL,
    skill_code    VARCHAR(50) NOT NULL,
    valid_from    DATE        NOT NULL,
    valid_to      DATE,

    CONSTRAINT pk_technician_skill PRIMARY KEY (id),

    CONSTRAINT uk_technician_skill_period
        UNIQUE (technician_id, skill_code, valid_from),

    CONSTRAINT fk_technician_skill_technician
        FOREIGN KEY (technician_id)
            REFERENCES technician (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_technician_skill_skill
        FOREIGN KEY (skill_code)
            REFERENCES skill (skill_code)
            ON DELETE RESTRICT,

    CONSTRAINT ck_technician_skill_validity
        CHECK (valid_to IS NULL OR valid_to >= valid_from)
);

CREATE TABLE working_calendar
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY,
    workshop_id BIGINT   NOT NULL,
    day_of_week SMALLINT NOT NULL,
    open_time   TIME     NOT NULL,
    close_time  TIME     NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_working_calendar PRIMARY KEY (id),

    CONSTRAINT uk_working_calendar_workshop_day
        UNIQUE (workshop_id, day_of_week),

    CONSTRAINT fk_working_calendar_workshop
        FOREIGN KEY (workshop_id)
            REFERENCES workshop (id)
            ON DELETE RESTRICT,

    CONSTRAINT ck_working_calendar_day
        CHECK (day_of_week BETWEEN 1 AND 7),

    CONSTRAINT ck_working_calendar_times
        CHECK (close_time > open_time)
);

CREATE TABLE holiday
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY,
    workshop_id BIGINT,
    holiday_date DATE         NOT NULL,
    description VARCHAR(255) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_holiday PRIMARY KEY (id),

    CONSTRAINT fk_holiday_workshop
        FOREIGN KEY (workshop_id)
            REFERENCES workshop (id)
            ON DELETE RESTRICT,

    CONSTRAINT ck_holiday_description_not_blank
        CHECK (btrim(description) <> '')
);

CREATE UNIQUE INDEX uk_holiday_workshop_date
    ON holiday (workshop_id, holiday_date)
    WHERE workshop_id IS NOT NULL;

CREATE UNIQUE INDEX uk_holiday_global_date
    ON holiday (holiday_date)
    WHERE workshop_id IS NULL;