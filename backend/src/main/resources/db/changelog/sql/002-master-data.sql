--liquibase formatted sql

-- =====================================================================
-- Master data: departments, ICD-10 codes, lab tests, drugs,
-- drug-drug interactions and SOAP note templates.
-- ICD-10 codes, reference ranges and interactions are a teaching sample,
-- not a complete clinical knowledge base.
-- =====================================================================

--changeset careconnect:010-departments
INSERT INTO departments (id, code, name, description, created_by) VALUES (departments_seq.NEXTVAL, 'GEN', 'General Medicine', 'Adult outpatient and inpatient medicine', 'system');
INSERT INTO departments (id, code, name, description, created_by) VALUES (departments_seq.NEXTVAL, 'CARD', 'Cardiology', 'Heart and vascular care', 'system');
INSERT INTO departments (id, code, name, description, created_by) VALUES (departments_seq.NEXTVAL, 'PED', 'Pediatrics', 'Child health', 'system');
INSERT INTO departments (id, code, name, description, created_by) VALUES (departments_seq.NEXTVAL, 'ORTH', 'Orthopedics', 'Bone and joint care', 'system');
INSERT INTO departments (id, code, name, description, created_by) VALUES (departments_seq.NEXTVAL, 'NURS', 'Nursing', 'Ward and OPD nursing services', 'system');
INSERT INTO departments (id, code, name, description, created_by) VALUES (departments_seq.NEXTVAL, 'PHAR', 'Pharmacy', 'Dispensing and drug inventory', 'system');
INSERT INTO departments (id, code, name, description, created_by) VALUES (departments_seq.NEXTVAL, 'LAB', 'Laboratory', 'Pathology and clinical laboratory', 'system');
INSERT INTO departments (id, code, name, description, created_by) VALUES (departments_seq.NEXTVAL, 'RAD', 'Radiology', 'Imaging services', 'system');
INSERT INTO departments (id, code, name, description, created_by) VALUES (departments_seq.NEXTVAL, 'FRONT', 'Front Office', 'Registration and scheduling', 'system');
INSERT INTO departments (id, code, name, description, created_by) VALUES (departments_seq.NEXTVAL, 'ADMN', 'Administration', 'Hospital administration and IT', 'system');

--changeset careconnect:011-icd10
INSERT INTO icd10_codes (code, description, category) VALUES ('I10', 'Essential (primary) hypertension', 'Circulatory system');
INSERT INTO icd10_codes (code, description, category) VALUES ('I20.9', 'Angina pectoris, unspecified', 'Circulatory system');
INSERT INTO icd10_codes (code, description, category) VALUES ('I21.9', 'Acute myocardial infarction, unspecified', 'Circulatory system');
INSERT INTO icd10_codes (code, description, category) VALUES ('I48.91', 'Unspecified atrial fibrillation', 'Circulatory system');
INSERT INTO icd10_codes (code, description, category) VALUES ('I50.9', 'Heart failure, unspecified', 'Circulatory system');
INSERT INTO icd10_codes (code, description, category) VALUES ('E11.9', 'Type 2 diabetes mellitus without complications', 'Endocrine and metabolic');
INSERT INTO icd10_codes (code, description, category) VALUES ('E11.65', 'Type 2 diabetes mellitus with hyperglycemia', 'Endocrine and metabolic');
INSERT INTO icd10_codes (code, description, category) VALUES ('E78.5', 'Hyperlipidemia, unspecified', 'Endocrine and metabolic');
INSERT INTO icd10_codes (code, description, category) VALUES ('E03.9', 'Hypothyroidism, unspecified', 'Endocrine and metabolic');
INSERT INTO icd10_codes (code, description, category) VALUES ('E66.9', 'Obesity, unspecified', 'Endocrine and metabolic');
INSERT INTO icd10_codes (code, description, category) VALUES ('J06.9', 'Acute upper respiratory infection, unspecified', 'Respiratory system');
INSERT INTO icd10_codes (code, description, category) VALUES ('J02.9', 'Acute pharyngitis, unspecified', 'Respiratory system');
INSERT INTO icd10_codes (code, description, category) VALUES ('J18.9', 'Pneumonia, unspecified organism', 'Respiratory system');
INSERT INTO icd10_codes (code, description, category) VALUES ('J45.909', 'Unspecified asthma, uncomplicated', 'Respiratory system');
INSERT INTO icd10_codes (code, description, category) VALUES ('J44.9', 'Chronic obstructive pulmonary disease, unspecified', 'Respiratory system');
INSERT INTO icd10_codes (code, description, category) VALUES ('A09', 'Infectious gastroenteritis and colitis, unspecified', 'Infectious diseases');
INSERT INTO icd10_codes (code, description, category) VALUES ('A90', 'Dengue fever (classical dengue)', 'Infectious diseases');
INSERT INTO icd10_codes (code, description, category) VALUES ('B54', 'Unspecified malaria', 'Infectious diseases');
INSERT INTO icd10_codes (code, description, category) VALUES ('A01.00', 'Typhoid fever, unspecified', 'Infectious diseases');
INSERT INTO icd10_codes (code, description, category) VALUES ('R50.9', 'Fever, unspecified', 'Symptoms and signs');
INSERT INTO icd10_codes (code, description, category) VALUES ('R51.9', 'Headache, unspecified', 'Symptoms and signs');
INSERT INTO icd10_codes (code, description, category) VALUES ('R05.9', 'Cough, unspecified', 'Symptoms and signs');
INSERT INTO icd10_codes (code, description, category) VALUES ('R07.9', 'Chest pain, unspecified', 'Symptoms and signs');
INSERT INTO icd10_codes (code, description, category) VALUES ('R10.9', 'Unspecified abdominal pain', 'Symptoms and signs');
INSERT INTO icd10_codes (code, description, category) VALUES ('N39.0', 'Urinary tract infection, site not specified', 'Genitourinary system');
INSERT INTO icd10_codes (code, description, category) VALUES ('N18.9', 'Chronic kidney disease, unspecified', 'Genitourinary system');
INSERT INTO icd10_codes (code, description, category) VALUES ('K21.9', 'Gastro-esophageal reflux disease without esophagitis', 'Digestive system');
INSERT INTO icd10_codes (code, description, category) VALUES ('K29.70', 'Gastritis, unspecified, without bleeding', 'Digestive system');
INSERT INTO icd10_codes (code, description, category) VALUES ('M54.50', 'Low back pain, unspecified', 'Musculoskeletal system');
INSERT INTO icd10_codes (code, description, category) VALUES ('M17.9', 'Osteoarthritis of knee, unspecified', 'Musculoskeletal system');
INSERT INTO icd10_codes (code, description, category) VALUES ('M81.0', 'Age-related osteoporosis without current pathological fracture', 'Musculoskeletal system');
INSERT INTO icd10_codes (code, description, category) VALUES ('G43.909', 'Migraine, unspecified, not intractable', 'Nervous system');
INSERT INTO icd10_codes (code, description, category) VALUES ('F32.9', 'Major depressive disorder, single episode, unspecified', 'Mental and behavioural');
INSERT INTO icd10_codes (code, description, category) VALUES ('F41.1', 'Generalized anxiety disorder', 'Mental and behavioural');
INSERT INTO icd10_codes (code, description, category) VALUES ('D64.9', 'Anemia, unspecified', 'Blood and immune');
INSERT INTO icd10_codes (code, description, category) VALUES ('L30.9', 'Dermatitis, unspecified', 'Skin');
INSERT INTO icd10_codes (code, description, category) VALUES ('H10.9', 'Unspecified conjunctivitis', 'Eye');
INSERT INTO icd10_codes (code, description, category) VALUES ('Z00.00', 'General adult medical examination without abnormal findings', 'Health status');
INSERT INTO icd10_codes (code, description, category) VALUES ('Z23', 'Encounter for immunization', 'Health status');

--changeset careconnect:012-lab-tests
INSERT INTO lab_tests (id, code, name, category, specimen, unit, normal_low, normal_high, critical_low, critical_high, turnaround_hours) VALUES (lab_tests_seq.NEXTVAL, 'HGB', 'Hemoglobin', 'Hematology', 'Whole blood', 'g/dL', 12.0, 17.5, 7.0, 20.0, 4);
INSERT INTO lab_tests (id, code, name, category, specimen, unit, normal_low, normal_high, critical_low, critical_high, turnaround_hours) VALUES (lab_tests_seq.NEXTVAL, 'WBC', 'White Blood Cell Count', 'Hematology', 'Whole blood', 'x10^3/uL', 4.0, 11.0, 2.0, 30.0, 4);
INSERT INTO lab_tests (id, code, name, category, specimen, unit, normal_low, normal_high, critical_low, critical_high, turnaround_hours) VALUES (lab_tests_seq.NEXTVAL, 'PLT', 'Platelet Count', 'Hematology', 'Whole blood', 'x10^3/uL', 150, 450, 50, 1000, 4);
INSERT INTO lab_tests (id, code, name, category, specimen, unit, normal_low, normal_high, critical_low, critical_high, turnaround_hours) VALUES (lab_tests_seq.NEXTVAL, 'FBS', 'Fasting Blood Sugar', 'Biochemistry', 'Serum', 'mg/dL', 70, 100, 40, 400, 6);
INSERT INTO lab_tests (id, code, name, category, specimen, unit, normal_low, normal_high, critical_low, critical_high, turnaround_hours) VALUES (lab_tests_seq.NEXTVAL, 'HBA1C', 'Glycated Hemoglobin (HbA1c)', 'Biochemistry', 'Whole blood', '%', 4.0, 5.6, NULL, NULL, 24);
INSERT INTO lab_tests (id, code, name, category, specimen, unit, normal_low, normal_high, critical_low, critical_high, turnaround_hours) VALUES (lab_tests_seq.NEXTVAL, 'CREAT', 'Serum Creatinine', 'Biochemistry', 'Serum', 'mg/dL', 0.6, 1.3, NULL, 5.0, 6);
INSERT INTO lab_tests (id, code, name, category, specimen, unit, normal_low, normal_high, critical_low, critical_high, turnaround_hours) VALUES (lab_tests_seq.NEXTVAL, 'K', 'Serum Potassium', 'Biochemistry', 'Serum', 'mmol/L', 3.5, 5.1, 2.5, 6.5, 4);
INSERT INTO lab_tests (id, code, name, category, specimen, unit, normal_low, normal_high, critical_low, critical_high, turnaround_hours) VALUES (lab_tests_seq.NEXTVAL, 'NA', 'Serum Sodium', 'Biochemistry', 'Serum', 'mmol/L', 135, 145, 120, 160, 4);
INSERT INTO lab_tests (id, code, name, category, specimen, unit, normal_low, normal_high, critical_low, critical_high, turnaround_hours) VALUES (lab_tests_seq.NEXTVAL, 'CHOL', 'Total Cholesterol', 'Biochemistry', 'Serum', 'mg/dL', 0, 200, NULL, NULL, 12);
INSERT INTO lab_tests (id, code, name, category, specimen, unit, normal_low, normal_high, critical_low, critical_high, turnaround_hours) VALUES (lab_tests_seq.NEXTVAL, 'LDL', 'LDL Cholesterol', 'Biochemistry', 'Serum', 'mg/dL', 0, 100, NULL, NULL, 12);
INSERT INTO lab_tests (id, code, name, category, specimen, unit, normal_low, normal_high, critical_low, critical_high, turnaround_hours) VALUES (lab_tests_seq.NEXTVAL, 'TSH', 'Thyroid Stimulating Hormone', 'Endocrinology', 'Serum', 'uIU/mL', 0.4, 4.0, NULL, NULL, 24);
INSERT INTO lab_tests (id, code, name, category, specimen, unit, normal_low, normal_high, critical_low, critical_high, turnaround_hours) VALUES (lab_tests_seq.NEXTVAL, 'ALT', 'Alanine Aminotransferase (ALT)', 'Biochemistry', 'Serum', 'U/L', 7, 56, NULL, 1000, 12);
INSERT INTO lab_tests (id, code, name, category, specimen, unit, normal_low, normal_high, critical_low, critical_high, turnaround_hours) VALUES (lab_tests_seq.NEXTVAL, 'INR', 'Prothrombin Time (INR)', 'Coagulation', 'Citrated plasma', 'ratio', 0.8, 1.2, NULL, 5.0, 4);
INSERT INTO lab_tests (id, code, name, category, specimen, unit, normal_low, normal_high, critical_low, critical_high, turnaround_hours) VALUES (lab_tests_seq.NEXTVAL, 'CRP', 'C-Reactive Protein', 'Immunology', 'Serum', 'mg/L', 0, 10, NULL, NULL, 12);
INSERT INTO lab_tests (id, code, name, category, specimen, unit, normal_low, normal_high, critical_low, critical_high, turnaround_hours) VALUES (lab_tests_seq.NEXTVAL, 'URINE', 'Urine Routine and Microscopy', 'Clinical Pathology', 'Urine', NULL, NULL, NULL, NULL, NULL, 6);
INSERT INTO lab_tests (id, code, name, category, specimen, unit, normal_low, normal_high, critical_low, critical_high, turnaround_hours) VALUES (lab_tests_seq.NEXTVAL, 'DENGUE', 'Dengue NS1 Antigen', 'Serology', 'Serum', NULL, NULL, NULL, NULL, NULL, 12);

--changeset careconnect:013-drugs
INSERT INTO drugs (id, code, name, generic_name, drug_class, dosage_form, strength, default_route, unit_price, stock_qty, reorder_level, created_by) VALUES (drugs_seq.NEXTVAL, 'AMOX500', 'Amoxicillin 500 mg', 'Amoxicillin', 'Penicillins', 'CAPSULE', '500 mg', 'ORAL', 4.50, 500, 100, 'system');
INSERT INTO drugs (id, code, name, generic_name, drug_class, dosage_form, strength, default_route, unit_price, stock_qty, reorder_level, created_by) VALUES (drugs_seq.NEXTVAL, 'AUGM625', 'Amoxicillin-Clavulanate 625 mg', 'Amoxicillin + Clavulanic acid', 'Penicillins', 'TABLET', '625 mg', 'ORAL', 18.00, 200, 50, 'system');
INSERT INTO drugs (id, code, name, generic_name, drug_class, dosage_form, strength, default_route, unit_price, stock_qty, reorder_level, created_by) VALUES (drugs_seq.NEXTVAL, 'AZI500', 'Azithromycin 500 mg', 'Azithromycin', 'Macrolides', 'TABLET', '500 mg', 'ORAL', 22.00, 150, 40, 'system');
INSERT INTO drugs (id, code, name, generic_name, drug_class, dosage_form, strength, default_route, unit_price, stock_qty, reorder_level, created_by) VALUES (drugs_seq.NEXTVAL, 'CLAR500', 'Clarithromycin 500 mg', 'Clarithromycin', 'Macrolides', 'TABLET', '500 mg', 'ORAL', 35.00, 60, 30, 'system');
INSERT INTO drugs (id, code, name, generic_name, drug_class, dosage_form, strength, default_route, unit_price, stock_qty, reorder_level, created_by) VALUES (drugs_seq.NEXTVAL, 'CIP500', 'Ciprofloxacin 500 mg', 'Ciprofloxacin', 'Fluoroquinolones', 'TABLET', '500 mg', 'ORAL', 6.00, 300, 60, 'system');
INSERT INTO drugs (id, code, name, generic_name, drug_class, dosage_form, strength, default_route, unit_price, stock_qty, reorder_level, created_by) VALUES (drugs_seq.NEXTVAL, 'CTRX1G', 'Ceftriaxone 1 g Injection', 'Ceftriaxone', 'Cephalosporins', 'INJECTION', '1 g', 'IV', 55.00, 80, 20, 'system');
INSERT INTO drugs (id, code, name, generic_name, drug_class, dosage_form, strength, default_route, unit_price, stock_qty, reorder_level, created_by) VALUES (drugs_seq.NEXTVAL, 'COTRI', 'Cotrimoxazole DS', 'Sulfamethoxazole + Trimethoprim', 'Sulfonamides', 'TABLET', '960 mg', 'ORAL', 3.00, 120, 30, 'system');
INSERT INTO drugs (id, code, name, generic_name, drug_class, dosage_form, strength, default_route, unit_price, stock_qty, reorder_level, created_by) VALUES (drugs_seq.NEXTVAL, 'PCM500', 'Paracetamol 500 mg', 'Paracetamol', 'Analgesics and antipyretics', 'TABLET', '500 mg', 'ORAL', 1.20, 1000, 200, 'system');
INSERT INTO drugs (id, code, name, generic_name, drug_class, dosage_form, strength, default_route, unit_price, stock_qty, reorder_level, created_by) VALUES (drugs_seq.NEXTVAL, 'IBU400', 'Ibuprofen 400 mg', 'Ibuprofen', 'NSAIDs', 'TABLET', '400 mg', 'ORAL', 2.00, 400, 100, 'system');
INSERT INTO drugs (id, code, name, generic_name, drug_class, dosage_form, strength, default_route, unit_price, stock_qty, reorder_level, created_by) VALUES (drugs_seq.NEXTVAL, 'ASP75', 'Aspirin 75 mg', 'Acetylsalicylic acid', 'NSAIDs and antiplatelets', 'TABLET', '75 mg', 'ORAL', 0.80, 600, 100, 'system');
INSERT INTO drugs (id, code, name, generic_name, drug_class, dosage_form, strength, default_route, unit_price, stock_qty, reorder_level, created_by) VALUES (drugs_seq.NEXTVAL, 'WARF5', 'Warfarin 5 mg', 'Warfarin', 'Anticoagulants', 'TABLET', '5 mg', 'ORAL', 3.50, 90, 30, 'system');
INSERT INTO drugs (id, code, name, generic_name, drug_class, dosage_form, strength, default_route, unit_price, stock_qty, reorder_level, created_by) VALUES (drugs_seq.NEXTVAL, 'CLOP75', 'Clopidogrel 75 mg', 'Clopidogrel', 'Antiplatelets', 'TABLET', '75 mg', 'ORAL', 5.00, 25, 40, 'system');
INSERT INTO drugs (id, code, name, generic_name, drug_class, dosage_form, strength, default_route, unit_price, stock_qty, reorder_level, created_by) VALUES (drugs_seq.NEXTVAL, 'MET500', 'Metformin 500 mg', 'Metformin', 'Biguanides', 'TABLET', '500 mg', 'ORAL', 1.50, 800, 150, 'system');
INSERT INTO drugs (id, code, name, generic_name, drug_class, dosage_form, strength, default_route, unit_price, stock_qty, reorder_level, created_by) VALUES (drugs_seq.NEXTVAL, 'GLIM2', 'Glimepiride 2 mg', 'Glimepiride', 'Sulfonylureas', 'TABLET', '2 mg', 'ORAL', 4.00, 250, 50, 'system');
INSERT INTO drugs (id, code, name, generic_name, drug_class, dosage_form, strength, default_route, unit_price, stock_qty, reorder_level, created_by) VALUES (drugs_seq.NEXTVAL, 'INSG', 'Insulin Glargine 100 IU/mL', 'Insulin glargine', 'Insulins', 'INJECTION', '100 IU/mL', 'SC', 750.00, 12, 15, 'system');
INSERT INTO drugs (id, code, name, generic_name, drug_class, dosage_form, strength, default_route, unit_price, stock_qty, reorder_level, created_by) VALUES (drugs_seq.NEXTVAL, 'ATOR20', 'Atorvastatin 20 mg', 'Atorvastatin', 'Statins', 'TABLET', '20 mg', 'ORAL', 6.00, 400, 80, 'system');
INSERT INTO drugs (id, code, name, generic_name, drug_class, dosage_form, strength, default_route, unit_price, stock_qty, reorder_level, created_by) VALUES (drugs_seq.NEXTVAL, 'SIMV20', 'Simvastatin 20 mg', 'Simvastatin', 'Statins', 'TABLET', '20 mg', 'ORAL', 5.00, 150, 40, 'system');
INSERT INTO drugs (id, code, name, generic_name, drug_class, dosage_form, strength, default_route, unit_price, stock_qty, reorder_level, created_by) VALUES (drugs_seq.NEXTVAL, 'AMLO5', 'Amlodipine 5 mg', 'Amlodipine', 'Calcium channel blockers', 'TABLET', '5 mg', 'ORAL', 2.00, 500, 100, 'system');
INSERT INTO drugs (id, code, name, generic_name, drug_class, dosage_form, strength, default_route, unit_price, stock_qty, reorder_level, created_by) VALUES (drugs_seq.NEXTVAL, 'LISI10', 'Lisinopril 10 mg', 'Lisinopril', 'ACE inhibitors', 'TABLET', '10 mg', 'ORAL', 3.00, 300, 60, 'system');
INSERT INTO drugs (id, code, name, generic_name, drug_class, dosage_form, strength, default_route, unit_price, stock_qty, reorder_level, created_by) VALUES (drugs_seq.NEXTVAL, 'LOSA50', 'Losartan 50 mg', 'Losartan', 'Angiotensin receptor blockers', 'TABLET', '50 mg', 'ORAL', 4.00, 350, 60, 'system');
INSERT INTO drugs (id, code, name, generic_name, drug_class, dosage_form, strength, default_route, unit_price, stock_qty, reorder_level, created_by) VALUES (drugs_seq.NEXTVAL, 'SPIR25', 'Spironolactone 25 mg', 'Spironolactone', 'Potassium-sparing diuretics', 'TABLET', '25 mg', 'ORAL', 3.00, 100, 30, 'system');
INSERT INTO drugs (id, code, name, generic_name, drug_class, dosage_form, strength, default_route, unit_price, stock_qty, reorder_level, created_by) VALUES (drugs_seq.NEXTVAL, 'OMEP20', 'Omeprazole 20 mg', 'Omeprazole', 'Proton pump inhibitors', 'CAPSULE', '20 mg', 'ORAL', 2.50, 600, 100, 'system');
INSERT INTO drugs (id, code, name, generic_name, drug_class, dosage_form, strength, default_route, unit_price, stock_qty, reorder_level, created_by) VALUES (drugs_seq.NEXTVAL, 'SALB', 'Salbutamol Inhaler 100 mcg', 'Salbutamol', 'Beta-2 agonists', 'INHALER', '100 mcg/dose', 'INHALED', 150.00, 40, 15, 'system');
INSERT INTO drugs (id, code, name, generic_name, drug_class, dosage_form, strength, default_route, unit_price, stock_qty, reorder_level, created_by) VALUES (drugs_seq.NEXTVAL, 'CETZ10', 'Cetirizine 10 mg', 'Cetirizine', 'Antihistamines', 'TABLET', '10 mg', 'ORAL', 1.00, 700, 100, 'system');
INSERT INTO drugs (id, code, name, generic_name, drug_class, dosage_form, strength, default_route, unit_price, stock_qty, reorder_level, created_by) VALUES (drugs_seq.NEXTVAL, 'ONDA4', 'Ondansetron 4 mg', 'Ondansetron', 'Antiemetics', 'TABLET', '4 mg', 'ORAL', 5.00, 8, 30, 'system');
INSERT INTO drugs (id, code, name, generic_name, drug_class, dosage_form, strength, default_route, unit_price, stock_qty, reorder_level, created_by) VALUES (drugs_seq.NEXTVAL, 'LEVO50', 'Levothyroxine 50 mcg', 'Levothyroxine', 'Thyroid hormones', 'TABLET', '50 mcg', 'ORAL', 1.80, 300, 60, 'system');

--changeset careconnect:014-drug-interactions
INSERT INTO drug_interactions (id, drug_a_id, drug_b_id, severity, description) VALUES (drug_interactions_seq.NEXTVAL, (SELECT id FROM drugs WHERE code = 'WARF5'), (SELECT id FROM drugs WHERE code = 'ASP75'), 'MAJOR', 'Increased bleeding risk: aspirin adds antiplatelet effect to warfarin anticoagulation.');
INSERT INTO drug_interactions (id, drug_a_id, drug_b_id, severity, description) VALUES (drug_interactions_seq.NEXTVAL, (SELECT id FROM drugs WHERE code = 'WARF5'), (SELECT id FROM drugs WHERE code = 'IBU400'), 'MAJOR', 'NSAIDs increase the risk of gastrointestinal and other bleeding with warfarin.');
INSERT INTO drug_interactions (id, drug_a_id, drug_b_id, severity, description) VALUES (drug_interactions_seq.NEXTVAL, (SELECT id FROM drugs WHERE code = 'WARF5'), (SELECT id FROM drugs WHERE code = 'CIP500'), 'MAJOR', 'Ciprofloxacin inhibits warfarin metabolism; INR may rise sharply. Monitor INR.');
INSERT INTO drug_interactions (id, drug_a_id, drug_b_id, severity, description) VALUES (drug_interactions_seq.NEXTVAL, (SELECT id FROM drugs WHERE code = 'WARF5'), (SELECT id FROM drugs WHERE code = 'CLAR500'), 'MAJOR', 'Clarithromycin increases the anticoagulant effect of warfarin. Monitor INR closely.');
INSERT INTO drug_interactions (id, drug_a_id, drug_b_id, severity, description) VALUES (drug_interactions_seq.NEXTVAL, (SELECT id FROM drugs WHERE code = 'WARF5'), (SELECT id FROM drugs WHERE code = 'COTRI'), 'MAJOR', 'Cotrimoxazole markedly potentiates warfarin; high risk of bleeding.');
INSERT INTO drug_interactions (id, drug_a_id, drug_b_id, severity, description) VALUES (drug_interactions_seq.NEXTVAL, (SELECT id FROM drugs WHERE code = 'WARF5'), (SELECT id FROM drugs WHERE code = 'CLOP75'), 'MAJOR', 'Combined anticoagulant and antiplatelet therapy increases bleeding risk.');
INSERT INTO drug_interactions (id, drug_a_id, drug_b_id, severity, description) VALUES (drug_interactions_seq.NEXTVAL, (SELECT id FROM drugs WHERE code = 'WARF5'), (SELECT id FROM drugs WHERE code = 'AZI500'), 'MODERATE', 'Azithromycin may increase INR in patients on warfarin. Monitor INR.');
INSERT INTO drug_interactions (id, drug_a_id, drug_b_id, severity, description) VALUES (drug_interactions_seq.NEXTVAL, (SELECT id FROM drugs WHERE code = 'SIMV20'), (SELECT id FROM drugs WHERE code = 'CLAR500'), 'CONTRAINDICATED', 'Clarithromycin greatly raises simvastatin levels; risk of rhabdomyolysis. Do not combine.');
INSERT INTO drug_interactions (id, drug_a_id, drug_b_id, severity, description) VALUES (drug_interactions_seq.NEXTVAL, (SELECT id FROM drugs WHERE code = 'ATOR20'), (SELECT id FROM drugs WHERE code = 'CLAR500'), 'MAJOR', 'Clarithromycin increases atorvastatin exposure and myopathy risk. Prefer azithromycin.');
INSERT INTO drug_interactions (id, drug_a_id, drug_b_id, severity, description) VALUES (drug_interactions_seq.NEXTVAL, (SELECT id FROM drugs WHERE code = 'LISI10'), (SELECT id FROM drugs WHERE code = 'SPIR25'), 'MAJOR', 'ACE inhibitor with potassium-sparing diuretic can cause dangerous hyperkalemia. Monitor potassium.');
INSERT INTO drug_interactions (id, drug_a_id, drug_b_id, severity, description) VALUES (drug_interactions_seq.NEXTVAL, (SELECT id FROM drugs WHERE code = 'LOSA50'), (SELECT id FROM drugs WHERE code = 'SPIR25'), 'MAJOR', 'ARB with potassium-sparing diuretic increases hyperkalemia risk. Monitor potassium and renal function.');
INSERT INTO drug_interactions (id, drug_a_id, drug_b_id, severity, description) VALUES (drug_interactions_seq.NEXTVAL, (SELECT id FROM drugs WHERE code = 'LISI10'), (SELECT id FROM drugs WHERE code = 'LOSA50'), 'MAJOR', 'Dual RAAS blockade raises the risk of hyperkalemia, hypotension and acute kidney injury.');
INSERT INTO drug_interactions (id, drug_a_id, drug_b_id, severity, description) VALUES (drug_interactions_seq.NEXTVAL, (SELECT id FROM drugs WHERE code = 'CLOP75'), (SELECT id FROM drugs WHERE code = 'OMEP20'), 'MODERATE', 'Omeprazole reduces activation of clopidogrel. Consider pantoprazole instead.');
INSERT INTO drug_interactions (id, drug_a_id, drug_b_id, severity, description) VALUES (drug_interactions_seq.NEXTVAL, (SELECT id FROM drugs WHERE code = 'ASP75'), (SELECT id FROM drugs WHERE code = 'IBU400'), 'MODERATE', 'Ibuprofen may reduce the cardioprotective effect of aspirin and adds GI bleeding risk.');
INSERT INTO drug_interactions (id, drug_a_id, drug_b_id, severity, description) VALUES (drug_interactions_seq.NEXTVAL, (SELECT id FROM drugs WHERE code = 'AMLO5'), (SELECT id FROM drugs WHERE code = 'SIMV20'), 'MODERATE', 'Amlodipine increases simvastatin exposure. Do not exceed simvastatin 20 mg daily.');
INSERT INTO drug_interactions (id, drug_a_id, drug_b_id, severity, description) VALUES (drug_interactions_seq.NEXTVAL, (SELECT id FROM drugs WHERE code = 'MET500'), (SELECT id FROM drugs WHERE code = 'CIP500'), 'MODERATE', 'Fluoroquinolones can cause blood glucose disturbances in diabetic patients.');
INSERT INTO drug_interactions (id, drug_a_id, drug_b_id, severity, description) VALUES (drug_interactions_seq.NEXTVAL, (SELECT id FROM drugs WHERE code = 'GLIM2'), (SELECT id FROM drugs WHERE code = 'CIP500'), 'MODERATE', 'Risk of severe hypoglycemia with sulfonylurea and ciprofloxacin. Monitor blood glucose.');

--changeset careconnect:015-note-templates
INSERT INTO note_templates (id, code, name, specialty, subjective, objective, assessment, plan_text) VALUES (note_templates_seq.NEXTVAL, 'GENERAL_OPD', 'General OPD visit', 'General Medicine',
 'Chief complaint:
History of present illness:
Relevant past history:',
 'General examination:
Systemic examination:',
 'Clinical impression:',
 'Investigations:
Medications:
Advice:
Follow-up:');
INSERT INTO note_templates (id, code, name, specialty, subjective, objective, assessment, plan_text) VALUES (note_templates_seq.NEXTVAL, 'FEVER', 'Fever evaluation', 'General Medicine',
 'Fever for __ days, pattern: continuous / intermittent. Chills: yes / no.
Associated symptoms: cough, sore throat, dysuria, rash, body ache.
Travel history / sick contacts:',
 'Temperature: __ C, pulse: __ /min.
Throat, chest and abdomen findings:
Rash / bleeding signs:',
 'Acute febrile illness - likely __',
 'CBC, dengue NS1 and malaria test as indicated.
Antipyretics and oral hydration.
Return immediately if bleeding, persistent vomiting or breathlessness.');
INSERT INTO note_templates (id, code, name, specialty, subjective, objective, assessment, plan_text) VALUES (note_templates_seq.NEXTVAL, 'DM_FOLLOWUP', 'Diabetes follow-up', 'General Medicine',
 'Home glucose readings:
Hypoglycemia episodes:
Diet and exercise adherence:
Medication adherence:',
 'Weight / BMI:
Blood pressure:
Foot examination:',
 'Type 2 diabetes mellitus - control: good / fair / poor',
 'HbA1c every 3 months, fasting glucose, lipid profile, creatinine.
Medication changes:
Diet counselling. Annual eye and foot review.');
INSERT INTO note_templates (id, code, name, specialty, subjective, objective, assessment, plan_text) VALUES (note_templates_seq.NEXTVAL, 'HTN_FOLLOWUP', 'Hypertension follow-up', 'Cardiology',
 'Home BP readings:
Headache, chest pain, breathlessness, palpitations:
Medication adherence and side effects:',
 'BP (two readings):
Pulse:
Cardiovascular examination:',
 'Essential hypertension - controlled / uncontrolled',
 'Continue / adjust antihypertensives.
Salt restriction, exercise, weight reduction.
Renal function and potassium if on ACE inhibitor or ARB.
Follow-up in 4 weeks.');
