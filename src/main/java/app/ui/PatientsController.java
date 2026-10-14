package app.ui;

import app.model.MedicalRecord;
import app.model.Patient;
import app.service.PatientService;
import app.service.Services;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/** Thin view: mandatory-field checks only; rules and SQL live in the service/DAO layers. */
public class PatientsController {

    @FXML private TextField nameField;
    @FXML private DatePicker dobPicker;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private Label registerMessage;
    @FXML private TextField diagnosisField;
    @FXML private TextArea notesArea;
    @FXML private Label recordMessage;
    @FXML private TextField searchField;
    @FXML private ListView<Patient> patientsList;
    @FXML private TextArea historyArea;

    private final PatientService patientService = Services.patients();

    @FXML
    public void initialize() {
        onSearch();
        patientsList.getSelectionModel().selectedItemProperty().addListener((obs, old, p) -> showHistory(p));
    }

    @FXML
    private void onRegister() {
        if (nameField.getText().isBlank()) {
            setMessage(registerMessage, "Patient name is required.", true);
            return;
        }
        try {
            Patient p = patientService.register(
                    nameField.getText(), dobPicker.getValue(), phoneField.getText(), emailField.getText());
            setMessage(registerMessage, "Registered " + p.fullName() + ".", false);
            nameField.clear();
            onSearch();
        } catch (IllegalArgumentException e) {
            setMessage(registerMessage, e.getMessage(), true);
        } catch (Exception e) {
            setMessage(registerMessage, "Registration failed: " + e.getMessage(), true);
        }
    }

    @FXML
    private void onAddRecord() {
        Patient selected = patientsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setMessage(recordMessage, "Select a patient first.", true);
            return;
        }
        try {
            patientService.addRecord(selected.id(), null, diagnosisField.getText(), notesArea.getText());
            setMessage(recordMessage, "Record added.", false);
            diagnosisField.clear();
            notesArea.clear();
            showHistory(selected);
        } catch (IllegalArgumentException e) {
            setMessage(recordMessage, e.getMessage(), true);
        } catch (Exception e) {
            setMessage(recordMessage, "Could not add record: " + e.getMessage(), true);
        }
    }

    @FXML
    private void onSearch() {
        try {
            patientsList.setItems(FXCollections.observableArrayList(patientService.search(searchField.getText())));
        } catch (Exception e) {
            setMessage(registerMessage, "Search failed: " + e.getMessage(), true);
        }
    }

    private void showHistory(Patient p) {
        if (p == null) {
            historyArea.clear();
            return;
        }
        try {
            StringBuilder sb = new StringBuilder("History for ").append(p.fullName()).append("\n\n");
            var records = patientService.historyFor(p.id());
            if (records.isEmpty()) {
                sb.append("No records yet.");
            }
            for (MedicalRecord r : records) {
                sb.append("• ").append(r.createdAt().toLocalDate())
                        .append(r.diagnosis() == null ? "" : " — " + r.diagnosis())
                        .append(r.notes() == null ? "" : "\n  " + r.notes())
                        .append("\n");
            }
            historyArea.setText(sb.toString());
        } catch (Exception e) {
            historyArea.setText("Could not load history: " + e.getMessage());
        }
    }

    private void setMessage(Label label, String text, boolean error) {
        label.setText(text);
        label.getStyleClass().removeAll("error", "ok");
        label.getStyleClass().add(error ? "error" : "ok");
    }
}
