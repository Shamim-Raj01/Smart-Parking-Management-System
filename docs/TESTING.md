# Build and testing guide

## Prerequisites

- JDK 21
- Maven 3.9+
- MySQL Server and the configured `smart_parking_test_db` for the opt-in integration workflow

Configure `src/main/resources/application.properties` from the checked-in example and apply `src/main/resources/schema.sql` before the MySQL test. The integration test first verifies that the selected database is `smart_parking_test_db`. It refuses to use any existing `TEST-*` registration or `P1`/`P2`/`P3`/`PX` fixture slot, and refuses to start if the database already has waiting vehicles (startup recovery can otherwise promote them). It saves the availability state of the sample slots, then removes only its own records and restores those values in `finally`.

## Commands

```powershell
mvn clean compile
mvn clean test
mvn "-Dparking.integration=true" clean test
mvn clean package
```

`mvn clean test` runs the six database-independent `ParkingServiceTest` cases. `MySqlWorkflowTest` is opt-in and appears as one skipped test in this mode. Add `-Dparking.integration=true` to execute that workflow against MySQL. Run it only with the intended test database configured.

## Coverage

The unit tests cover duplicate registration rejection, nearest compatible slot selection, FIFO queueing of multiple vehicles and promotion after exit, invalid or duplicate entry, and vehicle validation/rates.

The MySQL workflow covers vehicle persistence and duplicate constraints, nearest-slot allocation, active ticket persistence, FIFO queue persistence and promotion after a slot is freed, restart recovery, fee calculation, Cash/UPI/Card payment records, history writes, rollback for invalid foreign-key entry and duplicate payment, concurrent slot claim protection, and fixture cleanup.

The GUI exposes vehicle search, parking entry/exit, receipt display, and receipt printing. These Swing interactions and physical printer behavior are not automated by the current JUnit suite. Payment strategies simulate acceptance; they do not contact a payment provider.

## Last verification

Verified locally on 2026-11-11 with Java 21, Maven 3.9.16, and the configured MySQL server:

| Command | Result |
|---|---|
| `mvn clean compile` | Passed; 23 main source files compiled |
| `mvn clean test` | Passed; 6 unit tests, 1 opt-in integration test skipped |
| `mvn "-Dparking.integration=true" clean test` | Passed; 7 tests, 0 failures/errors/skips (6 unit + 1 MySQL workflow) |
| `mvn clean package` | Passed; JAR created and unit-test suite passed (6 unit tests, 1 integration test skipped) |
