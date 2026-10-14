package app.service;

import app.dao.AppointmentDao;
import app.dao.Database;
import app.dao.DoctorDao;
import app.dao.MedicalRecordDao;
import app.dao.PatientDao;

import java.sql.SQLException;

/**
 * Composition root. Controllers obtain services here, so app.ui never
 * imports app.dao and never touches the database directly.
 */
public final class Services {

    private static final AppointmentDao APPOINTMENT_DAO = new AppointmentDao();
    private static final DoctorDao DOCTOR_DAO = new DoctorDao();
    private static final PatientDao PATIENT_DAO = new PatientDao();
    private static final MedicalRecordDao RECORD_DAO = new MedicalRecordDao();

    private Services() {
    }

    /** Creates the schema and seeds demo data on first launch. */
    public static void startup() throws SQLException {
        Database.initialise();
        Database.seedIfEmpty();
    }

    public static AppointmentService appointments() {
        return new AppointmentService(APPOINTMENT_DAO);
    }

    public static PatientService patients() {
        return new PatientService(PATIENT_DAO, RECORD_DAO);
    }

    public static DoctorService doctors() {
        return new DoctorService(DOCTOR_DAO, APPOINTMENT_DAO);
    }
}
