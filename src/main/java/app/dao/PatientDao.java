package app.dao;

import app.model.Patient;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PatientDao {

    public Patient insert(String fullName, LocalDate dob, String phone, String email) throws SQLException {
        String sql = "INSERT INTO patients(full_name, date_of_birth, phone, email) VALUES (?,?,?,?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, fullName);
            ps.setString(2, dob == null ? null : dob.toString());
            ps.setString(3, phone);
            ps.setString(4, email);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new Patient(keys.getInt(1), fullName, dob, phone, email);
            }
        }
    }

    /** Wildcard search on name — parameterised, safe from SQL injection. */
    public List<Patient> searchByName(String nameFragment) throws SQLException {
        String sql = "SELECT * FROM patients WHERE full_name LIKE ? ORDER BY full_name";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + nameFragment + "%");
            try (ResultSet rs = ps.executeQuery()) {
                List<Patient> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(map(rs));
                }
                return out;
            }
        }
    }

    public List<Patient> findAll() throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM patients ORDER BY full_name");
             ResultSet rs = ps.executeQuery()) {
            List<Patient> out = new ArrayList<>();
            while (rs.next()) {
                out.add(map(rs));
            }
            return out;
        }
    }

    private Patient map(ResultSet rs) throws SQLException {
        String dob = rs.getString("date_of_birth");
        return new Patient(
                rs.getInt("id"),
                rs.getString("full_name"),
                dob == null ? null : LocalDate.parse(dob),
                rs.getString("phone"),
                rs.getString("email"));
    }
}
