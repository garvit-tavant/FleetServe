CREATE TABLE work_order_part_requirement
(
    id BIGINT GENERATED ALWAYS AS IDENTITY,

    work_order_id BIGINT NOT NULL,

    part_id BIGINT NOT NULL,

    quantity_required NUMERIC(12,3) NOT NULL,

    reason VARCHAR(500),

    status VARCHAR(30) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL
                            DEFAULT CURRENT_TIMESTAMP,

    resolved_at TIMESTAMPTZ,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_work_order_part_requirement
        PRIMARY KEY (id),

    CONSTRAINT fk_wopr_work_order
        FOREIGN KEY (work_order_id)
            REFERENCES work_order(id),

    CONSTRAINT fk_wopr_part
        FOREIGN KEY (part_id)
            REFERENCES part(id),

    CONSTRAINT chk_wopr_qty
        CHECK (quantity_required > 0),

    CONSTRAINT chk_wopr_status
        CHECK (
            status IN
            (
             'PENDING',
             'RESOLVED'
                )
            )
);