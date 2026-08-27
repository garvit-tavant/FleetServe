-- Remove old roles

DELETE FROM user_role;

DELETE FROM role;

-- Insert final business roles

INSERT INTO role
(
    code,
    name
)
VALUES
    (
        'FLEET_ADMINISTRATOR',
        'Fleet Administrator'
    ),
    (
        'MAINTENANCE_PLANNER',
        'Maintenance Planner'
    ),
    (
        'WORKSHOP_MANAGER',
        'Workshop Manager'
    ),
    (
        'SERVICE_COORDINATOR',
        'Service Coordinator'
    ),
    (
        'TECHNICIAN',
        'Technician'
    ),
    (
        'STOREKEEPER',
        'Storekeeper'
    ),
    (
        'DEPOT_SUPERVISOR',
        'Depot Supervisor'
    ),
    (
        'OPERATIONS_MANAGER',
        'Operations Manager'
    );