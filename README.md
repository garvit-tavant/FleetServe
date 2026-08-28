# FleetServe

## What This System Does

FleetServe is a fleet maintenance and workshop management platform. It maintains the vehicle asset register, derives preventive maintenance due dates, schedules jobs into constrained workshop capacity (bays and technicians), tracks work orders through execution, manages spare-parts inventory via an append-only ledger, and measures breakdown service levels (SLA) and reporting metrics such as MTTR and cost per asset.

## Setup

_(backend skeleton in progress)_

## Architecture Diagram

```mermaid
---
config:
  theme: base
  layout: dagre
  themeVariables:
    lineColor: "#8A94A6"
    edgeLabelBackground: "#ffffff"
    fontSize: "14px"
---
flowchart TB

    A["User Login"] --> B["Role Based Dashboard"]

    B --> FA["Fleet Administrator"] & MP["Maintenance Planner"] & WM["Workshop Manager"] & SC["Service Coordinator"] & T["Technician"] & SK["Storekeeper"] & DS["Depot Supervisor"] & OM["Operations Manager"]

    FA --> FA1["Register Asset"] & FA2["Update Odometer"] & FA3["Retire Asset"]
    FA1 --> CORE1["Asset Repository"]
    FA2 --> CORE1

    MP --> MP1["Create Maintenance Plan"] & MP2["View Due List"] & MP3["View Overdue List"]
    MP1 --> DUE["Maintenance Engine"]
    CORE1 --> DUE
    DUE --> STATUS["OK / DUE_SOON / OVERDUE"]

    WM --> WM1["Manage Workshops"] & WM2["Manage Bays"] & WM3["Manage Technicians"] & WM4["Manage Skills"] & WM5["Manage Calendar"]
    WM1 --> CAPACITY["Workshop Capacity"]
    WM2 --> CAPACITY
    WM3 --> CAPACITY
    WM4 --> CAPACITY
    WM5 --> CAPACITY

    STATUS --> SC
    DS --> BD1["Raise Breakdown"]
    BD1 --> SC & SLA["SLA Engine"]

    SC --> SC1["Find Earliest Slots"]
    SC1 --> SLOT["Feasible Slot Engine"]
    CAPACITY --> SLOT
    SLOT --> SC2["Suggested Slots"]
    SC2 --> SC3["Create Booking"]
    SC3 --> SC4["Reserve Bay"] & SC5["Reserve Technician"] & SC6["Reserve Parts"]
    SC4 --> WO["Create Work Order"]
    SC5 --> WO
    SC6 --> WO

    WO --> T
    T --> T1["View Assigned Work Orders"]
    T1 --> T2["Start Job"]
    T2 --> W1["SCHEDULED"]
    W1 --> W2["IN_PROGRESS"]
    W2 --> DECIDE{"Need Additional Parts?"}
    DECIDE -- No --> W5["Continue Work"]
    DECIDE -- Yes --> W3["AWAITING_PARTS"]
    W3 --> SK

    SK --> SK1["Receive Parts"] & SK2["Transfer Stock"] & SK3["Issue Parts"] & SK4["Return Parts"] & SK5["Adjust Inventory"] & SK6["Reorder Alert"]
    SK3 --> W4["Parts Available Again"]
    W4 --> W2

    W5 --> W6["COMPLETED"]
    W6 --> COST["Calculate Cost"] & REPORTS["Operational Reporting"]
    COST --> NEXT["Recalculate Next Due"]
    NEXT --> STATUS

    REPORTS --> OM
    OM --> OM1["SLA Compliance"] & OM2["MTTR"] & OM3["Maintenance Analytics"] & OM4["Cost Per Asset"]

    SLA --> WO

    classDef entry fill:#0F4C81,color:#FFFFFF,stroke:#FFFFFF,stroke-width:2px
    classDef role fill:#2E5C8A,color:#FFFFFF,stroke:#FFFFFF,stroke-width:2px
    classDef action fill:#4A78A8,color:#FFFFFF,stroke:#FFFFFF,stroke-width:1.5px
    classDef core fill:#8E44AD,color:#FFFFFF,stroke:#FFFFFF,stroke-width:1.5px
    classDef decision fill:#D6A700,color:#1A1300,stroke:#1A1300,stroke-width:1.5px
    classDef state fill:#27AE60,color:#FFFFFF,stroke:#FFFFFF,stroke-width:1.5px
    classDef report fill:#C0392B,color:#FFFFFF,stroke:#FFFFFF,stroke-width:1.5px

    class A,B entry
    class FA,MP,WM,SC,T,SK,DS,OM role
    class FA1,FA2,FA3,MP1,MP2,MP3,WM1,WM2,WM3,WM4,WM5 action
    class SC1,SC2,SC3,SC4,SC5,SC6,BD1,T1,T2 action
    class SK1,SK2,SK3,SK4,SK5,SK6 action
    class CORE1,DUE,CAPACITY,SLOT,SLA,WO,COST,NEXT,REPORTS core
    class STATUS,DECIDE decision
    class W1,W2,W3,W4,W5,W6 state
    class OM1,OM2,OM3,OM4 report
```
