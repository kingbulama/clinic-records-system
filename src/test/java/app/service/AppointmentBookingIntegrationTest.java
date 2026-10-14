package app.service;

import app.dao.*;
import app.model.Doctor;
import app.model.Patient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end tests against a throw-away SQLite file: double-booking is
 * rejected, the atomic transaction writes both rows, and multi-field search
 * filters correctly.
 */
class AppointmentBookingIntegrationTest {

    private Path dbFile;
    private AppointmentService appointmentService;
    private PatientService patientService;
    private Patient patient;
    private Doctor doctor;

    @BeforeEach
    void setUp() throws Exception {
        dbFile = Files.createTempFile("clinic-test-", ".db");
        Files.delete(dbFile); // let SQLite create it fresh
        Database.useUrl("jdbc:sqlite:" + dbFile.toAbsolutePath());
        Database.initialise();
        appointmentService = new AppointmentService(new AppointmentDao());
        patientService = new PatientService(new PatientDao(), new MedicalRecordDao());
        patient = patientService.register("Test Patient", LocalDate.of(1990, 1, 1), null, null);
        doctor = new DoctorService(new DoctorDao(), new AppointmentDao()).add("Dr. Test", "General");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (dbFile != null) Files.deleteIfExists(dbFile);
    }

    @Test
    void bookingCreatesAppointmentAndIntakeRecordAtomically() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withSecond(0).withNano(0);
        int id = appointmentService.book(patient.id(), doctor.id(), start, start.plusMinutes(30), "Check-up");
        assertTrue(id > 0);
        var history = patientService.historyFor(patient.id());
        assertEquals(1, history.size());
        assertEquals(Integer.valueOf(id), history.get(0).appointmentId());
    }

    @Test
    void doubleBookingIsRejected() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withSecond(0).withNano(0);
        appointmentService.book(patient.id(), doctor.id(), start, start.plusMinutes(30), "First");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                appointmentService.book(patient.id(), doctor.id(),
                        start.plusMinutes(15), start.plusMinutes(45), "Overlap"));
        assertTrue(ex.getMessage().contains("already has an appointment"));
    }

    @Test
    void backToBackBookingIsAllowed() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withSecond(0).withNano(0);
        appointmentService.book(patient.id(), doctor.id(), start, start.plusMinutes(30), "First");
        assertDoesNotThrow(() -> appointmentService.book(
                patient.id(), doctor.id(), start.plusMinutes(30), start.plusMinutes(60), "Second"));
    }

    @Test
    void failedIntakeInsertRollsBackTheAppointment() throws Exception {
        // Sabotage the SECOND write of the transaction: the appointment insert
        // succeeds, the intake-record insert then fails, so rollback must undo both.
        try (var c = Database.getConnection(); var st = c.createStatement()) {
            st.execute("DROP TABLE medical_records");
        }
        LocalDateTime start = LocalDateTime.now().plusDays(1).withSecond(0).withNano(0);
        assertThrows(java.sql.SQLException.class, () ->
                appointmentService.book(patient.id(), doctor.id(), start, start.plusMinutes(30), "Check-up"));
        assertEquals(0, appointmentService.search(null, null, null, null, null).size());
    }

    @Test
    void finishedAppointmentCannotBeCancelled() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withSecond(0).withNano(0);
        int id = appointmentService.book(patient.id(), doctor.id(), start, start.plusMinutes(30), "Check-up");
        appointmentService.complete(id);
        assertThrows(IllegalStateException.class, () -> appointmentService.cancel(id));
        assertThrows(IllegalArgumentException.class, () -> appointmentService.cancel(9999));
    }

    @Test
    void noAppointmentLinkedRecordHasNullAppointmentId() throws Exception {
        patientService.addRecord(patient.id(), null, "Flu", null);
        assertNull(patientService.historyFor(patient.id()).get(0).appointmentId());
    }

    @Test
    void invalidDateRangeRejectedInSearch() {
        assertThrows(IllegalArgumentException.class, () ->
                appointmentService.search(null, null, LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 1), null));
    }

    @Test
    void multiFieldSearchFiltersByStatusAndName() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withSecond(0).withNano(0);
        int id = appointmentService.book(patient.id(), doctor.id(), start, start.plusMinutes(30), "Check-up");
        assertEquals(1, appointmentService.search("Test", null, null, null, "scheduled").size());
        appointmentService.cancel(id);
        assertEquals(0, appointmentService.search("Test", null, null, null, "scheduled").size());
        assertEquals(1, appointmentService.search("Test", null, null, null, "cancelled").size());
        assertEquals(0, appointmentService.search("NoSuchName", null, null, null, null).size());
    }
}
