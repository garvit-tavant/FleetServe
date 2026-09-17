-- ==========================================================
-- Full test-data seed for manual/API testing of every module:
-- workshops/bays/technicians, assets, maintenance plans,
-- breakdowns, bookings (all statuses), work orders (all 5
-- statuses), labour, parts, inventory ledger, part requirements,
-- booking history.
-- Reuses V11 fixtures (depot DEP-BLR-01, workshop WS-BLR-01,
-- bays BAY-01/02, skills, capabilities, asset_class, plan,
-- asset SYNTHETIC-VIN-0001, parts) via SELECT ... WHERE code=...
-- Role code used for seeded users: TECHNICIAN (survives V12 role reset).
-- ==========================================================

-- ---------- 1. Users + Technicians ----------

INSERT INTO app_user (username, password_hash)
VALUES
    ('seed.tech.one',   '$2a$10$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPA'),
    ('seed.tech.two',   '$2a$10$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPB'),
    ('seed.tech.three', '$2a$10$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPC'),
    ('seed.tech.four',  '$2a$10$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPD');

INSERT INTO user_role (app_user_id, role_code)
SELECT id, 'TECHNICIAN' FROM app_user
WHERE username IN ('seed.tech.one', 'seed.tech.two', 'seed.tech.three', 'seed.tech.four');

INSERT INTO technician (app_user_id, workshop_id, hourly_rate)
SELECT au.id, w.id, 500.00
FROM app_user au JOIN workshop w ON w.code = 'WS-BLR-01'
WHERE au.username = 'seed.tech.one';

INSERT INTO technician (app_user_id, workshop_id, hourly_rate)
SELECT au.id, w.id, 550.00
FROM app_user au JOIN workshop w ON w.code = 'WS-BLR-01'
WHERE au.username = 'seed.tech.two';

INSERT INTO technician (app_user_id, workshop_id, hourly_rate)
SELECT au.id, w.id, 600.00
FROM app_user au JOIN workshop w ON w.code = 'WS-BLR-01'
WHERE au.username = 'seed.tech.three';

INSERT INTO technician (app_user_id, workshop_id, hourly_rate)
SELECT au.id, w.id, 475.00
FROM app_user au JOIN workshop w ON w.code = 'WS-BLR-01'
WHERE au.username = 'seed.tech.four';

INSERT INTO technician_skill (technician_id, skill_code, valid_from)
SELECT t.id, 'OIL_SERVICE', DATE '2025-01-01'
FROM technician t JOIN app_user au ON au.id = t.app_user_id
WHERE au.username IN ('seed.tech.one', 'seed.tech.two', 'seed.tech.four');

INSERT INTO technician_skill (technician_id, skill_code, valid_from)
SELECT t.id, 'BRAKE_SERVICE', DATE '2025-01-01'
FROM technician t JOIN app_user au ON au.id = t.app_user_id
WHERE au.username = 'seed.tech.two';

INSERT INTO technician_skill (technician_id, skill_code, valid_from)
SELECT t.id, 'ENGINE_DIAGNOSTICS', DATE '2025-01-01'
FROM technician t JOIN app_user au ON au.id = t.app_user_id
WHERE au.username = 'seed.tech.three';

INSERT INTO technician_skill (technician_id, skill_code, valid_from)
SELECT t.id, 'TYRE_SERVICE', DATE '2025-01-01'
FROM technician t JOIN app_user au ON au.id = t.app_user_id
WHERE au.username = 'seed.tech.four';

-- ---------- 2. Additional assets ----------

INSERT INTO asset
(vin, asset_class_id, home_depot_id, acquisition_date, acquisition_odometer_km, status)
SELECT 'SYNTHETIC-VIN-0002', ac.id, d.id, DATE '2025-03-01', 5000, 'ACTIVE'
FROM asset_class ac JOIN depot d ON d.code = 'DEP-BLR-01'
WHERE ac.code = 'LIGHT_COMMERCIAL';

INSERT INTO asset
(vin, asset_class_id, home_depot_id, acquisition_date, acquisition_odometer_km, status)
SELECT 'SYNTHETIC-VIN-0003', ac.id, d.id, DATE '2024-11-10', 22000, 'ACTIVE'
FROM asset_class ac JOIN depot d ON d.code = 'DEP-BLR-01'
WHERE ac.code = 'PASSENGER_CAR';

INSERT INTO asset
(vin, asset_class_id, home_depot_id, acquisition_date, acquisition_odometer_km, status)
SELECT 'SYNTHETIC-VIN-0004', ac.id, d.id, DATE '2023-06-20', 41000, 'ACTIVE'
FROM asset_class ac JOIN depot d ON d.code = 'DEP-BLR-01'
WHERE ac.code = 'LIGHT_COMMERCIAL';

-- ---------- 3. Additional parts ----------

INSERT INTO part (part_number, description, unit_of_measure, standard_cost)
VALUES
    ('BRAKE-PAD-SET-001', 'Synthetic brake pad set', 'SET', 1200.00),
    ('TYRE-STD-205-55R16', 'Synthetic standard tyre 205/55R16', 'EACH', 4500.00);

INSERT INTO part_reorder_level (part_id, workshop_id, reorder_level)
SELECT p.id, w.id, 5
FROM part p CROSS JOIN workshop w
WHERE w.code = 'WS-BLR-01' AND p.part_number IN ('BRAKE-PAD-SET-001', 'TYRE-STD-205-55R16');

-- ---------- 4. SLA policies ----------

INSERT INTO sla_policy
(priority, response_target_minutes, resolution_target_minutes, calendar_basis, effective_from)
VALUES
    ('HIGH', 30, 240, 'CALENDAR_TIME', DATE '2025-01-01'),
    ('CRITICAL', 15, 120, 'CALENDAR_TIME', DATE '2025-01-01'),
    ('MEDIUM', 60, 480, 'WORKING_TIME', DATE '2025-01-01');

-- ---------- 5. Breakdown requests ----------

INSERT INTO breakdown_request
(asset_id, depot_id, reported_by_id, priority, description, status, sla_policy_id)
SELECT a.id, d.id, au.id, 'HIGH', 'Seed: engine warning light on dashboard', 'BOOKED', sp.id
FROM asset a JOIN depot d ON d.code = 'DEP-BLR-01'
JOIN app_user au ON au.username = 'seed.tech.one'
JOIN sla_policy sp ON sp.priority = 'HIGH'
WHERE a.vin = 'SYNTHETIC-VIN-0001';

INSERT INTO breakdown_request
(asset_id, depot_id, reported_by_id, priority, description, status, sla_policy_id)
SELECT a.id, d.id, au.id, 'CRITICAL', 'Seed: brake failure - awaiting parts scenario', 'BOOKED', sp.id
FROM asset a JOIN depot d ON d.code = 'DEP-BLR-01'
JOIN app_user au ON au.username = 'seed.tech.three'
JOIN sla_policy sp ON sp.priority = 'CRITICAL'
WHERE a.vin = 'SYNTHETIC-VIN-0002';

INSERT INTO breakdown_request
(asset_id, depot_id, reported_by_id, priority, description, status, sla_policy_id)
SELECT a.id, d.id, au.id, 'MEDIUM', 'Seed: unresolved breakdown, not yet booked', 'REPORTED', sp.id
FROM asset a JOIN depot d ON d.code = 'DEP-BLR-01'
JOIN app_user au ON au.username = 'seed.tech.four'
JOIN sla_policy sp ON sp.priority = 'MEDIUM'
WHERE a.vin = 'SYNTHETIC-VIN-0003';

-- ---------- 6. Bookings + Work Orders: SCHEDULED (PREVENTIVE) ----------

INSERT INTO booking
(asset_id, workshop_id, bay_id, technician_id, start_at, end_at, kind, maintenance_plan_id, status)
SELECT a.id, w.id, sb.id, t.id,
    (CURRENT_DATE + INTERVAL '1 day' + TIME '10:00')::TIMESTAMPTZ,
    (CURRENT_DATE + INTERVAL '1 day' + TIME '12:00')::TIMESTAMPTZ,
    'PREVENTIVE', mp.id, 'CONFIRMED'
FROM asset a
JOIN workshop w ON w.code = 'WS-BLR-01'
JOIN service_bay sb ON sb.workshop_id = w.id AND sb.bay_code = 'BAY-01'
JOIN technician t ON t.workshop_id = w.id
JOIN app_user au ON au.id = t.app_user_id AND au.username = 'seed.tech.one'
JOIN maintenance_plan mp ON mp.code = 'STANDARD_10K_SERVICE'
WHERE a.vin = 'SYNTHETIC-VIN-0001';

INSERT INTO sla_checkpoint (booking_id)
SELECT b.id FROM booking b JOIN asset a ON a.id = b.asset_id
WHERE a.vin = 'SYNTHETIC-VIN-0001' AND b.kind = 'PREVENTIVE' AND b.status = 'CONFIRMED';

INSERT INTO work_order (work_order_number, booking_id, status)
SELECT 'WO-SEED-SCHEDULED-001', b.id, 'SCHEDULED'
FROM booking b JOIN asset a ON a.id = b.asset_id
WHERE a.vin = 'SYNTHETIC-VIN-0001' AND b.kind = 'PREVENTIVE' AND b.status = 'CONFIRMED';

-- ---------- 7. Bookings + Work Orders: SCHEDULED (CORRECTIVE) ----------

INSERT INTO booking
(asset_id, workshop_id, bay_id, technician_id, start_at, end_at, kind, breakdown_request_id, status)
SELECT a.id, w.id, sb.id, t.id,
    (CURRENT_DATE + INTERVAL '1 day' + TIME '13:00')::TIMESTAMPTZ,
    (CURRENT_DATE + INTERVAL '1 day' + TIME '15:00')::TIMESTAMPTZ,
    'CORRECTIVE', br.id, 'CONFIRMED'
FROM asset a
JOIN workshop w ON w.code = 'WS-BLR-01'
JOIN service_bay sb ON sb.workshop_id = w.id AND sb.bay_code = 'BAY-02'
JOIN technician t ON t.workshop_id = w.id
JOIN app_user au ON au.id = t.app_user_id AND au.username = 'seed.tech.two'
JOIN breakdown_request br ON br.description = 'Seed: engine warning light on dashboard'
WHERE a.vin = 'SYNTHETIC-VIN-0001';

INSERT INTO sla_checkpoint (booking_id)
SELECT b.id FROM booking b
JOIN breakdown_request br ON br.id = b.breakdown_request_id
WHERE br.description = 'Seed: engine warning light on dashboard';

INSERT INTO work_order (work_order_number, booking_id, status)
SELECT 'WO-SEED-SCHEDULED-002', b.id, 'SCHEDULED'
FROM booking b
JOIN breakdown_request br ON br.id = b.breakdown_request_id
WHERE br.description = 'Seed: engine warning light on dashboard';

-- ---------- 8. IN_PROGRESS work order ----------

INSERT INTO booking
(asset_id, workshop_id, bay_id, technician_id, start_at, end_at, kind, maintenance_plan_id, status)
SELECT a.id, w.id, sb.id, t.id,
    (CURRENT_DATE + INTERVAL '2 days' + TIME '09:00')::TIMESTAMPTZ,
    (CURRENT_DATE + INTERVAL '2 days' + TIME '11:00')::TIMESTAMPTZ,
    'PREVENTIVE', mp.id, 'CONFIRMED'
FROM asset a
JOIN workshop w ON w.code = 'WS-BLR-01'
JOIN service_bay sb ON sb.workshop_id = w.id AND sb.bay_code = 'BAY-01'
JOIN technician t ON t.workshop_id = w.id
JOIN app_user au ON au.id = t.app_user_id AND au.username = 'seed.tech.three'
JOIN maintenance_plan mp ON mp.code = 'STANDARD_10K_SERVICE'
WHERE a.vin = 'SYNTHETIC-VIN-0002';

INSERT INTO sla_checkpoint (booking_id)
SELECT b.id FROM booking b JOIN asset a ON a.id = b.asset_id
WHERE a.vin = 'SYNTHETIC-VIN-0002' AND b.kind = 'PREVENTIVE';

INSERT INTO work_order (work_order_number, booking_id, status, started_at)
SELECT 'WO-SEED-IN-PROGRESS-001', b.id, 'IN_PROGRESS',
    (CURRENT_DATE + INTERVAL '2 days' + TIME '09:05')::TIMESTAMPTZ
FROM booking b JOIN asset a ON a.id = b.asset_id
WHERE a.vin = 'SYNTHETIC-VIN-0002' AND b.kind = 'PREVENTIVE';

-- ---------- 9. AWAITING_PARTS work order (with part requirement) ----------

INSERT INTO booking
(asset_id, workshop_id, bay_id, technician_id, start_at, end_at, kind, breakdown_request_id, status)
SELECT a.id, w.id, sb.id, t.id,
    (CURRENT_DATE + INTERVAL '2 days' + TIME '13:00')::TIMESTAMPTZ,
    (CURRENT_DATE + INTERVAL '2 days' + TIME '15:00')::TIMESTAMPTZ,
    'CORRECTIVE', br.id, 'CONFIRMED'
FROM asset a
JOIN workshop w ON w.code = 'WS-BLR-01'
JOIN service_bay sb ON sb.workshop_id = w.id AND sb.bay_code = 'BAY-02'
JOIN technician t ON t.workshop_id = w.id
JOIN app_user au ON au.id = t.app_user_id AND au.username = 'seed.tech.three'
JOIN breakdown_request br ON br.description = 'Seed: brake failure - awaiting parts scenario'
WHERE a.vin = 'SYNTHETIC-VIN-0002';

INSERT INTO sla_checkpoint (booking_id)
SELECT b.id FROM booking b
JOIN breakdown_request br ON br.id = b.breakdown_request_id
WHERE br.description = 'Seed: brake failure - awaiting parts scenario';

INSERT INTO work_order (work_order_number, booking_id, status, started_at)
SELECT 'WO-SEED-AWAITING-PARTS-001', b.id, 'AWAITING_PARTS',
    (CURRENT_DATE + INTERVAL '2 days' + TIME '13:05')::TIMESTAMPTZ
FROM booking b
JOIN breakdown_request br ON br.id = b.breakdown_request_id
WHERE br.description = 'Seed: brake failure - awaiting parts scenario';

INSERT INTO work_order_part_requirement
(work_order_id, part_id, quantity_required, reason, status)
SELECT wo.id, p.id, 2, 'Brake pads out of stock at time of inspection', 'PENDING'
FROM work_order wo JOIN part p ON p.part_number = 'BRAKE-PAD-SET-001'
WHERE wo.work_order_number = 'WO-SEED-AWAITING-PARTS-001';

-- ---------- 10. COMPLETED work order (with labour + parts + inventory) ----------

INSERT INTO booking
(asset_id, workshop_id, bay_id, technician_id, start_at, end_at, kind, maintenance_plan_id, status)
SELECT a.id, w.id, sb.id, t.id,
    (CURRENT_DATE - INTERVAL '3 days' + TIME '09:00')::TIMESTAMPTZ,
    (CURRENT_DATE - INTERVAL '3 days' + TIME '11:00')::TIMESTAMPTZ,
    'PREVENTIVE', mp.id, 'COMPLETED'
FROM asset a
JOIN workshop w ON w.code = 'WS-BLR-01'
JOIN service_bay sb ON sb.workshop_id = w.id AND sb.bay_code = 'BAY-01'
JOIN technician t ON t.workshop_id = w.id
JOIN app_user au ON au.id = t.app_user_id AND au.username = 'seed.tech.one'
JOIN maintenance_plan mp ON mp.code = 'STANDARD_10K_SERVICE'
WHERE a.vin = 'SYNTHETIC-VIN-0001';

INSERT INTO sla_checkpoint (booking_id, responded_at, resolved_at)
SELECT b.id,
    (CURRENT_DATE - INTERVAL '3 days' + TIME '09:05')::TIMESTAMPTZ,
    (CURRENT_DATE - INTERVAL '3 days' + TIME '10:55')::TIMESTAMPTZ
FROM booking b JOIN asset a ON a.id = b.asset_id
WHERE a.vin = 'SYNTHETIC-VIN-0001' AND b.status = 'COMPLETED';

INSERT INTO work_order
(work_order_number, booking_id, status, started_at, completed_at, odometer_at_service, total_cost)
SELECT 'WO-SEED-COMPLETED-001', b.id, 'COMPLETED',
    (CURRENT_DATE - INTERVAL '3 days' + TIME '09:05')::TIMESTAMPTZ,
    (CURRENT_DATE - INTERVAL '3 days' + TIME '10:55')::TIMESTAMPTZ,
    12500.5, 1700.00
FROM booking b JOIN asset a ON a.id = b.asset_id
WHERE a.vin = 'SYNTHETIC-VIN-0001' AND b.status = 'COMPLETED';

INSERT INTO work_order_labour (work_order_id, technician_id, hours, rate_applied)
SELECT wo.id, t.id, 1.5, t.hourly_rate
FROM work_order wo
JOIN booking b ON b.id = wo.booking_id
JOIN technician t ON t.id = b.technician_id
WHERE wo.work_order_number = 'WO-SEED-COMPLETED-001';

INSERT INTO inventory_movement (part_id, workshop_id, movement_type, signed_quantity, unit_cost, recorded_by)
SELECT p.id, w.id, 'RECEIPT', 50, 450.00, 'seed-script'
FROM part p JOIN workshop w ON w.code = 'WS-BLR-01'
WHERE p.part_number = 'OIL-FILTER-001';

INSERT INTO inventory_movement (part_id, workshop_id, movement_type, signed_quantity, unit_cost, recorded_by)
SELECT p.id, w.id, 'ISSUE', -1, 450.00, 'seed-script-completed-wo'
FROM part p JOIN workshop w ON w.code = 'WS-BLR-01'
WHERE p.part_number = 'OIL-FILTER-001';

INSERT INTO work_order_part (work_order_id, part_id, quantity, unit_cost, movement_id)
SELECT wo.id, p.id, 1, 450.00, im.id
FROM work_order wo
JOIN part p ON p.part_number = 'OIL-FILTER-001'
JOIN inventory_movement im ON im.recorded_by = 'seed-script-completed-wo' AND im.part_id = p.id
WHERE wo.work_order_number = 'WO-SEED-COMPLETED-001';

-- A second completed work order (different asset/technician/parts, for list variety)

INSERT INTO booking
(asset_id, workshop_id, bay_id, technician_id, start_at, end_at, kind, maintenance_plan_id, status)
SELECT a.id, w.id, sb.id, t.id,
    (CURRENT_DATE - INTERVAL '5 days' + TIME '14:00')::TIMESTAMPTZ,
    (CURRENT_DATE - INTERVAL '5 days' + TIME '16:00')::TIMESTAMPTZ,
    'PREVENTIVE', mp.id, 'COMPLETED'
FROM asset a
JOIN workshop w ON w.code = 'WS-BLR-01'
JOIN service_bay sb ON sb.workshop_id = w.id AND sb.bay_code = 'BAY-02'
JOIN technician t ON t.workshop_id = w.id
JOIN app_user au ON au.id = t.app_user_id AND au.username = 'seed.tech.four'
JOIN maintenance_plan mp ON mp.code = 'STANDARD_10K_SERVICE'
WHERE a.vin = 'SYNTHETIC-VIN-0004';

INSERT INTO sla_checkpoint (booking_id, responded_at, resolved_at)
SELECT b.id,
    (CURRENT_DATE - INTERVAL '5 days' + TIME '14:05')::TIMESTAMPTZ,
    (CURRENT_DATE - INTERVAL '5 days' + TIME '15:50')::TIMESTAMPTZ
FROM booking b JOIN asset a ON a.id = b.asset_id
WHERE a.vin = 'SYNTHETIC-VIN-0004' AND b.status = 'COMPLETED';

INSERT INTO work_order
(work_order_number, booking_id, status, started_at, completed_at, odometer_at_service, total_cost)
SELECT 'WO-SEED-COMPLETED-002', b.id, 'COMPLETED',
    (CURRENT_DATE - INTERVAL '5 days' + TIME '14:05')::TIMESTAMPTZ,
    (CURRENT_DATE - INTERVAL '5 days' + TIME '15:50')::TIMESTAMPTZ,
    41500, 2100.00
FROM booking b JOIN asset a ON a.id = b.asset_id
WHERE a.vin = 'SYNTHETIC-VIN-0004' AND b.status = 'COMPLETED';

INSERT INTO work_order_labour (work_order_id, technician_id, hours, rate_applied)
SELECT wo.id, t.id, 2.0, t.hourly_rate
FROM work_order wo
JOIN booking b ON b.id = wo.booking_id
JOIN technician t ON t.id = b.technician_id
WHERE wo.work_order_number = 'WO-SEED-COMPLETED-002';

INSERT INTO inventory_movement (part_id, workshop_id, movement_type, signed_quantity, unit_cost, recorded_by)
SELECT p.id, w.id, 'RECEIPT', 30, 4500.00, 'seed-script'
FROM part p JOIN workshop w ON w.code = 'WS-BLR-01'
WHERE p.part_number = 'TYRE-STD-205-55R16';

INSERT INTO inventory_movement (part_id, workshop_id, movement_type, signed_quantity, unit_cost, recorded_by)
SELECT p.id, w.id, 'ISSUE', -2, 4500.00, 'seed-script-completed-wo-002'
FROM part p JOIN workshop w ON w.code = 'WS-BLR-01'
WHERE p.part_number = 'TYRE-STD-205-55R16';

INSERT INTO work_order_part (work_order_id, part_id, quantity, unit_cost, movement_id)
SELECT wo.id, p.id, 2, 4500.00, im.id
FROM work_order wo
JOIN part p ON p.part_number = 'TYRE-STD-205-55R16'
JOIN inventory_movement im ON im.recorded_by = 'seed-script-completed-wo-002' AND im.part_id = p.id
WHERE wo.work_order_number = 'WO-SEED-COMPLETED-002';

-- ---------- 11. CANCELLED booking (no work order created) ----------

INSERT INTO booking
(asset_id, workshop_id, bay_id, technician_id, start_at, end_at, kind, maintenance_plan_id, status)
SELECT a.id, w.id, sb.id, t.id,
    (CURRENT_DATE + INTERVAL '5 days' + TIME '09:00')::TIMESTAMPTZ,
    (CURRENT_DATE + INTERVAL '5 days' + TIME '11:00')::TIMESTAMPTZ,
    'PREVENTIVE', mp.id, 'CANCELLED'
FROM asset a
JOIN workshop w ON w.code = 'WS-BLR-01'
JOIN service_bay sb ON sb.workshop_id = w.id AND sb.bay_code = 'BAY-01'
JOIN technician t ON t.workshop_id = w.id
JOIN app_user au ON au.id = t.app_user_id AND au.username = 'seed.tech.two'
JOIN maintenance_plan mp ON mp.code = 'STANDARD_10K_SERVICE'
WHERE a.vin = 'SYNTHETIC-VIN-0001';

INSERT INTO sla_checkpoint (booking_id)
SELECT b.id FROM booking b JOIN asset a ON a.id = b.asset_id
WHERE a.vin = 'SYNTHETIC-VIN-0001' AND b.status = 'CANCELLED';

INSERT INTO booking_history
(booking_id, action, actor_id, reason, previous_start_at, previous_end_at)
SELECT b.id, 'CANCELLED', au.id, 'Seed: customer requested cancellation', b.start_at, b.end_at
FROM booking b
JOIN app_user au ON au.username = 'seed.tech.two'
JOIN asset a ON a.id = b.asset_id
WHERE a.vin = 'SYNTHETIC-VIN-0001' AND b.status = 'CANCELLED';

-- ---------- 12. A second CANCELLED booking + a CANCELLED work order ----------

INSERT INTO booking
(asset_id, workshop_id, bay_id, technician_id, start_at, end_at, kind, maintenance_plan_id, status)
SELECT a.id, w.id, sb.id, t.id,
    (CURRENT_DATE + INTERVAL '6 days' + TIME '10:00')::TIMESTAMPTZ,
    (CURRENT_DATE + INTERVAL '6 days' + TIME '12:00')::TIMESTAMPTZ,
    'PREVENTIVE', mp.id, 'CANCELLED'
FROM asset a
JOIN workshop w ON w.code = 'WS-BLR-01'
JOIN service_bay sb ON sb.workshop_id = w.id AND sb.bay_code = 'BAY-02'
JOIN technician t ON t.workshop_id = w.id
JOIN app_user au ON au.id = t.app_user_id AND au.username = 'seed.tech.four'
JOIN maintenance_plan mp ON mp.code = 'STANDARD_10K_SERVICE'
WHERE a.vin = 'SYNTHETIC-VIN-0003';

INSERT INTO sla_checkpoint (booking_id)
SELECT b.id FROM booking b JOIN asset a ON a.id = b.asset_id
WHERE a.vin = 'SYNTHETIC-VIN-0003' AND b.status = 'CANCELLED';

INSERT INTO work_order (work_order_number, booking_id, status)
SELECT 'WO-SEED-CANCELLED-001', b.id, 'CANCELLED'
FROM booking b JOIN asset a ON a.id = b.asset_id
WHERE a.vin = 'SYNTHETIC-VIN-0003' AND b.status = 'CANCELLED';

INSERT INTO booking_history
(booking_id, action, actor_id, reason, previous_start_at, previous_end_at)
SELECT b.id, 'CANCELLED', au.id, 'Seed: workshop unavailable, rescheduled elsewhere', b.start_at, b.end_at
FROM booking b
JOIN app_user au ON au.username = 'seed.tech.four'
JOIN asset a ON a.id = b.asset_id
WHERE a.vin = 'SYNTHETIC-VIN-0003' AND b.status = 'CANCELLED';