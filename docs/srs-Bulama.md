# SRS: Use Cases (Bulama)

## UC-1: Book an appointment.
- **Actor:** Receptionist
- **Goal:** Book a patient with a doctor at a chosen time
- **Preconditions:** Patient is registered; doctor exists
- **Main flow:**
  1. Receptionist selects a patient and a doctor.
  2. Receptionist picks a date and time.
  3. System checks the doctor has no overlapping appointment.
  4. System saves the appointment and the intake record together.
  5. System shows a confirmation.
- **Alternative flow:** The slot overlaps another booking, so the system rejects it and shows a message.
- **Postcondition:** Appointment and intake record are saved, or neither is.

## UC-2: View a patient's medical history.
- **Actor:** Receptionist
- **Goal:** Look up a patient's past visits and records
- **Preconditions:** The patient is already registered
- **Main flow:**
  1. Receptionist searches for a patient by name or ID.
  2. System shows the matching patients.
  3. Receptionist selects one patient.
  4. System displays that patient's medical history.
- **Alternative flow:** No patient matches the search, so the system shows "No patient found".
- **Postcondition:** The history is displayed and no data is changed.
