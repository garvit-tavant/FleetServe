CREATE TABLE role
(
    code        VARCHAR(50)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_role PRIMARY KEY (code),
    CONSTRAINT uk_role_name UNIQUE (name),
    CONSTRAINT ck_role_code_not_blank
        CHECK (btrim(code) <> ''),
    CONSTRAINT ck_role_name_not_blank
        CHECK (btrim(name) <> ''),
    CONSTRAINT ck_role_code_format
        CHECK (code ~ '^[A-Z][A-Z0-9_]*$')
    );

CREATE TABLE app_user
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY,
    username      VARCHAR(100) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_app_user PRIMARY KEY (id),
    CONSTRAINT uk_app_user_username UNIQUE (username),
    CONSTRAINT ck_app_user_username_not_blank
        CHECK (btrim(username) <> ''),
    CONSTRAINT ck_app_user_password_hash_not_blank
        CHECK (btrim(password_hash) <> '')
);

CREATE TABLE user_role
(
    app_user_id BIGINT      NOT NULL,
    role_code   VARCHAR(50) NOT NULL,

    CONSTRAINT pk_user_role
        PRIMARY KEY (app_user_id, role_code),

    CONSTRAINT fk_user_role_app_user
        FOREIGN KEY (app_user_id)
            REFERENCES app_user (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_user_role_role
        FOREIGN KEY (role_code)
            REFERENCES role (code)
            ON DELETE RESTRICT
);

CREATE INDEX idx_user_role_role_code
    ON user_role (role_code);