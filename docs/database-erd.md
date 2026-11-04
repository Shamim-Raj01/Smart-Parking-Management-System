# MySQL Database ER Diagram

This diagram follows `src/main/resources/schema.sql`. The script creates/selects `smart_parking_test_db`. `PK`, `FK`, and `UQ` identify primary, foreign, and unique keys.

```mermaid
erDiagram
    VEHICLES {
        varchar registration_number PK
        varchar owner_name
        enum vehicle_type
        timestamp created_at
    }
    PARKING_SLOTS {
        varchar slot_id PK
        int floor_number
        int slot_size
        int distance_from_entrance
        boolean available
    }
    WAITING_QUEUE {
        bigint queue_id PK
        varchar registration_number FK_UQ
        datetime enqueued_at
    }
    PARKING_TICKETS {
        char ticket_id PK
        varchar registration_number FK
        varchar slot_id FK
        datetime entry_time
        datetime exit_time
        decimal amount
        enum status
        varchar active_registration GENERATED_UQ
    }
    PAYMENTS {
        bigint payment_id PK
        char ticket_id FK_UQ
        enum method
        decimal amount
        enum status
        datetime paid_at
    }
    PARKING_HISTORY {
        bigint history_id PK
        char ticket_id FK_UQ
        varchar registration_number FK
        varchar slot_id FK
        datetime entry_time
        datetime exit_time
        bigint duration_minutes
        decimal fee
    }

    VEHICLES ||--o{ PARKING_TICKETS : has
    PARKING_SLOTS ||--o{ PARKING_TICKETS : assigned_to
    VEHICLES ||--o| WAITING_QUEUE : may_wait
    PARKING_TICKETS ||--o| PAYMENTS : may_have
    PARKING_TICKETS ||--o| PARKING_HISTORY : recorded_as
    VEHICLES ||--o{ PARKING_HISTORY : appears_in
    PARKING_SLOTS ||--o{ PARKING_HISTORY : used_for
```

## Relationship details

- A ticket belongs to one vehicle and one parking slot. A vehicle and a slot can be referenced by multiple historical tickets over time.
- `parking_tickets.active_registration` is a stored generated column: it equals the registration for `ACTIVE` tickets and is `NULL` for closed tickets. A unique key on it enforces at most one active ticket per vehicle.
- A vehicle can have at most one waiting row because `waiting_queue.registration_number` is unique; its foreign key cascades when a vehicle is deleted.
- `payments.ticket_id` is unique, so each ticket has at most one payment record. It references the ticket, not the vehicle directly.
- `parking_history.ticket_id` is unique; history also references the vehicle and slot. History is written on exit.
- The schema does not declare a direct foreign key from `payments` to `vehicles` or `parking_slots`; those are obtained through the ticket.
- Slot IDs are unique. `slot_size` is constrained to 1–3. Vehicle type, ticket status, payment method, and payment status use MySQL `ENUM` values.

Seed inventory is eight sample slots, `A1`–`B4`, inserted with `INSERT IGNORE`.
