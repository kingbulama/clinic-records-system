package app.model;

import java.time.LocalDateTime;

public record MedicalRecord(
        int id,
        int patientId,
        Integer appointmentId,
        String diagnosis,
        String notes,
        LocalDateTime createdAt) {
}
