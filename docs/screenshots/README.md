# Application screenshots

This directory is reserved for genuine screenshots captured from the running Swing application. No screenshots were captured during the current documentation pass because the desktop capture service did not expose the Java window. No generated or unrelated images are used as substitutes.

## Capture checklist

1. Configure a disposable/local test database using `src/main/resources/application.properties` and apply `src/main/resources/schema.sql`.
2. Start the app with `mvn exec:java`.
3. Capture the visible application window as PNG (not the IDE, terminal, or database editor) and save the files below in this directory.
4. Use fictional demo records only. Do not include credentials, personal data, or open configuration files in the capture.
5. For waiting queue/history/payments/receipt screenshots, use demo workflow data in a disposable database and clean it up afterward.

| Filename | Capture |
|---|---|
| `dashboard.png` | Overview metrics and active tickets |
| `vehicle-registration.png` | Vehicles tab and registration form |
| `vehicle-search.png` | Vehicles tab with a search result |
| `parking-entry.png` | Entry & Exit tab before entry |
| `parking-exit.png` | Entry & Exit tab with exit/payment controls |
| `parking-slots.png` | Parking Slots inventory and status |
| `waiting-queue.png` | Waiting Queue with a demo vehicle waiting |
| `parking-history.png` | History with a completed visit |
| `payments.png` | Payments with a completed payment |
| `receipt.png` | Receipt for a completed demo stay |

After adding the images, replace the capture instructions in the README Screenshots section with relative Markdown image links such as `![Dashboard](docs/screenshots/dashboard.png)`.
