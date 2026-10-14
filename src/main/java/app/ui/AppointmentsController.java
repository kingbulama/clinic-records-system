package app.ui;

import app.model.Appointment;
import app.model.Doctor;
import app.model.Patient;
import app.service.AppointmentService;
import app.service.DoctorService;
import app.service.PatientService;
import app.service.Services;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Thin view adapter: input validation (non-empty mandatory fields, parseable
 * times) happens here; ALL conflict logic and SQL live in the service/DAO
 * layers. No date-overlap math, no queries, no status rules in this class.
 */
public class AppointmentsController {

    @FXML private ComboBox<Patient> patientCombo;
    @FXML private ComboBox<Doctor> doctorCombo;
    @FXML private DatePicker datePicker;
    @FXML private TextField startField;
    @FXML private TextField endField;
    @FXML private TextField reasonField;
    @FXML private Label bookingMessage;

    @FXML private TextField searchPatientField;
    @FXML private ComboBox<Doctor> searchDoctorCombo;
    @FXML private ComboBox<String> searchStatusCombo;
    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;
    @FXML private Label searchMessage;

    @FXML private TableView<Appointment> appointmentsTable;
    @FXML private TableColumn<Appointment, String> colPatient;
    @FXML private TableColumn<Appointment, String> colDoctor;
    @FXML private TableColumn<Appointment, String> colStart;
    @FXML private TableColumn<Appointment, String> colEnd;
    @FXML private TableColumn<Appointment, String> colStatus;
    @FXML private TableColumn<Appointment, String> colReason;

    private final AppointmentService appointmentService = Services.appointments();
    private final PatientService patientService = Services.patients();
    private final DoctorService doctorService = Services.doctors();

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        try {
            patientCombo.setItems(FXCollections.observableArrayList(patientService.search("")));
            var doctors = FXCollections.observableArrayList(doctorService.all());
            doctorCombo.setItems(doctors);
            searchDoctorCombo.setItems(doctors);
        } catch (Exception e) {
            setMessage(bookingMessage, "Could not load data: " + e.getMessage(), true);
        }
        searchStatusCombo.setItems(FXCollections.observableArrayList("scheduled", "completed", "cancelled"));

        colPatient.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().patientName()));
        colDoctor.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().doctorName()));
        colStart.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().startTime().format(STAMP)));
        colEnd.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().endTime().format(STAMP)));
        colStatus.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().status()));
        colReason.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().reason() == null ? "" : c.getValue().reason()));
        onSearch();
    }

    @FXML
    private void onBook() {
        // --- UI-level validation only: mandatory fields present & parseable ---
        Patient patient = patientCombo.getValue();
        Doctor doctor = doctorCombo.getValue();
        LocalDate date = datePicker.getValue();
        if (patient == null || doctor == null || date == null
                || startField.getText().isBlank() || endField.getText().isBlank()) {
            setMessage(bookingMessage, "Patient, doctor, date, start and end are all required.", true);
            return;
        }
        final LocalDateTime start;
        final LocalDateTime end;
        try {
            start = LocalDateTime.of(date, LocalTime.parse(startField.getText().trim(), TIME));
            end = LocalDateTime.of(date, LocalTime.parse(endField.getText().trim(), TIME));
        } catch (DateTimeParseException e) {
            setMessage(bookingMessage, "Times must be in HH:mm format, e.g. 09:30.", true);
            return;
        }
        // --- Business rules delegated to the service layer ---
        try {
            appointmentService.book(patient.id(), doctor.id(), start, end, reasonField.getText());
            setMessage(bookingMessage, "Booked " + patient.fullName() + " with " + doctor.fullName()
                    + " at " + start.format(STAMP) + ".", false);
            onSearch();
        } catch (IllegalArgumentException | IllegalStateException e) {
            setMessage(bookingMessage, e.getMessage(), true);
        } catch (Exception e) {
            setMessage(bookingMessage, "Booking failed: " + e.getMessage(), true);
        }
    }

    @FXML
    private void onSearch() {
        try {
            Doctor d = searchDoctorCombo.getValue();
            var results = appointmentService.search(
                    searchPatientField.getText(),
                    d == null ? null : d.id(),
                    fromDate.getValue(),
                    toDate.getValue(),
                    searchStatusCombo.getValue());
            appointmentsTable.setItems(FXCollections.observableArrayList(results));
            setMessage(searchMessage, results.size() + " appointment(s) found.", false);
        } catch (IllegalArgumentException e) {
            setMessage(searchMessage, e.getMessage(), true);
        } catch (Exception e) {
            setMessage(searchMessage, "Search failed: " + e.getMessage(), true);
        }
    }

    @FXML
    private void onClearSearch() {
        searchPatientField.clear();
        searchDoctorCombo.setValue(null);
        searchStatusCombo.setValue(null);
        fromDate.setValue(null);
        toDate.setValue(null);
        onSearch();
    }

    @FXML
    private void onComplete() {
        updateSelected("completed");
    }

    @FXML
    private void onCancel() {
        updateSelected("cancelled");
    }

    private void updateSelected(String action) {
        Appointment selected = appointmentsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setMessage(searchMessage, "Select an appointment first.", true);
            return;
        }
        try {
            if ("completed".equals(action)) {
                appointmentService.complete(selected.id());
            } else {
                appointmentService.cancel(selected.id());
            }
            onSearch();
        } catch (Exception e) {
            setMessage(searchMessage, "Update failed: " + e.getMessage(), true);
        }
    }

    private void setMessage(Label label, String text, boolean error) {
        label.setText(text);
        label.getStyleClass().removeAll("error", "ok");
        label.getStyleClass().add(error ? "error" : "ok");
    }
}
