# Application screenshots

Preserve all current PNGs in this directory. They are genuine captures of the running Swing app; the vehicle-search image uses fictional demo data. `waiting-queue.png` is a genuine empty-state capture; keep it and save the populated queue in a separate `waiting-queue-populated.png` file.

## Capture status and manual checklist

| Required screen | File | Status |
|---|---|---|
| Dashboard overview | `dashboard.png` | Captured |
| Vehicle registration | `vehicle-registration.png` | Captured |
| Vehicle search | `vehicle-search.png` | Captured |
| Vehicle entry | `parking-entry.png` | Missing |
| Vehicle exit and receipt | `parking-exit.png` | Missing |
| Parking slot inventory | `parking-slots.png` | Captured |
| Parking history | `parking-history.png` | Missing |
| Payment records | `payments.png` | Missing |
| Waiting queue, empty state | `waiting-queue.png` | Captured; preserve |
| Waiting queue with at least one vehicle | `waiting-queue-populated.png` | Missing |
| Waiting queue after automatic promotion | `waiting-queue-promotion.png` | Missing |
| Receipt detail | `receipt.png` | Missing |

## Safe setup for the missing captures

Use a disposable MySQL database or a separate local MySQL instance. Do not use the configured database that contains existing project records, and do not delete or reset its data. In a copy of `schema.sql`, change only the database name in the `CREATE DATABASE` and `USE` statements to your disposable database. Apply that copy, then point the ignored local `application.properties` at the disposable database. Keep credentials out of screenshots.

Start the application from the project root with:

```powershell
mvn exec:java
```

Register fictional owners and vehicle registrations through the **Vehicles** tab. Capture the application window only, with no configuration editor or terminal visible.

## Manual capture checklist

1. **Vehicle entry — `parking-entry.png`:** In **Entry & Exit**, enter a registered vehicle's registration and click **Allocate nearest suitable slot**. Capture the confirmed ticket and slot status.
2. **Vehicle exit — `parking-exit.png`:** In **Entry & Exit**, enter an active registration, choose Cash, UPI, or Card, and click **Calculate, pay and close ticket**. Capture the completed exit screen.
3. **Receipt detail — `receipt.png`:** Open **Payments**, select the payment, click **View selected receipt**, and capture the receipt dialog.
4. **Parking history — `parking-history.png`:** Open **History** after a successful exit and capture the completed visit row.
5. **Payment records — `payments.png`:** Open **Payments** after that exit and capture the recorded method, amount, and status.
6. **Populated waiting queue — `waiting-queue-populated.png`:** The sample schema has four slots that fit Cars (`A3`, `A4`, `B2`, `B3`). Register five fictional Cars and park the first four. Attempt entry for the fifth; it should appear under **Waiting Queue**. Capture that row without changing the existing `waiting-queue.png`.
7. **Queue after automatic promotion — `waiting-queue-promotion.png`:** From **Entry & Exit**, exit one parked Car. The waiting Car should receive the released compatible slot automatically. Capture the now-empty **Waiting Queue** and verify the promoted car in the active tickets or **Parking Slots** before saving. A second capture showing the promoted vehicle's active ticket and assigned slot is useful evidence.

Capture only genuine application views. Do not draw, edit, or substitute mock screens. Use fictional demo values, keep credentials out of screenshots, and leave the disposable database intact unless you decide it is safe to remove.
