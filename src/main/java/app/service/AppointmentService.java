package app.service;

import app.dao.AppointmentDao;
import app.model.Appointment;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Business rules for scheduling. The UI layer calls this; it never touches
 * SQL itself. All temporal conflict logic lives here so it can be unit-tested
 * without a database.
 */
public class AppointmentService {

    private final AppointmentDao appointmentDao;

    public AppointmentService(AppointmentDao appointmentDao) {
        this.appointmentDao = appointmentDao;
    }

    /**
     * Classic interval-overlap test. Two half-open intervals [s1,e1) and
     * [s2,e2) overlap iff:
     *
     *     startNew < endExisting  AND  endNew > startExisting
     *
     * Back-to-back appointments (endNew == startExisting) do NOT overlap.
     *
     * Time complexity: O(1) — four comparisons, no loops, no allocation.
     * Checking one request against n existing appointments is O(n) with this
     * predicate applied per row; the database index on (doctor_id, start_time)
     * keeps the candidate set small in practice.
     */
    public static boolean overlaps(LocalDateTime startNew, LocalDateTime endNew,
                                   LocalDateTime startExisting, LocalDateTime endExisting) {
        return startNew.isBefore(endExisting) && endNew.isAfter(startExisting);
    }

    /** Validates a requested slot against the rules; returns an error message or null if valid. */
    public static String validateSlot(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return "Start and end times are required.";
        }
        if (!end.isAfter(start)) {
            return "End time must be after start time.";
        }
        if (start.isBefore(LocalDateTime.now())) {
            return "Cannot book appointments in the past.";
        }
        return null;
    }

    /**
     * Books an appointment atomically: the appointment row and its initial
     * medical intake record are written in one transaction (see
     * AppointmentDao.bookWithIntakeRecord). Throws IllegalStateException on
     * a double-booking attempt.
     */
    public int book(int patientId, int doctorId, LocalDateTime start, LocalDateTime end, String reason)
            throws SQLException {
        String error = validateSlot(start, end);
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
        // Pre-flight conflict check for a friendly error; the authoritative
        // check runs again inside the transaction in the DAO.
        List<Appointment> existing = appointmentDao.findByDoctor(doctorId);
        for (Appointment a : existing) { // O(n) in the doctor's appointments
            if (overlaps(start, end, a.startTime(), a.endTime())) {
                throw new IllegalStateException(
                        a.doctorName() + " already has an appointment from " +
                        a.startTime() + " to " + a.endTime());
            }
        }
        return appointmentDao.bookWithIntakeRecord(patientId, doctorId, start, end, reason);
    }

    public List<Appointment> search(String patientName, Integer doctorId,
                                    LocalDate from, LocalDate to, String status) throws SQLException {
        if (from != null && to != null && to.isBefore(from)) {
            throw new IllegalArgumentException("The 'to' date must not be before the 'from' date.");
        }
        return appointmentDao.search(patientName, doctorId, from, to, status);
    }

    /** Only a scheduled appointment can be completed or cancelled; finished ones are final. */
    public static boolean canTransition(String from, String to) {
        return "scheduled".equals(from) && ("completed".equals(to) || "cancelled".equals(to));
    }

    public void cancel(int appointmentId) throws SQLException {
        changeStatus(appointmentId, "cancelled");
    }

    public void complete(int appointmentId) throws SQLException {
        changeStatus(appointmentId, "completed");
    }

    private void changeStatus(int appointmentId, String to) throws SQLException {
        Appointment a = appointmentDao.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found."));
        if (!canTransition(a.status(), to)) {
            throw new IllegalStateException("Cannot change a " + a.status() + " appointment to " + to + ".");
        }
        appointmentDao.updateStatus(appointmentId, to);
    }
}
