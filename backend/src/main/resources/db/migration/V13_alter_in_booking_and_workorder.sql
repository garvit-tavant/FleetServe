ALTER TABLE booking
DROP CONSTRAINT IF EXISTS ck_booking_kind;

ALTER TABLE booking
    ADD CONSTRAINT ck_booking_kind
        CHECK (
            kind IN (
                     'PREVENTIVE',
                     'CORRECTIVE'
                )
            );

ALTER TABLE booking
DROP CONSTRAINT IF EXISTS ck_booking_status;

ALTER TABLE booking
    ADD CONSTRAINT ck_booking_status
        CHECK (
            status IN (
                       'HELD',
                       'CONFIRMED',
                       'CANCELLED',
                       'COMPLETED'
                )
            );

ALTER TABLE work_order
DROP CONSTRAINT IF EXISTS ck_work_order_status;

ALTER TABLE work_order
    ADD CONSTRAINT ck_work_order_status
        CHECK (
            status IN (
                       'SCHEDULED',
                       'IN_PROGRESS',
                       'AWAITING_PARTS',
                       'COMPLETED',
                       'CANCELLED'
                )
            );