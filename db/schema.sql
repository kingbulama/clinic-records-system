-- Clinic Records System — 3NF relational schema
-- Tables: Patients, Doctors, Appointments, MedicalRecords
-- 3NF notes: every non-key attribute depends on the whole primary key and
-- nothing else; specialties/names are not repeated across tables.

PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS patients (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    full_name     TEXT NOT NULL,
    date_of_birth TEXT,               -- ISO-8601: YYYY-MM-DD
    phone         TEXT,
    email         TEXT,
    created_at    TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS doctors (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    full_name  TEXT NOT NULL,
    specialty  TEXT NOT NULL DEFAULT 'General Practice',
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS appointments (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    patient_id INTEGER NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    doctor_id  INTEGER NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    start_time TEXT NOT NULL,         -- ISO-8601 local date-time
    end_time   TEXT NOT NULL,
    status     TEXT NOT NULL DEFAULT 'scheduled'
               CHECK (status IN ('scheduled', 'completed', 'cancelled')),
    reason     TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    CHECK (end_time > start_time)
);

-- Fast conflict lookups per doctor
CREATE INDEX IF NOT EXISTS idx_appointments_doctor_time
    ON appointments(doctor_id, start_time, end_time);

CREATE TABLE IF NOT EXISTS medical_records (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    patient_id     INTEGER NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    appointment_id INTEGER REFERENCES appointments(id) ON DELETE SET NULL,
    diagnosis      TEXT,
    notes          TEXT,
    created_at     TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_records_patient ON medical_records(patient_id);
