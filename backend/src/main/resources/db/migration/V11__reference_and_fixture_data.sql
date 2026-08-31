INSERT INTO role (code, name)
VALUES
    ('ADMIN', 'Administrator'),
    ('WORKSHOP_MANAGER', 'Workshop Manager'),
    ('SERVICE_SCHEDULER', 'Service Scheduler'),
    ('TECHNICIAN', 'Technician'),
    ('STOREKEEPER', 'Storekeeper');

INSERT INTO skill (skill_code, name, time,description)
VALUES
    ('OIL_SERVICE', 'Oil Service', 60 ,'Oil and filter replacement'),
    ('BRAKE_SERVICE', 'Brake Service', 45 ,'Brake inspection and repair'),
    ('ENGINE_DIAGNOSTICS', 'Engine Diagnostics', 30 ,'Engine diagnostic work'),
    ('TYRE_SERVICE', 'Tyre Service', 60,'Tyre inspection and replacement');

INSERT INTO capability
(
    capability_code,
    name,
    description
)
VALUES
    ('GENERAL_SERVICE', 'General Service', 'Routine maintenance work'),
    ('BRAKE_REPAIR', 'Brake Repair', 'Brake system work'),
    ('ENGINE_REPAIR', 'Engine Repair', 'Engine diagnostic and repair work'),
    ('TYRE_REPLACEMENT', 'Tyre Replacement', 'Tyre replacement work');

INSERT INTO depot (code, region)
VALUES
    ('DEP-BLR-01', 'Bangalore North'),
    ('DEP-BLR-02', 'Bangalore South');

INSERT INTO workshop
(
    code,
    depot_id,
    time_zone
)
SELECT
    'WS-BLR-01',
    id,
    'Asia/Kolkata'
FROM depot
WHERE code = 'DEP-BLR-01';

INSERT INTO service_bay
(
    workshop_id,
    bay_code
)
SELECT
    id,
    'BAY-01'
FROM workshop
WHERE code = 'WS-BLR-01';

INSERT INTO service_bay
(
    workshop_id,
    bay_code
)
SELECT
    id,
    'BAY-02'
FROM workshop
WHERE code = 'WS-BLR-01';

INSERT INTO bay_capability
(
    bay_id,
    capability_code
)
SELECT
    sb.id,
    'GENERAL_SERVICE'
FROM service_bay sb
         JOIN workshop w
              ON w.id = sb.workshop_id
WHERE w.code = 'WS-BLR-01'
  AND sb.bay_code = 'BAY-01';

INSERT INTO bay_capability
(
    bay_id,
    capability_code
)
SELECT
    sb.id,
    'BRAKE_REPAIR'
FROM service_bay sb
         JOIN workshop w
              ON w.id = sb.workshop_id
WHERE w.code = 'WS-BLR-01'
  AND sb.bay_code = 'BAY-02';

INSERT INTO working_calendar
(
    workshop_id,
    day_of_week,
    open_time,
    close_time
)
SELECT
    w.id,
    days.day_number,
    TIME '09:00',
    TIME '18:00'
FROM workshop w
         CROSS JOIN
     (
         VALUES (1), (2), (3), (4), (5), (6)
     ) AS days(day_number)
WHERE w.code = 'WS-BLR-01';

INSERT INTO holiday
(
    workshop_id,
    holiday_date,
    description
)
SELECT
    id,
    DATE '2026-09-15',
    'Synthetic workshop holiday'
FROM workshop
WHERE code = 'WS-BLR-01';

INSERT INTO asset_class (code, description)
VALUES
    ('PASSENGER_CAR', 'Passenger car'),
    ('LIGHT_COMMERCIAL', 'Light commercial vehicle');

INSERT INTO maintenance_plan
(
    code,
    distance_interval_km,
    time_interval_days,
    estimated_duration_minutes,
    required_skill_code,
    required_capability_code
)
VALUES
    (
        'STANDARD_10K_SERVICE',
        10000,
        180,
        120,
        'OIL_SERVICE',
        'GENERAL_SERVICE'
    );

INSERT INTO asset_class_plan
(
    asset_class_id,
    maintenance_plan_id
)
SELECT
    ac.id,
    mp.id
FROM asset_class ac
         CROSS JOIN maintenance_plan mp
WHERE ac.code = 'PASSENGER_CAR'
  AND mp.code = 'STANDARD_10K_SERVICE'
    ON CONFLICT (asset_class_id, maintenance_plan_id)
DO NOTHING;

INSERT INTO asset
(
    vin,
    asset_class_id,
    home_depot_id,
    acquisition_date,
    acquisition_odometer_km,
    status
)
SELECT
    'SYNTHETIC-VIN-0001',
    ac.id,
    d.id,
    DATE '2025-01-15',
    0,
    'ACTIVE'
FROM asset_class ac
         CROSS JOIN depot d
WHERE ac.code = 'PASSENGER_CAR'
  AND d.code = 'DEP-BLR-01';

INSERT INTO part
(
    part_number,
    description,
    unit_of_measure,
    standard_cost
)
VALUES
    ('OIL-FILTER-001', 'Synthetic oil filter', 'EACH', 450.00),
    ('ENGINE-OIL-5W30', 'Synthetic engine oil', 'LITRE', 800.00);

INSERT INTO part_reorder_level
(
    part_id,
    workshop_id,
    reorder_level
)
SELECT
    p.id,
    w.id,
    10
FROM part p
         CROSS JOIN workshop w
WHERE w.code = 'WS-BLR-01';