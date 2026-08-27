CREATE TABLE part
(
    id              BIGINT GENERATED ALWAYS AS IDENTITY,
    part_number     VARCHAR(100)  NOT NULL,
    description     VARCHAR(500)  NOT NULL,
    unit_of_measure VARCHAR(30)   NOT NULL,
    standard_cost   NUMERIC(12,2) NOT NULL,
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_part PRIMARY KEY (id),
    CONSTRAINT uk_part_number UNIQUE (part_number),

    CONSTRAINT ck_part_number_not_blank
        CHECK (btrim(part_number) <> ''),

    CONSTRAINT ck_part_description_not_blank
        CHECK (btrim(description) <> ''),

    CONSTRAINT ck_part_standard_cost
        CHECK (standard_cost >= 0)
);

CREATE TABLE part_reorder_level
(
    part_id       BIGINT         NOT NULL,
    workshop_id   BIGINT         NOT NULL,
    reorder_level NUMERIC(12,3) NOT NULL,

    CONSTRAINT pk_part_reorder_level
        PRIMARY KEY (part_id, workshop_id),

    CONSTRAINT fk_reorder_level_part
        FOREIGN KEY (part_id)
            REFERENCES part (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_reorder_level_workshop
        FOREIGN KEY (workshop_id)
            REFERENCES workshop (id)
            ON DELETE RESTRICT,

    CONSTRAINT ck_reorder_level
        CHECK (reorder_level >= 0)
);

CREATE TABLE inventory_movement
(
    id               BIGINT GENERATED ALWAYS AS IDENTITY,
    part_id          BIGINT         NOT NULL,
    workshop_id      BIGINT         NOT NULL,
    movement_type    VARCHAR(30)    NOT NULL,
    signed_quantity  NUMERIC(12,3) NOT NULL,
    unit_cost        NUMERIC(12,2) NOT NULL,
    work_order_id    BIGINT,
    transfer_reference VARCHAR(100),
    reason           VARCHAR(1000),
    actor_id         BIGINT         NOT NULL,
    occurred_at      TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_inventory_movement PRIMARY KEY (id),

    CONSTRAINT fk_inventory_movement_part
        FOREIGN KEY (part_id)
            REFERENCES part (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_inventory_movement_workshop
        FOREIGN KEY (workshop_id)
            REFERENCES workshop (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_inventory_movement_work_order
        FOREIGN KEY (work_order_id)
            REFERENCES work_order (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_inventory_movement_actor
        FOREIGN KEY (actor_id)
            REFERENCES app_user (id)
            ON DELETE RESTRICT,

    CONSTRAINT ck_inventory_movement_type
        CHECK
            (
            movement_type IN
            (
             'RECEIPT',
             'RETURN',
             'TRANSFER_IN',
             'ISSUE',
             'TRANSFER_OUT',
             'ADJUSTMENT'
                )
            ),

    CONSTRAINT ck_inventory_quantity_non_zero
        CHECK (signed_quantity <> 0),

    CONSTRAINT ck_inventory_quantity_sign
        CHECK
            (
            (
                movement_type IN
                (
                 'RECEIPT',
                 'RETURN',
                 'TRANSFER_IN'
                    )
                    AND signed_quantity > 0
                )
                OR
            (
                movement_type IN
                (
                 'ISSUE',
                 'TRANSFER_OUT'
                    )
                    AND signed_quantity < 0
                )
                OR movement_type = 'ADJUSTMENT'
            ),

    CONSTRAINT ck_inventory_adjustment_reason
        CHECK
            (
            movement_type <> 'ADJUSTMENT'
                OR
            (
                reason IS NOT NULL
                    AND btrim(reason) <> ''
                )
            ),

    CONSTRAINT ck_inventory_unit_cost
        CHECK (unit_cost >= 0),

    CONSTRAINT ck_inventory_reference
        CHECK
            (
            movement_type NOT IN ('ISSUE', 'RETURN')
                OR work_order_id IS NOT NULL
            ),

    CONSTRAINT ck_inventory_transfer_reference
        CHECK
            (
            movement_type NOT IN ('TRANSFER_IN', 'TRANSFER_OUT')
                OR
            (
                transfer_reference IS NOT NULL
                    AND btrim(transfer_reference) <> ''
                )
            )
);

CREATE TABLE work_order_part
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY,
    work_order_id BIGINT         NOT NULL,
    part_id       BIGINT         NOT NULL,
    quantity      NUMERIC(12,3) NOT NULL,
    unit_cost     NUMERIC(12,2) NOT NULL,
    movement_id   BIGINT         NOT NULL,

    CONSTRAINT pk_work_order_part PRIMARY KEY (id),
    CONSTRAINT uk_work_order_part_movement UNIQUE (movement_id),

    CONSTRAINT fk_work_order_part_order
        FOREIGN KEY (work_order_id)
            REFERENCES work_order (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_work_order_part_part
        FOREIGN KEY (part_id)
            REFERENCES part (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_work_order_part_movement
        FOREIGN KEY (movement_id)
            REFERENCES inventory_movement (id)
            ON DELETE RESTRICT,

    CONSTRAINT ck_work_order_part_quantity
        CHECK (quantity > 0),

    CONSTRAINT ck_work_order_part_unit_cost
        CHECK (unit_cost >= 0)
);

CREATE VIEW v_part_stock AS
SELECT
    part_id,
    workshop_id,
    COALESCE(SUM(signed_quantity), 0)::NUMERIC(12,3) AS on_hand
FROM inventory_movement
GROUP BY part_id, workshop_id;