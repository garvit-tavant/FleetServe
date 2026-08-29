1. Asset Management Service
Owns - Asset
    Asset Class
    Maintenance Plan
    Asset-Class-Plan
    Odometer Reading
    Due Calculation
Responsibilities: Register Asset
    Update Odometer
    Calculate Next Due
    Find Due Maintenance
    Retire Asset
    Reinstate Asset
Tables: asset
    asset_class
    maintenance_plan
    asset_class_plan
    odometer_reading

2. Capacity & Scheduling Service
Owns - Workshop
   Bay
   Capability
   Technician
   Skill
   Working Calendar
   Holiday
   Booking
Responsibilities:Find Slots
   Create Booking
   Reschedule Booking
   Cancel Booking
   Manage Calendars
Tables: workshop
service_bay
capability
bay_capability
    technician
    skill
    technician_skill
    working_calendar
    holiday
    booking
    booking_history

3. Work Order Service
Owns : Work Order LifeCycle
States: SCHEDULED
   IN_PROGRESS
   AWAITING_PARTS
   COMPLETED
   CANCELLED
Responsibilites:
   Create Work Order
   Start Work
   Await Parts
   Resume Work
   Complete Work
   Cancel Work
Tables:
    work_order
Owns : State Machine
   Transition Validation
   Idempotency

4. Inventory Service
Owns:
   Part
   Inventory Movement
   Part Reorder Level
   Stock View
Responsibilities:
   Receive Stock
   Transfer Stock
   Issue Part
   Return Part
   Adjust Stock
   Reorder Alerts
Tables:
   part
   part_reorder_level
   inventory_movement
   v_part_stock
Specification:
   Append-only ledger
   On-hand as a view
   No mutable quantity column

5. Execution Service
Owns : Labour
   Findings
   Part Usage
Responsibilites:
   Record Labour
   Record Findings
   Request Part
   Issue Part
   Capture Issue Cost
   Calculate Cost Breakdown
Tables:
   work_order_labour
   work_order_part
Here fits the technician parts ordering request
   Technician
   ↓
   Request Part
   ↓
   Inventory Service
   ↓
   ISSUE Movement
   ↓
   work_order_part

6. SLA & Breakdown Service
Owns :
   Breakdown Request
   SLA Policy
   SLA Calculations
Responsibilites:
   Raise Breakdown
   Apply SLA
   Pause SLA
   Resume SLA 
   Compliance Metrics
   MTTR Metrics
Tables:breakdown_request
   sla_policy

# **Architecture Design**
Asset Service
│
▼

Scheduling Service
│
▼

Booking
│
▼

Work Order Service
│
├──► Inventory Service
│
├──► Execution Service
│
└──► SLA Service

# Folder Structure

src/main/java/com/fleetserve/backend

├── common
│
│   ├── exception
│   ├── config
│   ├── response
│   └── util
│
├── security
│
│   ├── controller
│   ├── service
│   ├── repository
│   ├── entity
│   ├── dto
│   └── jwt
│
├── asset
│
│   ├── controller
│   ├── service
│   ├── repository
│   ├── entity
│   ├── dto
│   └── mapper
│
├── scheduling
│
│   ├── controller
│   ├── service
│   ├── repository
│   ├── entity
│   ├── dto
│   └── mapper
│
├── workorder
│
│   ├── controller
│   ├── service
│   ├── repository
│   ├── entity
│   ├── dto
│   └── mapper
│
├── inventory
│
│   ├── controller
│   ├── service
│   ├── repository
│   ├── entity
│   ├── dto
│   └── mapper
│
├── execution
│
│   ├── controller
│   ├── service
│   ├── repository
│   ├── entity
│   ├── dto
│   └── mapper
│
└── sla
    │
    ├── controller
    ├── service
    ├── repository
    ├── entity
    ├── dto
    └── mapper