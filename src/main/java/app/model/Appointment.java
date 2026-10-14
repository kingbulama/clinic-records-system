package app.model;

import java.time.LocalDateTime;

public record Appointment(
        int id,
        int patientId,
        String patientName,
        int doctorId,
        String doctorName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status,
        String reason) {
}
