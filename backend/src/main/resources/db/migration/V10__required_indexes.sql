-- Schedule board:
-- workshop -> bay -> bookings overlapping a requested window.

CREATE INDEX idx_service_bay_workshop
    ON service_bay (workshop_id, id);

CREATE INDEX idx_booking_slot_gist
    ON booking USING GIST (slot);

-- Latest odometer reading per asset was already created in V4:
-- idx_odometer_asset_read_at_desc

-- Open work orders only.

CREATE INDEX idx_work_order_open_status
    ON work_order (status, booking_id)
    WHERE status IN
    (
        'SCHEDULED',
        'IN_PROGRESS',
        'AWAITING_PARTS'
    );

-- Supports grouping/filtering movement ledger by part and workshop.

CREATE INDEX idx_inventory_part_workshop
    ON inventory_movement (part_id, workshop_id);

-- Helps movement-history reads.

CREATE INDEX idx_inventory_workshop_time
    ON inventory_movement (workshop_id, occurred_at DESC);

CREATE INDEX idx_booking_asset
    ON booking (asset_id);

CREATE INDEX idx_booking_bay
    ON booking (bay_id);

CREATE INDEX idx_booking_technician
    ON booking (technician_id);

CREATE INDEX idx_technician_workshop
    ON technician (workshop_id);

CREATE INDEX idx_breakdown_asset_reported
    ON breakdown_request (asset_id, reported_at DESC);