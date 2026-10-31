--liquibase formatted sql

-- =====================================================================
-- Demo data: one user per role, sample patients and a short clinical
-- history so every screen and dashboard has something to show.
-- All demo users share the password  Password@123  (BCrypt hash below).
-- The data is fictional.
-- =====================================================================

--changeset careconnect:020-users
INSERT INTO app_users (id, username, email, password_hash, full_name, user_role, department_id, phone, created_by)
VALUES (nextval('app_users_seq'), 'admin', 'admin@careconnect.local', '$2a$10$jOOAVYspAXHK.zBuFeBkWezfBqHODeShEhLFmHvRqc35cYaALK3/a', 'System Administrator', 'ADMIN', (SELECT id FROM departments WHERE code = 'ADMN'), '9000000001', 'system');
INSERT INTO app_users (id, username, email, password_hash, full_name, user_role, department_id, phone, specialty, license_no, created_by)
VALUES (nextval('app_users_seq'), 'dr.mehta', 'asha.mehta@careconnect.local', '$2a$10$jOOAVYspAXHK.zBuFeBkWezfBqHODeShEhLFmHvRqc35cYaALK3/a', 'Dr. Asha Mehta', 'DOCTOR', (SELECT id FROM departments WHERE code = 'GEN'), '9000000002', 'General Medicine', 'MCI-GEN-10231', 'system');
INSERT INTO app_users (id, username, email, password_hash, full_name, user_role, department_id, phone, specialty, license_no, created_by)
VALUES (nextval('app_users_seq'), 'dr.rao', 'vikram.rao@careconnect.local', '$2a$10$jOOAVYspAXHK.zBuFeBkWezfBqHODeShEhLFmHvRqc35cYaALK3/a', 'Dr. Vikram Rao', 'DOCTOR', (SELECT id FROM departments WHERE code = 'CARD'), '9000000003', 'Cardiology', 'MCI-CAR-20417', 'system');
INSERT INTO app_users (id, username, email, password_hash, full_name, user_role, department_id, phone, created_by)
VALUES (nextval('app_users_seq'), 'nurse.priya', 'priya.nair@careconnect.local', '$2a$10$jOOAVYspAXHK.zBuFeBkWezfBqHODeShEhLFmHvRqc35cYaALK3/a', 'Priya Nair', 'NURSE', (SELECT id FROM departments WHERE code = 'NURS'), '9000000004', 'system');
INSERT INTO app_users (id, username, email, password_hash, full_name, user_role, department_id, phone, license_no, created_by)
VALUES (nextval('app_users_seq'), 'pharm.arjun', 'arjun.singh@careconnect.local', '$2a$10$jOOAVYspAXHK.zBuFeBkWezfBqHODeShEhLFmHvRqc35cYaALK3/a', 'Arjun Singh', 'PHARMACIST', (SELECT id FROM departments WHERE code = 'PHAR'), '9000000005', 'PCI-PH-55120', 'system');
INSERT INTO app_users (id, username, email, password_hash, full_name, user_role, department_id, phone, created_by)
VALUES (nextval('app_users_seq'), 'recep.neha', 'neha.kapoor@careconnect.local', '$2a$10$jOOAVYspAXHK.zBuFeBkWezfBqHODeShEhLFmHvRqc35cYaALK3/a', 'Neha Kapoor', 'RECEPTIONIST', (SELECT id FROM departments WHERE code = 'FRONT'), '9000000006', 'system');

--changeset careconnect:021-patients
INSERT INTO patients (id, mrn, first_name, last_name, date_of_birth, gender, blood_group, phone, email, address_line, city, state_name, postal_code, emergency_name, emergency_phone, emergency_relation, insurance_provider, insurance_policy_no, insurance_valid_till, medical_history, primary_provider_id, created_by)
VALUES (nextval('patients_seq'), 'CC-2026-000001', 'Ravi', 'Kumar', DATE '1968-04-12', 'MALE', 'B+', '9876500001', 'ravi.kumar@example.com', '12 MG Road', 'Pune', 'Maharashtra', '411001', 'Anita Kumar', '9876500011', 'Spouse', 'Star Health', 'SH-44821907', DATE '2027-03-31',
        'Type 2 diabetes for 8 years and hypertension for 5 years. Appendectomy in 1995. Non-smoker.', (SELECT id FROM app_users WHERE username = 'dr.mehta'), 'system');
INSERT INTO patients (id, mrn, first_name, last_name, date_of_birth, gender, blood_group, phone, email, address_line, city, state_name, postal_code, emergency_name, emergency_phone, emergency_relation, insurance_provider, insurance_policy_no, insurance_valid_till, medical_history, primary_provider_id, created_by)
VALUES (nextval('patients_seq'), 'CC-2026-000002', 'Sita', 'Verma', DATE '1991-09-23', 'FEMALE', 'O+', '9876500002', 'sita.verma@example.com', '45 Residency Road', 'Bengaluru', 'Karnataka', '560025', 'Rohit Verma', '9876500012', 'Brother', 'HDFC Ergo', 'HE-77120455', DATE '2026-12-31',
        'Bronchial asthma since childhood, seasonal exacerbations. Severe peanut allergy.', (SELECT id FROM app_users WHERE username = 'dr.mehta'), 'system');
INSERT INTO patients (id, mrn, first_name, last_name, date_of_birth, gender, blood_group, phone, email, address_line, city, state_name, postal_code, emergency_name, emergency_phone, emergency_relation, insurance_provider, insurance_policy_no, insurance_valid_till, medical_history, primary_provider_id, created_by)
VALUES (nextval('patients_seq'), 'CC-2026-000003', 'Mohammed', 'Irfan', DATE '1955-01-30', 'MALE', 'A+', '9876500003', 'm.irfan@example.com', '8 Banjara Hills', 'Hyderabad', 'Telangana', '500034', 'Sana Irfan', '9876500013', 'Daughter', 'ICICI Lombard', 'IL-30988172', DATE '2027-06-30',
        'Atrial fibrillation diagnosed 2023, on anticoagulation. Hypertension.', (SELECT id FROM app_users WHERE username = 'dr.rao'), 'system');
INSERT INTO patients (id, mrn, first_name, last_name, date_of_birth, gender, blood_group, phone, email, address_line, city, state_name, postal_code, emergency_name, emergency_phone, emergency_relation, medical_history, primary_provider_id, created_by)
VALUES (nextval('patients_seq'), 'CC-2026-000004', 'Lakshmi', 'Iyer', DATE '1980-06-05', 'FEMALE', 'AB+', '9876500004', 'lakshmi.iyer@example.com', '21 TTK Road', 'Chennai', 'Tamil Nadu', '600018', 'Suresh Iyer', '9876500014', 'Husband',
        'Hypothyroidism on levothyroxine since 2019.', (SELECT id FROM app_users WHERE username = 'dr.mehta'), 'system');
INSERT INTO patients (id, mrn, first_name, last_name, date_of_birth, gender, blood_group, phone, address_line, city, state_name, postal_code, emergency_name, emergency_phone, emergency_relation, medical_history, primary_provider_id, created_by)
VALUES (nextval('patients_seq'), 'CC-2026-000005', 'Kabir', 'Malhotra', DATE '2016-11-14', 'MALE', 'O-', '9876500005', '3 Lodhi Estate', 'New Delhi', 'Delhi', '110003', 'Meera Malhotra', '9876500015', 'Mother',
        'Healthy child. Immunizations up to date.', (SELECT id FROM app_users WHERE username = 'dr.mehta'), 'system');

--changeset careconnect:022-patient-users
INSERT INTO app_users (id, username, email, password_hash, full_name, user_role, phone, patient_id, created_by)
VALUES (nextval('app_users_seq'), 'patient.ravi', 'ravi.kumar@example.com', '$2a$10$jOOAVYspAXHK.zBuFeBkWezfBqHODeShEhLFmHvRqc35cYaALK3/a', 'Ravi Kumar', 'PATIENT', '9876500001', (SELECT id FROM patients WHERE mrn = 'CC-2026-000001'), 'system');
INSERT INTO app_users (id, username, email, password_hash, full_name, user_role, phone, patient_id, created_by)
VALUES (nextval('app_users_seq'), 'patient.sita', 'sita.verma@example.com', '$2a$10$jOOAVYspAXHK.zBuFeBkWezfBqHODeShEhLFmHvRqc35cYaALK3/a', 'Sita Verma', 'PATIENT', '9876500002', (SELECT id FROM patients WHERE mrn = 'CC-2026-000002'), 'system');

--changeset careconnect:023-history
INSERT INTO patient_allergies (id, patient_id, allergen, allergy_type, reaction, severity, noted_on, created_by) VALUES (nextval('patient_allergies_seq'), (SELECT id FROM patients WHERE mrn = 'CC-2026-000001'), 'Penicillin', 'DRUG', 'Hives and facial swelling', 'SEVERE', DATE '2010-05-01', 'system');
INSERT INTO patient_allergies (id, patient_id, allergen, allergy_type, reaction, severity, noted_on, created_by) VALUES (nextval('patient_allergies_seq'), (SELECT id FROM patients WHERE mrn = 'CC-2026-000002'), 'Sulfa', 'DRUG', 'Generalised rash', 'MODERATE', DATE '2015-02-10', 'system');
INSERT INTO patient_allergies (id, patient_id, allergen, allergy_type, reaction, severity, noted_on, created_by) VALUES (nextval('patient_allergies_seq'), (SELECT id FROM patients WHERE mrn = 'CC-2026-000002'), 'Peanuts', 'FOOD', 'Anaphylaxis', 'SEVERE', DATE '2000-01-01', 'system');
INSERT INTO patient_allergies (id, patient_id, allergen, allergy_type, reaction, severity, noted_on, created_by) VALUES (nextval('patient_allergies_seq'), (SELECT id FROM patients WHERE mrn = 'CC-2026-000003'), 'Dust mites', 'ENVIRONMENT', 'Sneezing', 'MILD', DATE '2018-08-01', 'system');
INSERT INTO patient_allergies (id, patient_id, allergen, allergy_type, reaction, severity, noted_on, created_by) VALUES (nextval('patient_allergies_seq'), (SELECT id FROM patients WHERE mrn = 'CC-2026-000004'), 'Ibuprofen', 'DRUG', 'Severe gastric pain', 'MODERATE', DATE '2021-03-15', 'system');

INSERT INTO patient_problems (id, patient_id, icd10_code, description, status, onset_date, created_by) VALUES (nextval('patient_problems_seq'), (SELECT id FROM patients WHERE mrn = 'CC-2026-000001'), 'E11.9', 'Type 2 diabetes mellitus', 'ACTIVE', DATE '2018-03-01', 'system');
INSERT INTO patient_problems (id, patient_id, icd10_code, description, status, onset_date, created_by) VALUES (nextval('patient_problems_seq'), (SELECT id FROM patients WHERE mrn = 'CC-2026-000001'), 'I10', 'Essential hypertension', 'ACTIVE', DATE '2021-06-01', 'system');
INSERT INTO patient_problems (id, patient_id, icd10_code, description, status, onset_date, created_by) VALUES (nextval('patient_problems_seq'), (SELECT id FROM patients WHERE mrn = 'CC-2026-000001'), 'E78.5', 'Hyperlipidemia', 'ACTIVE', DATE '2022-01-15', 'system');
INSERT INTO patient_problems (id, patient_id, icd10_code, description, status, onset_date, created_by) VALUES (nextval('patient_problems_seq'), (SELECT id FROM patients WHERE mrn = 'CC-2026-000002'), 'J45.909', 'Bronchial asthma', 'ACTIVE', DATE '2005-07-01', 'system');
INSERT INTO patient_problems (id, patient_id, icd10_code, description, status, onset_date, created_by) VALUES (nextval('patient_problems_seq'), (SELECT id FROM patients WHERE mrn = 'CC-2026-000003'), 'I48.91', 'Atrial fibrillation', 'ACTIVE', DATE '2023-02-10', 'system');
INSERT INTO patient_problems (id, patient_id, icd10_code, description, status, onset_date, created_by) VALUES (nextval('patient_problems_seq'), (SELECT id FROM patients WHERE mrn = 'CC-2026-000003'), 'I10', 'Essential hypertension', 'ACTIVE', DATE '2015-04-01', 'system');
INSERT INTO patient_problems (id, patient_id, icd10_code, description, status, onset_date, created_by) VALUES (nextval('patient_problems_seq'), (SELECT id FROM patients WHERE mrn = 'CC-2026-000004'), 'E03.9', 'Hypothyroidism', 'ACTIVE', DATE '2019-09-01', 'system');

INSERT INTO immunizations (id, patient_id, vaccine_name, dose_number, administered_on, administered_by, lot_number, created_by) VALUES (nextval('immunizations_seq'), (SELECT id FROM patients WHERE mrn = 'CC-2026-000001'), 'Influenza (seasonal)', 1, DATE '2025-10-15', 'Priya Nair', 'FLU-25-1182', 'system');
INSERT INTO immunizations (id, patient_id, vaccine_name, dose_number, administered_on, administered_by, lot_number, created_by) VALUES (nextval('immunizations_seq'), (SELECT id FROM patients WHERE mrn = 'CC-2026-000001'), 'COVID-19 (Covishield)', 2, DATE '2021-08-20', 'Govt. vaccination centre', 'CS-21-7731', 'system');
INSERT INTO immunizations (id, patient_id, vaccine_name, dose_number, administered_on, administered_by, lot_number, created_by) VALUES (nextval('immunizations_seq'), (SELECT id FROM patients WHERE mrn = 'CC-2026-000005'), 'MMR', 2, DATE '2021-11-20', 'Pediatric clinic', 'MMR-21-0923', 'system');
INSERT INTO immunizations (id, patient_id, vaccine_name, dose_number, administered_on, administered_by, lot_number, created_by) VALUES (nextval('immunizations_seq'), (SELECT id FROM patients WHERE mrn = 'CC-2026-000005'), 'DTaP booster', 5, DATE '2021-12-05', 'Pediatric clinic', 'DTP-21-4410', 'system');

INSERT INTO family_history (id, patient_id, relation, condition_name, created_by) VALUES (nextval('family_history_seq'), (SELECT id FROM patients WHERE mrn = 'CC-2026-000001'), 'Father', 'Type 2 diabetes mellitus', 'system');
INSERT INTO family_history (id, patient_id, relation, condition_name, created_by) VALUES (nextval('family_history_seq'), (SELECT id FROM patients WHERE mrn = 'CC-2026-000001'), 'Mother', 'Hypertension', 'system');
INSERT INTO family_history (id, patient_id, relation, condition_name, created_by) VALUES (nextval('family_history_seq'), (SELECT id FROM patients WHERE mrn = 'CC-2026-000002'), 'Mother', 'Bronchial asthma', 'system');
INSERT INTO family_history (id, patient_id, relation, condition_name, notes, created_by) VALUES (nextval('family_history_seq'), (SELECT id FROM patients WHERE mrn = 'CC-2026-000003'), 'Brother', 'Coronary artery disease', 'Myocardial infarction at age 58', 'system');

--changeset careconnect:024-encounters
INSERT INTO encounters (id, patient_id, provider_id, department_id, encounter_type, status, encounter_date, chief_complaint, closed_at, created_by)
VALUES (nextval('encounters_seq'), (SELECT id FROM patients WHERE mrn = 'CC-2026-000001'), (SELECT id FROM app_users WHERE username = 'dr.mehta'), (SELECT id FROM departments WHERE code = 'GEN'), 'FOLLOW_UP', 'CLOSED', TIMESTAMP '2026-08-12 10:00:00', 'Diabetes and blood pressure review', TIMESTAMP '2026-08-12 10:40:00', 'dr.mehta');
INSERT INTO encounters (id, patient_id, provider_id, department_id, encounter_type, status, encounter_date, chief_complaint, closed_at, created_by)
VALUES (nextval('encounters_seq'), (SELECT id FROM patients WHERE mrn = 'CC-2026-000001'), (SELECT id FROM app_users WHERE username = 'dr.mehta'), (SELECT id FROM departments WHERE code = 'GEN'), 'OUTPATIENT', 'CLOSED', TIMESTAMP '2026-09-10 10:30:00', 'Routine diabetes follow-up, occasional headache', TIMESTAMP '2026-09-10 11:15:00', 'dr.mehta');
INSERT INTO encounters (id, patient_id, provider_id, department_id, encounter_type, status, encounter_date, chief_complaint, closed_at, created_by)
VALUES (nextval('encounters_seq'), (SELECT id FROM patients WHERE mrn = 'CC-2026-000003'), (SELECT id FROM app_users WHERE username = 'dr.rao'), (SELECT id FROM departments WHERE code = 'CARD'), 'OUTPATIENT', 'CLOSED', TIMESTAMP '2026-09-15 11:00:00', 'Palpitations follow-up and INR review', TIMESTAMP '2026-09-15 11:30:00', 'dr.rao');
INSERT INTO encounters (id, patient_id, provider_id, department_id, encounter_type, status, encounter_date, chief_complaint, created_by)
VALUES (nextval('encounters_seq'), (SELECT id FROM patients WHERE mrn = 'CC-2026-000002'), (SELECT id FROM app_users WHERE username = 'dr.mehta'), (SELECT id FROM departments WHERE code = 'GEN'), 'OUTPATIENT', 'OPEN', TIMESTAMP '2026-09-28 09:30:00', 'Wheezing and dry cough for 3 days', 'dr.mehta');

--changeset careconnect:025-notes-diagnoses
INSERT INTO clinical_notes (id, encounter_id, patient_id, author_id, note_type, template_code, subjective, objective, assessment, plan_text, status, signed_at, signed_by, created_by)
VALUES (nextval('clinical_notes_seq'),
        (SELECT id FROM encounters WHERE encounter_date = TIMESTAMP '2026-08-12 10:00:00'),
        (SELECT id FROM patients WHERE mrn = 'CC-2026-000001'), (SELECT id FROM app_users WHERE username = 'dr.mehta'), 'PROGRESS', 'DM_FOLLOWUP',
        'Home fasting glucose 140-160 mg/dL. No hypoglycemia. Irregular walking. Taking metformin regularly.',
        'Weight 83 kg, BMI 28.1. BP 142/90. Feet: no ulcers, sensation intact.',
        'Type 2 diabetes mellitus, fair control. Hypertension, not at target.',
        'Continue metformin 500 mg twice daily. Start amlodipine 5 mg once daily. HbA1c and fasting glucose before next visit. Diet counselling given.',
        'SIGNED', TIMESTAMP '2026-08-12 10:40:00', (SELECT id FROM app_users WHERE username = 'dr.mehta'), 'dr.mehta');
INSERT INTO clinical_notes (id, encounter_id, patient_id, author_id, note_type, template_code, subjective, objective, assessment, plan_text, status, signed_at, signed_by, created_by)
VALUES (nextval('clinical_notes_seq'),
        (SELECT id FROM encounters WHERE encounter_date = TIMESTAMP '2026-09-10 10:30:00'),
        (SELECT id FROM patients WHERE mrn = 'CC-2026-000001'), (SELECT id FROM app_users WHERE username = 'dr.mehta'), 'PROGRESS', 'DM_FOLLOWUP',
        'Occasional morning headache. Fasting glucose 130-150 mg/dL at home. Adherent to medicines.',
        'Weight 82 kg, BMI 27.7. BP 136/86, pulse 78. Chest clear.',
        'Type 2 diabetes mellitus with hyperglycemia. Hypertension improving on amlodipine.',
        'HbA1c and fasting blood sugar today. Continue metformin and amlodipine. Review in 4 weeks with reports.',
        'SIGNED', TIMESTAMP '2026-09-10 11:15:00', (SELECT id FROM app_users WHERE username = 'dr.mehta'), 'dr.mehta');
INSERT INTO clinical_notes (id, encounter_id, patient_id, author_id, note_type, template_code, subjective, objective, assessment, plan_text, status, signed_at, signed_by, created_by)
VALUES (nextval('clinical_notes_seq'),
        (SELECT id FROM encounters WHERE encounter_date = TIMESTAMP '2026-09-15 11:00:00'),
        (SELECT id FROM patients WHERE mrn = 'CC-2026-000003'), (SELECT id FROM app_users WHERE username = 'dr.rao'), 'PROGRESS', 'HTN_FOLLOWUP',
        'Occasional palpitations, no syncope or chest pain. No bleeding episodes on warfarin.',
        'Pulse 92 irregularly irregular. BP 128/80. No signs of heart failure.',
        'Atrial fibrillation, rate reasonably controlled. INR above lab normal, within therapeutic target (2-3).',
        'Continue warfarin 5 mg once daily. Repeat INR in 2 weeks. Avoid NSAIDs.',
        'SIGNED', TIMESTAMP '2026-09-15 11:30:00', (SELECT id FROM app_users WHERE username = 'dr.rao'), 'dr.rao');
INSERT INTO clinical_notes (id, encounter_id, patient_id, author_id, note_type, template_code, subjective, objective, assessment, plan_text, status, created_by)
VALUES (nextval('clinical_notes_seq'),
        (SELECT id FROM encounters WHERE encounter_date = TIMESTAMP '2026-09-28 09:30:00'),
        (SELECT id FROM patients WHERE mrn = 'CC-2026-000002'), (SELECT id FROM app_users WHERE username = 'dr.mehta'), 'PROGRESS', 'GENERAL_OPD',
        'Wheezing and dry cough for 3 days after exposure to dust. Night symptoms twice.',
        'RR 22/min, SpO2 94% on room air. Bilateral expiratory wheeze.',
        'Acute exacerbation of bronchial asthma, mild to moderate.',
        'Salbutamol inhaler as needed. Chest X-ray. Review in 3 days.',
        'DRAFT', 'dr.mehta');

INSERT INTO encounter_diagnoses (id, encounter_id, icd10_code, diagnosis_type, created_by) VALUES (nextval('encounter_diagnoses_seq'), (SELECT id FROM encounters WHERE encounter_date = TIMESTAMP '2026-08-12 10:00:00'), 'E11.9', 'PRIMARY', 'dr.mehta');
INSERT INTO encounter_diagnoses (id, encounter_id, icd10_code, diagnosis_type, created_by) VALUES (nextval('encounter_diagnoses_seq'), (SELECT id FROM encounters WHERE encounter_date = TIMESTAMP '2026-08-12 10:00:00'), 'I10', 'SECONDARY', 'dr.mehta');
INSERT INTO encounter_diagnoses (id, encounter_id, icd10_code, diagnosis_type, created_by) VALUES (nextval('encounter_diagnoses_seq'), (SELECT id FROM encounters WHERE encounter_date = TIMESTAMP '2026-09-10 10:30:00'), 'E11.65', 'PRIMARY', 'dr.mehta');
INSERT INTO encounter_diagnoses (id, encounter_id, icd10_code, diagnosis_type, created_by) VALUES (nextval('encounter_diagnoses_seq'), (SELECT id FROM encounters WHERE encounter_date = TIMESTAMP '2026-09-10 10:30:00'), 'I10', 'SECONDARY', 'dr.mehta');
INSERT INTO encounter_diagnoses (id, encounter_id, icd10_code, diagnosis_type, created_by) VALUES (nextval('encounter_diagnoses_seq'), (SELECT id FROM encounters WHERE encounter_date = TIMESTAMP '2026-09-15 11:00:00'), 'I48.91', 'PRIMARY', 'dr.rao');
INSERT INTO encounter_diagnoses (id, encounter_id, icd10_code, diagnosis_type, created_by) VALUES (nextval('encounter_diagnoses_seq'), (SELECT id FROM encounters WHERE encounter_date = TIMESTAMP '2026-09-28 09:30:00'), 'J45.909', 'PRIMARY', 'dr.mehta');

--changeset careconnect:026-vitals
INSERT INTO vitals (id, patient_id, recorded_by, recorded_at, systolic, diastolic, pulse, temperature, resp_rate, spo2, weight_kg, height_cm, bmi, created_by)
VALUES (nextval('vitals_seq'), (SELECT id FROM patients WHERE mrn = 'CC-2026-000001'), (SELECT id FROM app_users WHERE username = 'nurse.priya'), TIMESTAMP '2026-06-10 09:45:00', 148, 94, 84, 36.8, 16, 97, 84.0, 172.0, 28.4, 'nurse.priya');
INSERT INTO vitals (id, patient_id, encounter_id, recorded_by, recorded_at, systolic, diastolic, pulse, temperature, resp_rate, spo2, weight_kg, height_cm, bmi, created_by)
VALUES (nextval('vitals_seq'), (SELECT id FROM patients WHERE mrn = 'CC-2026-000001'), (SELECT id FROM encounters WHERE encounter_date = TIMESTAMP '2026-08-12 10:00:00'), (SELECT id FROM app_users WHERE username = 'nurse.priya'), TIMESTAMP '2026-08-12 09:50:00', 142, 90, 82, 36.7, 16, 98, 83.0, 172.0, 28.1, 'nurse.priya');
INSERT INTO vitals (id, patient_id, encounter_id, recorded_by, recorded_at, systolic, diastolic, pulse, temperature, resp_rate, spo2, weight_kg, height_cm, bmi, created_by)
VALUES (nextval('vitals_seq'), (SELECT id FROM patients WHERE mrn = 'CC-2026-000001'), (SELECT id FROM encounters WHERE encounter_date = TIMESTAMP '2026-09-10 10:30:00'), (SELECT id FROM app_users WHERE username = 'nurse.priya'), TIMESTAMP '2026-09-10 10:20:00', 136, 86, 78, 36.9, 15, 98, 82.0, 172.0, 27.7, 'nurse.priya');
INSERT INTO vitals (id, patient_id, encounter_id, recorded_by, recorded_at, systolic, diastolic, pulse, temperature, resp_rate, spo2, weight_kg, height_cm, bmi, created_by)
VALUES (nextval('vitals_seq'), (SELECT id FROM patients WHERE mrn = 'CC-2026-000003'), (SELECT id FROM encounters WHERE encounter_date = TIMESTAMP '2026-09-15 11:00:00'), (SELECT id FROM app_users WHERE username = 'nurse.priya'), TIMESTAMP '2026-09-15 10:50:00', 128, 80, 92, 36.6, 18, 97, 76.0, 170.0, 26.3, 'nurse.priya');
INSERT INTO vitals (id, patient_id, encounter_id, recorded_by, recorded_at, systolic, diastolic, pulse, temperature, resp_rate, spo2, weight_kg, height_cm, bmi, created_by)
VALUES (nextval('vitals_seq'), (SELECT id FROM patients WHERE mrn = 'CC-2026-000002'), (SELECT id FROM encounters WHERE encounter_date = TIMESTAMP '2026-09-28 09:30:00'), (SELECT id FROM app_users WHERE username = 'nurse.priya'), TIMESTAMP '2026-09-28 09:20:00', 118, 76, 96, 37.4, 22, 94, 58.0, 160.0, 22.7, 'nurse.priya');

--changeset careconnect:027-orders
INSERT INTO clinical_orders (id, order_no, patient_id, encounter_id, ordered_by, order_type, status, priority, lab_test_id, clinical_indication, placed_at, started_at, completed_at, created_by)
VALUES (nextval('clinical_orders_seq'), 'ORD-000001', (SELECT id FROM patients WHERE mrn = 'CC-2026-000001'), (SELECT id FROM encounters WHERE encounter_date = TIMESTAMP '2026-09-10 10:30:00'), (SELECT id FROM app_users WHERE username = 'dr.mehta'),
        'LAB', 'COMPLETED', 'ROUTINE', (SELECT id FROM lab_tests WHERE code = 'HBA1C'), 'Diabetes monitoring', TIMESTAMP '2026-09-10 10:50:00', TIMESTAMP '2026-09-10 12:00:00', TIMESTAMP '2026-09-11 14:00:00', 'dr.mehta');
INSERT INTO clinical_orders (id, order_no, patient_id, encounter_id, ordered_by, order_type, status, priority, lab_test_id, clinical_indication, placed_at, started_at, completed_at, created_by)
VALUES (nextval('clinical_orders_seq'), 'ORD-000002', (SELECT id FROM patients WHERE mrn = 'CC-2026-000001'), (SELECT id FROM encounters WHERE encounter_date = TIMESTAMP '2026-09-10 10:30:00'), (SELECT id FROM app_users WHERE username = 'dr.mehta'),
        'LAB', 'COMPLETED', 'ROUTINE', (SELECT id FROM lab_tests WHERE code = 'FBS'), 'Diabetes monitoring', TIMESTAMP '2026-09-10 10:51:00', TIMESTAMP '2026-09-10 12:00:00', TIMESTAMP '2026-09-10 16:30:00', 'dr.mehta');
INSERT INTO clinical_orders (id, order_no, patient_id, encounter_id, ordered_by, order_type, status, priority, clinical_indication, placed_at, started_at, completed_at, created_by)
VALUES (nextval('clinical_orders_seq'), 'ORD-000003', (SELECT id FROM patients WHERE mrn = 'CC-2026-000001'), (SELECT id FROM encounters WHERE encounter_date = TIMESTAMP '2026-09-10 10:30:00'), (SELECT id FROM app_users WHERE username = 'dr.mehta'),
        'MEDICATION', 'COMPLETED', 'ROUTINE', 'Type 2 diabetes mellitus', TIMESTAMP '2026-09-10 11:00:00', TIMESTAMP '2026-09-10 11:20:00', TIMESTAMP '2026-09-10 11:40:00', 'dr.mehta');
INSERT INTO clinical_orders (id, order_no, patient_id, encounter_id, ordered_by, order_type, status, priority, clinical_indication, placed_at, started_at, completed_at, created_by)
VALUES (nextval('clinical_orders_seq'), 'ORD-000004', (SELECT id FROM patients WHERE mrn = 'CC-2026-000001'), (SELECT id FROM encounters WHERE encounter_date = TIMESTAMP '2026-09-10 10:30:00'), (SELECT id FROM app_users WHERE username = 'dr.mehta'),
        'MEDICATION', 'COMPLETED', 'ROUTINE', 'Essential hypertension', TIMESTAMP '2026-09-10 11:01:00', TIMESTAMP '2026-09-10 11:20:00', TIMESTAMP '2026-09-10 11:40:00', 'dr.mehta');
INSERT INTO clinical_orders (id, order_no, patient_id, encounter_id, ordered_by, order_type, status, priority, lab_test_id, clinical_indication, placed_at, started_at, completed_at, created_by)
VALUES (nextval('clinical_orders_seq'), 'ORD-000005', (SELECT id FROM patients WHERE mrn = 'CC-2026-000003'), (SELECT id FROM encounters WHERE encounter_date = TIMESTAMP '2026-09-15 11:00:00'), (SELECT id FROM app_users WHERE username = 'dr.rao'),
        'LAB', 'COMPLETED', 'URGENT', (SELECT id FROM lab_tests WHERE code = 'INR'), 'Warfarin monitoring', TIMESTAMP '2026-09-15 11:10:00', TIMESTAMP '2026-09-15 11:30:00', TIMESTAMP '2026-09-15 15:00:00', 'dr.rao');
INSERT INTO clinical_orders (id, order_no, patient_id, encounter_id, ordered_by, order_type, status, priority, clinical_indication, placed_at, started_at, created_by)
VALUES (nextval('clinical_orders_seq'), 'ORD-000006', (SELECT id FROM patients WHERE mrn = 'CC-2026-000003'), (SELECT id FROM encounters WHERE encounter_date = TIMESTAMP '2026-09-15 11:00:00'), (SELECT id FROM app_users WHERE username = 'dr.rao'),
        'MEDICATION', 'IN_PROGRESS', 'ROUTINE', 'Atrial fibrillation - stroke prevention', TIMESTAMP '2026-09-15 11:25:00', TIMESTAMP '2026-09-15 12:00:00', 'dr.rao');
INSERT INTO clinical_orders (id, order_no, patient_id, encounter_id, ordered_by, order_type, status, priority, clinical_indication, placed_at, created_by)
VALUES (nextval('clinical_orders_seq'), 'ORD-000007', (SELECT id FROM patients WHERE mrn = 'CC-2026-000002'), (SELECT id FROM encounters WHERE encounter_date = TIMESTAMP '2026-09-28 09:30:00'), (SELECT id FROM app_users WHERE username = 'dr.mehta'),
        'MEDICATION', 'PLACED', 'URGENT', 'Acute asthma exacerbation', TIMESTAMP '2026-09-28 09:55:00', 'dr.mehta');
INSERT INTO clinical_orders (id, order_no, patient_id, encounter_id, ordered_by, order_type, status, priority, imaging_study, body_site, clinical_indication, instructions, placed_at, created_by)
VALUES (nextval('clinical_orders_seq'), 'ORD-000008', (SELECT id FROM patients WHERE mrn = 'CC-2026-000002'), (SELECT id FROM encounters WHERE encounter_date = TIMESTAMP '2026-09-28 09:30:00'), (SELECT id FROM app_users WHERE username = 'dr.mehta'),
        'IMAGING', 'PLACED', 'URGENT', 'Chest X-ray PA view', 'Chest', 'Wheeze, rule out pneumonia', 'Patient can walk to radiology', TIMESTAMP '2026-09-28 09:56:00', 'dr.mehta');
INSERT INTO clinical_orders (id, order_no, patient_id, encounter_id, ordered_by, order_type, status, priority, lab_test_id, clinical_indication, placed_at, created_by)
VALUES (nextval('clinical_orders_seq'), 'ORD-000009', (SELECT id FROM patients WHERE mrn = 'CC-2026-000002'), (SELECT id FROM encounters WHERE encounter_date = TIMESTAMP '2026-09-28 09:30:00'), (SELECT id FROM app_users WHERE username = 'dr.mehta'),
        'LAB', 'PLACED', 'ROUTINE', (SELECT id FROM lab_tests WHERE code = 'WBC'), 'Rule out infection', TIMESTAMP '2026-09-28 09:57:00', 'dr.mehta');

--changeset careconnect:028-lab-results
INSERT INTO lab_results (id, order_id, lab_test_id, value_numeric, unit, reference_range, flag, result_status, comments, resulted_at, resulted_by, created_by)
VALUES (nextval('lab_results_seq'), (SELECT id FROM clinical_orders WHERE order_no = 'ORD-000001'), (SELECT id FROM lab_tests WHERE code = 'HBA1C'), 7.8, '%', '4.0 - 5.6', 'HIGH', 'FINAL', 'Suboptimal glycemic control', TIMESTAMP '2026-09-11 14:00:00', (SELECT id FROM app_users WHERE username = 'admin'), 'admin');
INSERT INTO lab_results (id, order_id, lab_test_id, value_numeric, unit, reference_range, flag, result_status, resulted_at, resulted_by, created_by)
VALUES (nextval('lab_results_seq'), (SELECT id FROM clinical_orders WHERE order_no = 'ORD-000002'), (SELECT id FROM lab_tests WHERE code = 'FBS'), 142, 'mg/dL', '70 - 100', 'HIGH', 'FINAL', TIMESTAMP '2026-09-10 16:30:00', (SELECT id FROM app_users WHERE username = 'admin'), 'admin');
INSERT INTO lab_results (id, order_id, lab_test_id, value_numeric, unit, reference_range, flag, result_status, comments, resulted_at, resulted_by, created_by)
VALUES (nextval('lab_results_seq'), (SELECT id FROM clinical_orders WHERE order_no = 'ORD-000005'), (SELECT id FROM lab_tests WHERE code = 'INR'), 2.6, 'ratio', '0.8 - 1.2', 'HIGH', 'FINAL', 'Within warfarin therapeutic range 2.0 - 3.0', TIMESTAMP '2026-09-15 15:00:00', (SELECT id FROM app_users WHERE username = 'admin'), 'admin');

--changeset careconnect:029-prescriptions
INSERT INTO prescriptions (id, rx_no, order_id, patient_id, encounter_id, prescriber_id, drug_id, dose, route, frequency, duration_days, quantity, instructions, status, verified_by, verified_at, dispensed_by, dispensed_at, dispensed_qty, created_by)
VALUES (nextval('prescriptions_seq'), 'RX-000001', (SELECT id FROM clinical_orders WHERE order_no = 'ORD-000003'), (SELECT id FROM patients WHERE mrn = 'CC-2026-000001'), (SELECT id FROM encounters WHERE encounter_date = TIMESTAMP '2026-09-10 10:30:00'),
        (SELECT id FROM app_users WHERE username = 'dr.mehta'), (SELECT id FROM drugs WHERE code = 'MET500'), '500 mg', 'ORAL', 'BD (twice daily)', 30, 60, 'Take after meals',
        'DISPENSED', (SELECT id FROM app_users WHERE username = 'pharm.arjun'), TIMESTAMP '2026-09-10 11:20:00', (SELECT id FROM app_users WHERE username = 'pharm.arjun'), TIMESTAMP '2026-09-10 11:40:00', 60, 'dr.mehta');
INSERT INTO prescriptions (id, rx_no, order_id, patient_id, encounter_id, prescriber_id, drug_id, dose, route, frequency, duration_days, quantity, instructions, status, verified_by, verified_at, dispensed_by, dispensed_at, dispensed_qty, created_by)
VALUES (nextval('prescriptions_seq'), 'RX-000002', (SELECT id FROM clinical_orders WHERE order_no = 'ORD-000004'), (SELECT id FROM patients WHERE mrn = 'CC-2026-000001'), (SELECT id FROM encounters WHERE encounter_date = TIMESTAMP '2026-09-10 10:30:00'),
        (SELECT id FROM app_users WHERE username = 'dr.mehta'), (SELECT id FROM drugs WHERE code = 'AMLO5'), '5 mg', 'ORAL', 'OD (once daily)', 30, 30, 'Take in the morning',
        'DISPENSED', (SELECT id FROM app_users WHERE username = 'pharm.arjun'), TIMESTAMP '2026-09-10 11:20:00', (SELECT id FROM app_users WHERE username = 'pharm.arjun'), TIMESTAMP '2026-09-10 11:40:00', 30, 'dr.mehta');
INSERT INTO prescriptions (id, rx_no, order_id, patient_id, encounter_id, prescriber_id, drug_id, dose, route, frequency, duration_days, quantity, instructions, status, verified_by, verified_at, pharmacist_note, created_by)
VALUES (nextval('prescriptions_seq'), 'RX-000003', (SELECT id FROM clinical_orders WHERE order_no = 'ORD-000006'), (SELECT id FROM patients WHERE mrn = 'CC-2026-000003'), (SELECT id FROM encounters WHERE encounter_date = TIMESTAMP '2026-09-15 11:00:00'),
        (SELECT id FROM app_users WHERE username = 'dr.rao'), (SELECT id FROM drugs WHERE code = 'WARF5'), '5 mg', 'ORAL', 'OD (once daily)', 30, 30, 'Take at the same time every evening. Avoid NSAIDs.',
        'VERIFIED', (SELECT id FROM app_users WHERE username = 'pharm.arjun'), TIMESTAMP '2026-09-15 12:00:00', 'INR 2.6 reviewed - dose appropriate', 'dr.rao');
INSERT INTO prescriptions (id, rx_no, order_id, patient_id, encounter_id, prescriber_id, drug_id, dose, route, frequency, duration_days, quantity, instructions, status, created_by)
VALUES (nextval('prescriptions_seq'), 'RX-000004', (SELECT id FROM clinical_orders WHERE order_no = 'ORD-000007'), (SELECT id FROM patients WHERE mrn = 'CC-2026-000002'), (SELECT id FROM encounters WHERE encounter_date = TIMESTAMP '2026-09-28 09:30:00'),
        (SELECT id FROM app_users WHERE username = 'dr.mehta'), (SELECT id FROM drugs WHERE code = 'SALB'), '2 puffs', 'INHALED', 'PRN (as needed)', 30, 1, 'Use spacer. Up to 4 times a day for wheeze.',
        'PENDING_VERIFICATION', 'dr.mehta');

INSERT INTO medication_administrations (id, prescription_id, patient_id, administered_by, administered_at, dose_given, route, admin_status, notes, created_by)
VALUES (nextval('med_admins_seq'), (SELECT id FROM prescriptions WHERE rx_no = 'RX-000001'), (SELECT id FROM patients WHERE mrn = 'CC-2026-000001'), (SELECT id FROM app_users WHERE username = 'nurse.priya'), TIMESTAMP '2026-09-10 11:50:00', '500 mg', 'ORAL', 'GIVEN', 'First dose given in clinic after meal', 'nurse.priya');

INSERT INTO inventory_transactions (id, drug_id, change_qty, balance_after, txn_type, reference, performed_by, created_at)
VALUES (nextval('inventory_txns_seq'), (SELECT id FROM drugs WHERE code = 'MET500'), -60, 800, 'DISPENSE', 'RX-000001', (SELECT id FROM app_users WHERE username = 'pharm.arjun'), TIMESTAMP '2026-09-10 11:40:00');
INSERT INTO inventory_transactions (id, drug_id, change_qty, balance_after, txn_type, reference, performed_by, created_at)
VALUES (nextval('inventory_txns_seq'), (SELECT id FROM drugs WHERE code = 'AMLO5'), -30, 500, 'DISPENSE', 'RX-000002', (SELECT id FROM app_users WHERE username = 'pharm.arjun'), TIMESTAMP '2026-09-10 11:40:00');

--changeset careconnect:030-messages-notifications
INSERT INTO messages (id, patient_id, sender_id, recipient_id, subject, body, read_at, created_at)
VALUES (nextval('messages_seq'), (SELECT id FROM patients WHERE mrn = 'CC-2026-000001'), (SELECT id FROM app_users WHERE username = 'patient.ravi'), (SELECT id FROM app_users WHERE username = 'dr.mehta'),
        'Metformin timing', 'Doctor, should I take metformin before or after breakfast? I sometimes feel nauseous.', TIMESTAMP '2026-09-12 18:10:00', TIMESTAMP '2026-09-12 08:30:00');
INSERT INTO messages (id, patient_id, sender_id, recipient_id, subject, body, created_at)
VALUES (nextval('messages_seq'), (SELECT id FROM patients WHERE mrn = 'CC-2026-000001'), (SELECT id FROM app_users WHERE username = 'dr.mehta'), (SELECT id FROM app_users WHERE username = 'patient.ravi'),
        'Re: Metformin timing', 'Please take it right after breakfast and dinner. The nausea usually settles in 1-2 weeks. Message me if it continues.', TIMESTAMP '2026-09-12 18:15:00');

INSERT INTO notifications (id, user_id, title, message, notif_type, link, created_at)
VALUES (nextval('notifications_seq'), (SELECT id FROM app_users WHERE username = 'dr.mehta'), 'Abnormal lab result', 'HbA1c 7.8 % (HIGH) for Ravi Kumar (CC-2026-000001).', 'LAB_RESULT', '/patients/' || (SELECT id FROM patients WHERE mrn = 'CC-2026-000001'), TIMESTAMP '2026-09-11 14:00:00');
INSERT INTO notifications (id, user_id, title, message, notif_type, link, created_at)
VALUES (nextval('notifications_seq'), (SELECT id FROM app_users WHERE username = 'pharm.arjun'), 'Prescription awaiting verification', 'RX-000004 Salbutamol Inhaler for Sita Verma needs verification.', 'PRESCRIPTION', '/pharmacy', TIMESTAMP '2026-09-28 09:55:00');
INSERT INTO notifications (id, user_id, title, message, notif_type, link, created_at)
VALUES (nextval('notifications_seq'), (SELECT id FROM app_users WHERE username = 'pharm.arjun'), 'Low stock', 'Clopidogrel 75 mg, Insulin Glargine and Ondansetron 4 mg are below reorder level.', 'LOW_STOCK', '/pharmacy/inventory', TIMESTAMP '2026-09-28 08:00:00');
