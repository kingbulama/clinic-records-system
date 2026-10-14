package app.dao;

import app.model.Doctor;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DoctorDao {

    public Doctor insert(String fullName, String specialty) throws SQLException {
        String sql = "INSERT INTO doctors(full_name, specialty) VALUES (?,?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, fullName);
            ps.setString(2, specialty);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new Doctor(keys.getInt(1), fullName, specialty);
            }
        }
    }

    public List<Doctor> findAll() throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM doctors ORDER BY full_name");
             ResultSet rs = ps.executeQuery()) {
            List<Doctor> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new Doctor(rs.getInt("id"), rs.getString("full_name"), rs.getString("specialty")));
            }
            return out;
        }
    }
}
