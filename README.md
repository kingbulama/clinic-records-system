# Clinic Records System

A three-layer JavaFX desktop application for patient management, doctor
schedules, appointment booking, and medical histories.

## Architecture

```
[ JavaFX Controllers (app.ui) ]   — thin views, input validation only
            |
            v
[ Service Layer (app.service) ]   — conflict resolution, business rules (unit-tested)
            |
            v
[ DAO Layer (app.dao) ]           — parameterised JDBC, multi-write transactions
            |
            v
[ SQLite database (db/schema.sql, db/seed.sql) ] — 3NF schema: Patients, Doctors, Appointments, MedicalRecords
```

- **app.ui** — FXML/CSS views for Patient Registration, Doctor Availability,
  Appointment Booking, and Medical History lookup. Controllers validate that
  mandatory fields are filled; they contain no SQL and no date-overlap logic.
- **app.service** — business rules. `AppointmentService.overlaps()` implements
  the interval conflict test `startNew < endExisting && endNew > startExisting`
  (O(1) per comparison; O(n) to check a request against a doctor's n bookings).
- **app.dao** — all SQL via `PreparedStatement` (including wildcard multi-field
  search). `AppointmentDao.bookWithIntakeRecord` is the atomic multi-write
  transaction: appointment + initial medical intake record inside a single
  `setAutoCommit(false)` block with commit/rollback.
- **db/schema.sql** — 3NF schema with indexes. **db/seed.sql** — demo data, loaded only when the doctors table is empty.
- Controllers get services from `app.service.Services`; `app.ui` never imports `app.dao`.

## Requirements

- JDK 17+
- Maven 3.8+

## Run

```bash
mvn javafx:run
```

The embedded SQLite database (`clinic.db`) is created automatically on first
launch, with seed doctors and patients.

## Build the executable fat JAR

```bash
mvn clean package
java -jar target/clinic-records-system-fat.jar
```

The JAR bundles the JavaFX jars for the OS it was built on, so build it on the
OS you will demo on. `clinic.db` is created next to wherever you run it.

## Test

```bash
mvn test
```

JUnit 5 tests cover boundary/edge cases: back-to-back bookings (allowed),
double-booking (rejected), invalid date ranges, past slots, the atomic
booking transaction (including a rollback test), and status-change rules.

## Team tracks

- Track A (Frontend): `app/ui/**`, `src/main/resources/app/ui/**`
- Track B (Logic): `app/service/**`, `src/test/java/app/service/**`
- Track C (Backend): `app/dao/**`, `db/schema.sql`

## Team and Contributions

The application is split into three layers, one per team member.

| Layer | Member | ID Number | Responsibility |
|-------|--------|-----------|----------------|
| Core / Backend | Bulama Andabari Lalai | CSC/19U/18U/3884 | The hardest part of the app: the core back end (dao, model and the db database) |
| Business Logic | Peace David | CSC/19U/4035 | Application logic (service) |
| User Interface | Abba Caleb Jarama | CSC/19U/4061 | The UI (ui controllers, FXML screens, app.css styling and animations) |

### Project structure
- src/main/java/app/dao and app/model: data access and data models (core)
- src/main/java/app/service: business logic
- src/main/java/app/ui and src/main/resources/app/ui: screens, styling and animations
