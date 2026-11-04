# Smart Parking Management System: Viva Questions and Answers

Answers are based on the current source and schema. Payment strategies simulate acceptance; they do not connect to a payment provider.

## A. Java and OOP — 10 questions

### 1. Why did you choose Java for this project?
Java provides object-oriented modeling, the Collections Framework, Swing for a desktop GUI, JDBC for relational database access, and a mature Maven/JUnit toolchain used by this application.

### 2. Where is inheritance used?
`Car`, `Bike`, and `Truck` extend abstract `Vehicle`, reusing its registration and owner data while implementing their own type, required slot size, and hourly rate.

### 3. How is encapsulation used?
`Vehicle` fields and `ParkingSlot.available` are private. Slot state changes through synchronized `occupy()` and `release()` methods that validate transitions.

### 4. Where is abstraction used?
`Vehicle` declares abstract subtype operations; `AllocationStrategy` and `PaymentStrategy` expose behavior contracts without requiring callers to know each implementation.

### 5. How does the project demonstrate runtime polymorphism?
Code that holds a `Vehicle` calls overridden methods such as `requiredSize()` and `hourlyRate()`. Java dispatches to the actual Car, Bike, or Truck implementation.

### 6. Why use interfaces?
`AllocationStrategy` and `PaymentStrategy` describe behavior independently from a concrete implementation, helping the service depend on contracts.

### 7. Where is the Strategy pattern?
`NearestSlotStrategy` implements `AllocationStrategy`; `CashPayment`, `UpiPayment`, and `CardPayment` implement `PaymentStrategy`.

### 8. Why are `ParkingTicket` and `Payment` records?
They are compact data carriers with generated accessors and value-oriented behavior. `ParkingTicket` also has `active()`, which checks whether an exit time is absent.

### 9. Why use `BigDecimal` for fees?
Decimal monetary values should not use binary floating-point rounding. The service uses `BigDecimal` and sets the calculated amount to two decimal places.

### 10. How does vehicle validation work?
`Vehicle` validates a 3–15 character alphanumeric/hyphen registration and nonblank owner at construction. `setOwnerName` also rejects blank values; the registration is final and normalized to uppercase.

## B. Data structures and algorithms — 10 questions

### 11. Why use a `PriorityQueue` for slot allocation?
The application needs the available compatible slot with the smallest ordering key. A priority queue returns the minimum according to its comparator.

### 12. What is the slot comparator?
`NearestSlotStrategy` compares distance from the entrance, then floor number, then slot ID for deterministic ties.

### 13. What is nearest-slot complexity?
The strategy scans n slots and inserts up to m candidates into the heap, so work is O(n + m log m), bounded by O(n log n), with O(m) temporary space.

### 14. Why use a `HashMap`?
`ParkingService` uses maps keyed by registration for vehicles and active tickets, so expected lookup is O(1) rather than scanning all records.

### 15. Is `HashMap` lookup always O(1)?
It is expected O(1) with well-distributed hashes. Severe hash collisions can worsen lookup; the complexity is not a strict worst-case guarantee.

### 16. How does the waiting queue work?
An `ArrayDeque` backs `Queue<String>`. New registrations are appended, and the service scans waiting order for the first vehicle compatible with an available slot.

### 17. How does waiting order survive an application restart?
`WaitingQueueDAO` stores each registration in `waiting_queue`; startup reads rows ordered by `queue_id` and rebuilds the in-memory queue.

### 18. How are slot sizes matched to vehicle types?
`Bike.requiredSize()` is 1, `Car.requiredSize()` is 2, and `Truck.requiredSize()` is 3. `ParkingSlot.fits(vehicle)` checks that slot size is at least the requirement.

### 19. Where are `ArrayList`s used?
`ParkingService` stores the slot inventory and completed history in lists. Appending a record is amortized O(1); scanning is O(n).

### 20. What prevents two transactions from claiming one slot?
`ParkingSlotDAO.setAvailable` updates only a row whose current availability has the expected opposite value. It requires exactly one updated row, so a competing claim fails instead of silently succeeding.

## C. JDBC and MySQL — 10 questions

### 21. How does JDBC work here?
`DBConnection` creates JDBC connections. DAO classes use `PreparedStatement` for SQL and map `ResultSet` rows into model objects. The MySQL Connector/J driver supplies the MySQL protocol implementation.

### 22. Why use `PreparedStatement`?
It binds values separately from SQL text, handles JDBC type conversion, and avoids assembling user-provided values into SQL strings.

### 23. What is the DAO pattern?
Each DAO owns persistence operations for a data area—vehicles, slots, tickets, payments, history, or waiting rows—separating SQL from service/UI logic.

### 24. Why are transactions important for exit?
Exit changes multiple related rows: ticket status, payment, history, and slot availability. A transaction makes those writes succeed together or roll back together.

### 25. What does rollback do?
`Connection.rollback()` cancels the uncommitted writes in the current transaction. The integration workflow verifies that a duplicate payment attempt leaves the ticket active and creates no partial payment/history record.

### 26. What foreign keys exist?
Waiting rows reference vehicles; tickets reference vehicles and slots; payments reference tickets; history references tickets, vehicles, and slots.

### 27. How does the schema enforce one active ticket per vehicle?
`parking_tickets.active_registration` is a generated value containing the registration only for ACTIVE tickets. A unique key on this nullable column permits closed tickets but rejects a second active one.

### 28. How are credentials configured safely?
`DBConnection` reads an ignored local `application.properties` or environment variables. The Maven resources configuration excludes the local properties file from the packaged JAR.

### 29. How do you run MySQL integration tests?
Configure the local test database, then run `mvn "-Dparking.integration=true" clean test`. The property enables the Surefire-discovered `MySqlWorkflowTest`, which uses dedicated test IDs and cleanup checks.

### 30. Why use try-with-resources?
Connections, statements, and result sets are closed automatically when their scope ends, including when a database operation throws an exception.

## D. Architecture and implementation — 10 questions

### 31. What are the main application layers?
`DashboardFrame` is the Swing view; `ParkingService` coordinates rules and operations; DAOs handle SQL; `DBConnection` creates JDBC connections; models represent vehicles, slots, tickets, and payments.

### 32. What happens at startup?
`Main` creates `ParkingService.persistent()`. It loads vehicles, slots, active tickets, history, and queue order from MySQL and reconstructs the in-memory maps, lists, and queue.

### 33. How does the application report a database startup failure?
`Main` catches startup exceptions and displays an error dialog asking the user to configure `application.properties` and apply `schema.sql`.

### 34. How is the fee calculated?
`ParkingService.exit` computes elapsed seconds, rounds partial hours up, applies a one-hour minimum, multiplies by the vehicle's hourly rate, and sets scale to two decimal places.

### 35. What are the implemented hourly rates?
Bike: 20.00; Car: 40.00; Truck: 80.00. These values come from the concrete vehicle classes.

### 36. What happens when no compatible slot is available?
The service persists the vehicle in the waiting queue and returns no ticket. The GUI reports that the vehicle was added to the queue.

### 37. How is the waiting vehicle chosen after exit?
The service scans waiting order, checking for a vehicle with at least one compatible available slot. It assigns the first compatible one, leaving incompatible registrations queued.

### 38. What information appears on a receipt?
The GUI receipt includes ticket ID, vehicle registration/type, slot, entry and exit timestamps, duration, fee, payment method, and status.

### 39. Does a payment strategy charge a real card or UPI account?
No. `CashPayment`, `UpiPayment`, and `CardPayment` simulate acceptance for nonnegative amounts. The selected method and result are stored as a payment record.

### 40. Which tests are unit tests and which are integration tests?
`ParkingServiceTest` exercises in-memory service behavior without MySQL. `MySqlWorkflowTest` performs real JDBC database operations only when `parking.integration=true` is supplied.
