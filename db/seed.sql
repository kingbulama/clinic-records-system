-- Demo data. Loaded by the app only when the doctors table is empty.
INSERT INTO doctors(full_name, specialty) VALUES
    ('Dr. Amina Bello', 'Cardiology'),
    ('Dr. Tunde Okafor', 'Pediatrics'),
    ('Dr. Sarah Lin', 'General Practice');

INSERT INTO patients(full_name, date_of_birth, phone, email) VALUES
    ('John Adeyemi', '1985-03-12', '0801 234 5678', 'john.adeyemi@example.com'),
    ('Mary Eze', '1992-07-04', '0802 345 6789', 'mary.eze@example.com');
