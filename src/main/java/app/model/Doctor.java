package app.model;

public record Doctor(int id, String fullName, String specialty) {
    @Override
    public String toString() {
        return fullName + " (" + specialty + ")";
    }
}
