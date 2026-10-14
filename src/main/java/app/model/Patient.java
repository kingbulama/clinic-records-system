package app.model;

import java.time.LocalDate;

public record Patient(int id, String fullName, LocalDate dateOfBirth, String phone, String email) {
    @Override
    public String toString() {
        return fullName;
    }
}
