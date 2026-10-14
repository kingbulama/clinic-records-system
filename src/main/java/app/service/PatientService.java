package app.service;

import app.dao.MedicalRecordDao;
import app.dao.PatientDao;
import app.model.MedicalRecord;
import app.model.Patient;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** Patient registration validation + history linking. No SQL here. */
public class PatientService {

    private final PatientDao patientDao;
    private final MedicalRecordDao recordDao;

    public PatientService(PatientDao patientDao, MedicalRecordDao recordDao) {
        this.patientDao = patientDao;
        this.recordDao = recordDao;
    }

    public Patient register(String fullName, LocalDate dob, String phone, String email) throws SQLException {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Patient name is required.");
        }
        if (dob != null && dob.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Date of birth cannot be in the future.");
        }
        return patientDao.insert(fullName.trim(), dob, phone, email);
    }

    public List<Patient> search(String nameFragment) throws SQLException {
        return patientDao.searchByName(nameFragment == null ? "" : nameFragment);
    }

    /** Medical history linked to a patient profile. */
    public List<MedicalRecord> historyFor(int patientId) throws SQLException {
        return recordDao.findByPatient(patientId);
    }

    public MedicalRecord addRecord(int patientId, Integer appointmentId, String diagnosis, String notes)
            throws SQLException {
        if ((diagnosis == null || diagnosis.isBlank()) && (notes == null || notes.isBlank())) {
            throw new IllegalArgumentException("A record needs at least a diagnosis or notes.");
        }
        return recordDao.insert(patientId, appointmentId, diagnosis, notes);
    }
}
