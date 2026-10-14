package app.dao;

import app.model.MedicalRecord;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MedicalRecordDao {

    public MedicalRecord insert(int patientId, Integer appointmentId, String diagnosis, String notes) throws SQLException {
        String sql = "INSERT INTO medical_records(patient_id, appointment_id, diagnosis, notes) VALUES (?,?,?,?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, patientId);
            if (appointmentId == null) {
                ps.setNull(2, Types.INTEGER);
            } else {
                ps.setInt(2, appointmentId);
            }
            ps.setString(3, diagnosis);
            ps.setString(4, notes);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new MedicalRecord(keys.getInt(1), patientId, appointmentId, diagnosis, notes, LocalDateTime.now());
            }
        }
    }

    /** All records linked to one patient profile, newest first. */
    public List<MedicalRecord> findByPatient(int patientId) throws SQLException {
        String sql = "SELECT * FROM medical_records WHERE patient_id = ? ORDER BY created_at DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                List<MedicalRecord> out = new ArrayList<>();
                while (rs.next()) {
                    int appt = rs.getInt("appointment_id");
                    Integer apptId = rs.wasNull() ? null : appt; // must be checked right after the read
                    out.add(new MedicalRecord(
                            rs.getInt("id"),
                            rs.getInt("patient_id"),
                            apptId,
                            rs.getString("diagnosis"),
                            rs.getString("notes"),
                            LocalDateTime.parse(rs.getString("created_at").replace(' ', 'T'))));
                }
                return out;
            }
        }
    }
}
