package app.dao;

import app.model.Appointment;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AppointmentDao {

    private static final String SELECT_JOIN =
            "SELECT a.*, p.full_name AS patient_name, d.full_name AS doctor_name " +
            "FROM appointments a " +
            "JOIN patients p ON p.id = a.patient_id " +
            "JOIN doctors  d ON d.id = a.doctor_id ";

    /**
     * Multi-field search: every filter is optional and applied only when
     * non-null. All values are bound parameters (PreparedStatement), never
     * concatenated, so wildcard input cannot inject SQL.
     */
    public List<Appointment> search(String patientName, Integer doctorId,
                                    LocalDate from, LocalDate to, String status) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_JOIN).append("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (patientName != null && !patientName.isBlank()) {
            sql.append("AND p.full_name LIKE ? ");
            params.add("%" + patientName.trim() + "%");
        }
        if (doctorId != null) {
            sql.append("AND a.doctor_id = ? ");
            params.add(doctorId);
        }
        if (from != null) {
            sql.append("AND a.start_time >= ? ");
            params.add(from.atStartOfDay().toString());
        }
        if (to != null) {
            sql.append("AND a.start_time < ? ");
            params.add(to.plusDays(1).atStartOfDay().toString());
        }
        if (status != null && !status.isBlank()) {
            sql.append("AND a.status = ? ");
            params.add(status);
        }
        sql.append("ORDER BY a.start_time");

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Appointment> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(map(rs));
                }
                return out;
            }
        }
    }

    public List<Appointment> findByDoctor(int doctorId) throws SQLException {
        String sql = SELECT_JOIN + "WHERE a.doctor_id = ? AND a.status <> 'cancelled' ORDER BY a.start_time";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Appointment> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(map(rs));
                }
                return out;
            }
        }
    }

    public java.util.Optional<Appointment> findById(int appointmentId) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_JOIN + "WHERE a.id = ?")) {
            ps.setInt(1, appointmentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? java.util.Optional.of(map(rs)) : java.util.Optional.empty();
            }
        }
    }

    public void updateStatus(int appointmentId, String status) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE appointments SET status = ? WHERE id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, appointmentId);
            ps.executeUpdate();
        }
    }

    /**
     * Atomic multi-write transaction: inserts the appointment AND its initial
     * medical intake record inside a single setAutoCommit(false) block.
     * The overlap re-check happens inside the same transaction, so a
     * concurrent booking cannot slip between check and insert. Any failure
     * rolls both writes back.
     */
    public int bookWithIntakeRecord(int patientId, int doctorId,
                                    LocalDateTime start, LocalDateTime end, String reason) throws SQLException {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Re-check overlap inside the transaction.
                try (PreparedStatement check = conn.prepareStatement(
                        "SELECT COUNT(*) FROM appointments " +
                        "WHERE doctor_id = ? AND status <> 'cancelled' AND ? < end_time AND ? > start_time")) {
                    check.setInt(1, doctorId);
                    check.setString(2, start.toString());
                    check.setString(3, end.toString());
                    try (ResultSet rs = check.executeQuery()) {
                        rs.next();
                        if (rs.getInt(1) > 0) {
                            throw new SQLException("CONFLICT: doctor already has an overlapping appointment");
                        }
                    }
                }

                int appointmentId;
                try (PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO appointments(patient_id, doctor_id, start_time, end_time, reason) " +
                        "VALUES (?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    ins.setInt(1, patientId);
                    ins.setInt(2, doctorId);
                    ins.setString(3, start.toString());
                    ins.setString(4, end.toString());
                    ins.setString(5, reason);
                    ins.executeUpdate();
                    try (ResultSet keys = ins.getGeneratedKeys()) {
                        keys.next();
                        appointmentId = keys.getInt(1);
                    }
                }

                try (PreparedStatement rec = conn.prepareStatement(
                        "INSERT INTO medical_records(patient_id, appointment_id, notes) VALUES (?,?,?)")) {
                    rec.setInt(1, patientId);
                    rec.setInt(2, appointmentId);
                    rec.setString(3, "Intake: " + (reason == null ? "" : reason));
                    rec.executeUpdate();
                }

                conn.commit();
                return appointmentId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private Appointment map(ResultSet rs) throws SQLException {
        return new Appointment(
                rs.getInt("id"),
                rs.getInt("patient_id"),
                rs.getString("patient_name"),
                rs.getInt("doctor_id"),
                rs.getString("doctor_name"),
                LocalDateTime.parse(rs.getString("start_time")),
                LocalDateTime.parse(rs.getString("end_time")),
                rs.getString("status"),
                rs.getString("reason"));
    }
}
