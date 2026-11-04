# Smart Parking Management System

### An intelligent parking management application built with Java, OOP, DSA, JDBC, and MySQL.

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Maven](https://img.shields.io/badge/Maven-3.9%2B-blue?logo=apachemaven)](https://maven.apache.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8%2B-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com/)
[![JUnit](https://img.shields.io/badge/JUnit-5-25A162?logo=junit5&logoColor=white)](https://junit.org/junit5/)
[![Build](https://img.shields.io/badge/build-locally%20verified-brightgreen)](#verification)

A desktop application for registering vehicles, allocating compatible parking slots, recording entry and exit, calculating fees, and keeping payment, history, and waiting-list data in MySQL. The project is a practical Java portfolio example of object-oriented design, Java collections, Swing, JDBC, DAO separation, and transactional writes.

**Verified locally:** `mvn clean package` succeeds; the opt-in MySQL workflow and unit tests passed in the configured environment. There is no CI workflow or hosted build badge configured.

## Contents

- [Overview](#overview)
- [Screenshots](#screenshots)
- [Features](#features)
- [Technology stack](#technology-stack)
- [OOP concepts](#oop-concepts)
- [Data structures and complexity](#data-structures-and-complexity)
- [Database design](#database-design)
- [Architecture](#architecture)
- [Project structure](#project-structure)
- [Installation and execution](#installation-and-execution)
- [Verification](#verification)
- [Future enhancements](#future-enhancements)
- [Author](#author)

## Overview

Parking operations often require staff to look up vehicles, find a slot that fits, track active stays, calculate a fee, and preserve records. This application brings those operations into one Swing dashboard. It selects the nearest available slot that fits a vehicle, queues a vehicle when no compatible slot is available, and records completed stays and payments in MySQL.

The technically interesting part is the combination of in-memory Java collections for fast application decisions and a relational database for durable state. The service layer coordinates the two: JDBC transactions persist slot and ticket changes together, while a startup reload rebuilds the working collections from database records.

## Screenshots

Authentic application screenshots are not included yet. The Swing window was not available to the desktop capture tool in this session, so no substitute images are presented as screenshots. Capture the following from the running application and save the PNG files under [`docs/screenshots/`](docs/screenshots/README.md):

| File | Screen to capture |
|---|---|
| `dashboard.png` | Overview with database-backed metrics |
| `vehicle-registration.png` | Vehicles tab with the registration form and vehicle table |
| `vehicle-search.png` | Vehicles tab showing a search/filter result |
| `parking-entry.png` | Entry & Exit tab before confirming an entry |
| `parking-exit.png` | Entry & Exit tab with the exit controls |
| `parking-slots.png` | Parking Slots tab with slot status |
| `waiting-queue.png` | Waiting Queue tab with a queued demo vehicle |
| `parking-history.png` | History tab with a completed visit |
| `payments.png` | Payments tab with a recorded payment |
| `receipt.png` | Completed receipt displayed in the entry/exit screen |

Use a local test database and clearly fictional vehicle details. Avoid capturing configuration files or credentials. See [`docs/screenshots/README.md`](docs/screenshots/README.md) for a manual capture checklist.

## Features

### Vehicle management

- Register Cars, Bikes, and Trucks with validated registration and owner values.
- Search/filter registered vehicles in the dashboard.
- Reject duplicate registration numbers.

### Smart parking allocation

- Find the nearest available compatible slot through `NearestSlotStrategy`.
- Slot size requirements are Bike 1, Car 2, Truck 3.
- Select by distance, then floor, then slot ID for deterministic tie-breaking.
- Persist slot occupancy with the active ticket.

### Parking operations

- Create an active ticket at entry and close it at exit.
- Round partial hours up, with a one-hour minimum, using `BigDecimal` rates: Bike 20.00, Car 40.00, Truck 80.00 per hour.
- Release a slot and promote a compatible waiting vehicle after exit.
- Display and print a receipt with ticket, vehicle, slot, timestamps, and fee.

### Payment management

- Select Cash, UPI, or Card through the `PaymentStrategy` interface.
- Store payment method, amount, status, and paid timestamp in MySQL.
- Browse recorded payments and view/print the related receipt.

These strategies simulate successful payments; they do not connect to banking or payment-provider APIs.

### History, queue, and dashboard

- Keep completed parking visits in persistent parking history.
- Persist the waiting list in queue order so it can be reconstructed after restart.
- Display slots, registered vehicles, active tickets, history, payments, waiting vehicles, availability, and total recorded revenue.
- Refresh dashboard data from the service layer, which reloads its persistent state from MySQL.

## Technology stack

| Technology | Purpose |
|---|---|
| Java 21 | Models, business rules, collections, and Swing UI |
| Swing | Desktop dashboard and workflow screens |
| Java Collections Framework | Priority queue, maps, queue, and record lists |
| JDBC | Parameterized SQL and transaction control |
| MySQL 8+ | Durable vehicle, slot, ticket, payment, history, and queue records |
| Maven | Dependency management, compile, test, and package lifecycle |
| JUnit 5 | Unit tests and opt-in MySQL workflow test |

## OOP concepts

| Concept | Actual implementation | Use in this project |
|---|---|---|
| Inheritance | Abstract `Vehicle`; concrete `Car`, `Bike`, `Truck` | Shares registration and owner state while each subtype supplies fit size and rate |
| Encapsulation | Private fields in `Vehicle` and `ParkingSlot`; checked setters/state methods | Protects validated vehicle data and slot availability changes |
| Abstraction | `AllocationStrategy`, `PaymentStrategy`; abstract `Vehicle` | Lets service code depend on behaviors and common vehicle operations |
| Polymorphism | `getType()`, `requiredSize()`, `hourlyRate()` overrides | A `Vehicle` reference provides subtype-specific allocation and billing values |
| Strategy pattern | `NearestSlotStrategy`; `CashPayment`, `UpiPayment`, `CardPayment` | Encapsulates interchangeable allocation and payment behaviors |

More code examples and diagrams are in [`docs/OOP_CONCEPTS.md`](docs/OOP_CONCEPTS.md).

## Data structures and complexity

`ParkingService` uses `HashMap` for registered vehicles and active tickets, `ArrayDeque` through the `Queue` interface for waiting registrations, and `ArrayList` for slots and history. `NearestSlotStrategy` filters compatible available slots into a `PriorityQueue`; its comparator orders by distance, floor, then slot ID.

| Operation | Data structure / actual work | Complexity |
|---|---|---|
| Vehicle and active-ticket lookup | `HashMap` | Expected O(1) |
| Enqueue/dequeue waiting registration | `ArrayDeque` | O(1) |
| Select nearest compatible slot | Scan n slots and add up to n candidates to a heap, then poll | O(n log n) worst case; O(n) temporary space |
| Append in-memory parking history | `ArrayList` | Amortized O(1) |
| Promote compatible waiting vehicles after a release | Repeatedly scan the remaining queue and available slots | Up to O(w² × n log n) in this implementation |

Database calls add network and SQL execution costs; the table describes collection work only. See [`docs/DSA_IMPLEMENTATION.md`](docs/DSA_IMPLEMENTATION.md) for code excerpts and restart synchronization details.

## Database design

The schema in `src/main/resources/schema.sql` creates and selects **`smart_parking_test_db`**. It defines these tables:

| Table | Primary key | Important fields and relationships |
|---|---|---|
| `vehicles` | `registration_number` | `owner_name`, `vehicle_type`, `created_at` |
| `parking_slots` | `slot_id` | `floor_number`, `slot_size`, `distance_from_entrance`, `available` |
| `waiting_queue` | `queue_id` | Unique `registration_number` → `vehicles`; `enqueued_at`; queue order uses the auto-increment ID |
| `parking_tickets` | `ticket_id` | `registration_number` → `vehicles`; `slot_id` → `parking_slots`; entry/exit, amount, status; generated unique active-registration key permits at most one active ticket per vehicle |
| `payments` | `payment_id` | Unique `ticket_id` → `parking_tickets`; method, amount, status, `paid_at` |
| `parking_history` | `history_id` | Unique `ticket_id` → `parking_tickets`; registration → `vehicles`; slot → `parking_slots`; entry/exit, duration, fee |

The schema inserts eight sample slots (`A1`–`B4`) with `INSERT IGNORE`. DAO classes use `PreparedStatement` and try-with-resources. Entry and exit are coordinated by `ParkingService` using transactions so related slot/ticket/payment/history updates commit or roll back together. `DBConnection` reads a local properties file or environment variables; credentials are not included in the JAR.

The full entity relationship diagram is in [`docs/database-erd.md`](docs/database-erd.md).

## Architecture

```text
Swing DashboardFrame
        ↓ user actions / refreshed view
ParkingService ─── AllocationStrategy / PaymentStrategy
        ↓ coordinates rules and in-memory collections
DAO classes → DBConnection (JDBC) → MySQL
        ↑ models: Vehicle, ParkingSlot, ParkingTicket, Payment
```

The GUI calls service methods; it does not issue SQL. DAOs own SQL mapping, while `ParkingService` coordinates transactions and synchronizes Java collections. See [`docs/architecture.md`](docs/architecture.md) for component and workflow diagrams.

## Project structure

```text
.
├── .gitignore
├── README.md
├── pom.xml
├── docs/
│   ├── architecture.md
│   ├── database-erd.md
│   ├── DSA_IMPLEMENTATION.md
│   ├── OOP_CONCEPTS.md
│   ├── VIVA_QUESTIONS.md
│   └── screenshots/README.md
└── src/
    ├── main/
    │   ├── java/com/smartparking/
    │   │   ├── Main.java
    │   │   ├── dao/
    │   │   ├── database/
    │   │   ├── gui/
    │   │   ├── interfaces/
    │   │   ├── model/
    │   │   ├── service/
    │   │   └── strategy/
    │   └── resources/
    │       ├── application.properties.example
    │       └── schema.sql
    └── test/java/com/smartparking/
        ├── MySqlWorkflowTest.java
        └── ParkingServiceTest.java
```

The local `src/main/resources/application.properties` is intentionally omitted from this tree because it contains machine-specific credentials and is ignored by Git.

## Installation and execution

### Requirements

- JDK 21 or newer.
- Maven 3.9 or newer.
- MySQL Server 8 or newer.
- MySQL Workbench is optional; the MySQL command-line client can run the schema.

### 1. Install prerequisites and open the project

Install a JDK 21 distribution, Maven, and MySQL Server using their official installers. Confirm Java and Maven are available:

```shell
java -version
mvn -version
```

Open the project directory in an IDE or terminal.

### 2. Create the database schema

From the project root, run the schema using the MySQL CLI:

```shell
mysql -u root -p < src/main/resources/schema.sql
```

Or open and execute `src/main/resources/schema.sql` in MySQL Workbench. The script creates/selects `smart_parking_test_db`, defines the six tables, and seeds eight slots. Review the target database before running it; the script does not drop existing tables or data.

### 3. Configure the application

Copy `src/main/resources/application.properties.example` to `src/main/resources/application.properties` and enter your local database username and password in that ignored local file:

```properties
db.url=jdbc:mysql://127.0.0.1:3306/smart_parking_test_db?useSSL=false&serverTimezone=Asia/Kolkata
db.user=your_mysql_user
db.password=your_local_password
```

Do not commit `application.properties`. Alternatively set `PARKING_DB_URL`, `PARKING_DB_USER`, and `PARKING_DB_PASSWORD`; set `PARKING_DB_CONFIG` to use a different properties-file path. The checked-in example uses a blank password and is safe to publish.

### 4. Compile, test, launch, and package

```shell
mvn clean compile
mvn clean test
mvn exec:java
mvn clean package
```

The regular test command runs the database-independent JUnit unit tests. The opt-in workflow test connects to MySQL and uses dedicated `TEST-*` vehicle registrations and `P1`/`P2`/`P3`/`PX` slots; it refuses to overwrite existing fixtures and removes its fixtures after completion. Run it only against the configured test database:

```shell
mvn "-Dparking.integration=true" clean test
```

The quotes shown work in PowerShell and are also accepted by common shells. Check the test report output for the `MySqlWorkflowTest` result to confirm the opt-in test ran rather than being skipped.

## Verification

The project has been verified locally with Java 21 and the configured MySQL server:

- `mvn clean compile` — passed.
- `mvn clean test` — passed, 5 unit tests.
- `mvn "-Dparking.integration=true" clean test` — passed, 1 MySQL workflow test plus 5 unit tests (6 total).
- `mvn clean package` — passed.

These are local run results; no hosted CI status is implied. The opt-in workflow verifies JDBC connection, database selection, registration/lookup, nearest-slot persistence, waiting-queue persistence and promotion, exit, fee, payment/history writes, rollback, concurrency protection, restart recovery, and fixture cleanup.

## Future enhancements

The following are ideas, not current features:

- Multiple parking locations and per-location capacity.
- QR-code tickets.
- Integration with a real online payment gateway.
- Web-based administration and a mobile application.
- Real-time analytics and sensor-based parking detection.
- Schema migration/version management.

## Author

- **Name:** [Your Name]
- **GitHub:** [Your GitHub profile]
- **LinkedIn:** [Your LinkedIn profile]
- **Contact:** [Your email address]

Replace these placeholders with your own details before publishing.
