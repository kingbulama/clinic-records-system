package app.ui;

import app.service.DoctorService;
import app.service.Services;
import app.model.Appointment;
import app.model.Doctor;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.format.DateTimeFormatter;

/** Thin view: shows doctors and their schedule; no SQL or logic here. */
public class DoctorsController {

    @FXML private TextField nameField;
    @FXML private TextField specialtyField;
    @FXML private Label message;
    @FXML private ListView<Doctor> doctorsList;
    @FXML private TableView<Appointment> scheduleTable;
    @FXML private TableColumn<Appointment, String> colPatient;
    @FXML private TableColumn<Appointment, String> colStart;
    @FXML private TableColumn<Appointment, String> colEnd;
    @FXML private TableColumn<Appointment, String> colStatus;

    private final DoctorService doctorService = Services.doctors();
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        colPatient.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().patientName()));
        colStart.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().startTime().format(STAMP)));
        colEnd.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().endTime().format(STAMP)));
        colStatus.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().status()));
        refresh();
        doctorsList.getSelectionModel().selectedItemProperty().addListener((obs, old, d) -> showSchedule(d));
    }

    @FXML
    private void onAdd() {
        if (nameField.getText().isBlank() || specialtyField.getText().isBlank()) {
            setMessage("Name and specialty are required.", true);
            return;
        }
        try {
            doctorService.add(nameField.getText(), specialtyField.getText());
            setMessage("Doctor added.", false);
            nameField.clear();
            specialtyField.clear();
            refresh();
        } catch (Exception e) {
            setMessage("Could not add doctor: " + e.getMessage(), true);
        }
    }

    private void refresh() {
        try {
            doctorsList.setItems(FXCollections.observableArrayList(doctorService.all()));
        } catch (Exception e) {
            setMessage("Could not load doctors: " + e.getMessage(), true);
        }
    }

    private void showSchedule(Doctor d) {
        if (d == null) {
            scheduleTable.getItems().clear();
            return;
        }
        try {
            scheduleTable.setItems(FXCollections.observableArrayList(doctorService.schedule(d.id())));
        } catch (Exception e) {
            setMessage("Could not load schedule: " + e.getMessage(), true);
        }
    }

    private void setMessage(String text, boolean error) {
        message.setText(text);
        message.getStyleClass().removeAll("error", "ok");
        message.getStyleClass().add(error ? "error" : "ok");
    }
}
