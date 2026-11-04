# Application Architecture

## Layered component view

```mermaid
flowchart TB
    GUI["Swing GUI<br/>DashboardFrame"]
    SERVICE["Service layer<br/>ParkingService"]
    ALLOC["Allocation strategy<br/>NearestSlotStrategy"]
    PAYSTRAT["Payment strategies<br/>CashPayment / UpiPayment / CardPayment"]
    MODELS["Domain models<br/>Vehicle / Car / Bike / Truck<br/>ParkingSlot / ParkingTicket / Payment"]
    DS["In-memory working state<br/>HashMaps / ArrayDeque / ArrayLists"]
    DAOS["DAO layer<br/>VehicleDAO / ParkingSlotDAO<br/>ParkingTicketDAO / PaymentDAO<br/>ParkingHistoryDAO / WaitingQueueDAO"]
    JDBC["JDBC connection<br/>DBConnection"]
    DB[("MySQL<br/>smart_parking_test_db")]

    GUI -->|service calls and view refresh| SERVICE
    SERVICE --> ALLOC
    SERVICE --> PAYSTRAT
    SERVICE --> MODELS
    SERVICE <--> DS
    SERVICE --> DAOS
    DAOS --> JDBC
    JDBC --> DB
```

The GUI calls `ParkingService`; it contains no SQL. The service owns workflow rules and DSA working collections, DAOs contain parameterized SQL and row mapping, and `DBConnection` obtains a JDBC connection from local properties or environment variables. The payment strategies simulate acceptance and do not contact a payment processor.

## Parking workflow

```mermaid
flowchart LR
    A[Vehicle registration] --> B[Vehicle entry request]
    B --> C{Compatible slot available?}
    C -->|Yes| D[PriorityQueue selects nearest slot]
    D --> E[Transaction: mark occupied and insert active ticket]
    E --> F[Vehicle parks]
    F --> G[Vehicle exit request]
    G --> H[Calculate rounded parking fee]
    H --> I[Payment strategy accepts selected method]
    I --> J[Transaction: close ticket, insert payment and history, release slot]
    J --> K[Display and optionally print receipt]
    J --> L[Promote compatible waiting vehicle if present]
    C -->|No| M[Persist registration in waiting_queue]
```

Entry and exit changes that affect multiple rows use JDBC transactions. The in-memory state is updated after the database transaction succeeds.

## Waiting queue and recovery

```mermaid
flowchart TD
    FULL[No compatible slot] --> Q[Insert waiting_queue row]
    Q --> RESTART[Application startup / refresh]
    RESTART --> LOAD[Load vehicles, slot states, active tickets, history and queue order from MySQL]
    LOAD --> DSA[Rebuild HashMaps, slot list, ArrayDeque and history list]
    SLOT[Slot released] --> SCAN[Scan waiting order for first vehicle compatible with a free slot]
    DSA --> SCAN
    SCAN -->|Found| ASSIGN[Transactional slot claim and ticket insert; remove queue row]
    ASSIGN --> DSA
    SCAN -->|None fits| KEEP[Keep registrations waiting]
```

The queue is stored by `queue_id` and loaded in that order. Promotion considers the first queued vehicle that can use the newly available inventory; incompatible vehicles remain queued.

## Package responsibilities

| Package/class | Responsibility |
|---|---|
| `gui.DashboardFrame` | Swing tabs, user input, receipt view, and service-backed refresh |
| `service.ParkingService` | Registration and parking rules, fee calculation, transactions, recovery, DSA synchronization |
| `interfaces` and `strategy` | Pluggable allocation/payment behavior |
| `model` | Vehicle, slot, ticket, and payment domain values |
| `dao` | Prepared SQL, persistence, and database row mapping |
| `database.DBConnection` | Connection settings and JDBC connection creation |
| `Main` | Starts the Swing UI and reports startup connection errors |
