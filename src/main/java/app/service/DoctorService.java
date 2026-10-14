package app.service;

import app.dao.AppointmentDao;
import app.dao.DoctorDao;
import app.model.Appointment;
import app.model.Doctor;

import java.sql.SQLException;
import java.util.List;

/** Doctor management and availability view. No SQL here. */
public class DoctorService {

    private final DoctorDao doctorDao;
    private final AppointmentDao appointmentDao;

    public DoctorService(DoctorDao doctorDao, AppointmentDao appointmentDao) {
        this.doctorDao = doctorDao;
        this.appointmentDao = appointmentDao;
    }

    public Doctor add(String fullName, String specialty) throws SQLException {
        if (fullName == null || fullName.isBlank() || specialty == null || specialty.isBlank()) {
            throw new IllegalArgumentException("Name and specialty are required.");
        }
        return doctorDao.insert(fullName.trim(), specialty.trim());
    }

    public List<Doctor> all() throws SQLException {
        return doctorDao.findAll();
    }

    /** Active (non-cancelled) bookings for a doctor, ordered by start time. */
    public List<Appointment> schedule(int doctorId) throws SQLException {
        return appointmentDao.findByDoctor(doctorId);
    }
}
