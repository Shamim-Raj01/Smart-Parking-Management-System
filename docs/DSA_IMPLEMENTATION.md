# Data Structures and Algorithms

The application uses Java collections for fast decisions while MySQL preserves durable state. Relevant implementation is in `NearestSlotStrategy` and `ParkingService`.

## PriorityQueue: nearest compatible slot

`NearestSlotStrategy.allocateSlot` scans the slot collection, filters out occupied or undersized slots, inserts candidates into a `PriorityQueue`, and polls the first candidate:

```java
PriorityQueue<ParkingSlot> q = new PriorityQueue<>(
    Comparator.comparingInt(ParkingSlot::getDistance)
        .thenComparingInt(ParkingSlot::getFloor)
        .thenComparing(ParkingSlot::getId));
for (ParkingSlot slot : slots) {
    if (slot.isAvailable() && slot.fits(vehicle)) q.offer(slot);
}
return Optional.ofNullable(q.poll());
```

The comparator prioritizes entrance distance, then floor, then slot ID. Filtering costs O(n). Offering m compatible slots costs O(m log m), and polling the selected slot costs O(log m), so the complete operation is O(n + m log m), bounded by O(n log n); temporary heap space is O(m), bounded by O(n). This is a direct way to select the minimum according to a custom ordering.

## HashMap: vehicle and active-ticket lookup

`ParkingService` has `Map<String, Vehicle> vehicles` and `Map<String, ParkingTicket> active`, both backed by `HashMap`. Registration and ticket maps are keyed by a normalized registration number. Lookups are expected O(1); pathological hash collisions can increase that cost. The maps let entry/exit checks avoid repeatedly scanning the in-memory record collections.

## Queue: waiting vehicles

`ParkingService.waiting` uses `Queue<String>` with an `ArrayDeque` implementation. New waiting registrations are offered at the tail; assigned or removed registrations leave the queue. These end operations are O(1). The persistent `WaitingQueueDAO` writes the row and reloads registrations by ascending auto-increment `queue_id`.

Promotion scans the ordered waiting queue and selects the first registration that fits one currently available slot. This preserves order among vehicles compatible with the available slot. It may skip an earlier vehicle that does not fit while leaving that vehicle in the queue. The service repeats this process while a compatible slot remains, so promoting p vehicles can rescan up to w, w−1, … registrations. With n slots, the worst-case bound is O(w² × n log n) and the allocator uses O(n) temporary heap space. `ArrayDeque.contains` is O(w), so duplicate-waiting checks also scan the queue.

## ArrayList: slots and history

The service stores the slot inventory and completed parking tickets in `ArrayList`s. Appending history is amortized O(1); iterating the list is O(n). The slot list is scanned by the allocation strategy. DAO queries also return lists of vehicles, slots, history, and payments for the refreshed UI.

## Complexity summary

| Operation | Time | Extra space | Notes |
|---|---:|---:|---|
| Vehicle/active ticket map lookup | Expected O(1) | O(1) | `HashMap` lookup by registration |
| Waiting enqueue/dequeue | O(1) | O(1) | `ArrayDeque` tail/head operations |
| Duplicate waiting check | O(w) | O(1) | `Queue.contains` scans the queue |
| Nearest compatible slot | O(n + m log m), worst O(n log n) | O(m), at most O(n) | n slots, m compatible free candidates |
| History append | Amortized O(1) | O(1) amortized | Backed by `ArrayList` |
| Promote compatible waiters after a release | Up to O(w² × n log n) | O(n) heap per allocation | Repeats queue/inventory scans until no more vehicles fit |

These figures describe collection work. MySQL query, network, locking, and transaction costs are additional and depend on database state and environment.

## Synchronization with MySQL and restart recovery

- Vehicle registration writes via `VehicleDAO` before adding to the in-memory vehicle map.
- Entry opens a JDBC transaction, conditionally claims an available slot, inserts an active ticket, and removes a queued record when applicable. The in-memory slot/map/queue changes follow a successful commit.
- Exit transactionally closes the ticket, inserts payment and history records, and marks the slot available. On success, the service updates its working collections and promotes a queued vehicle.
- Waiting list insertion/removal is persisted by `WaitingQueueDAO` and maintained in the `ArrayDeque`.
- `ParkingService.persistent()` reloads database rows into vehicles, slots, active tickets, history, and the ordered waiting list. It associates recovered active tickets with the service's canonical vehicle and slot instances, so a subsequent slot release is visible to allocation.
- The GUI refresh calls `refreshFromDatabase()` and calculates dashboard statistics from reloaded service/DAO data.

`new ParkingService()` is a separate in-memory constructor used by database-independent unit tests.
