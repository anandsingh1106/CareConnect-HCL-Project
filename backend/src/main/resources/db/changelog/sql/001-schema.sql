--liquibase formatted sql

-- =====================================================================
-- CareConnect EHR - schema
-- Portable Oracle SQL: runs on Oracle 10g XE through 23ai (and on H2 in
-- Oracle mode for automated tests). Primary keys come from sequences,
-- booleans are NUMBER(1) with CHECK (0,1), and identifiers stay <= 30 chars.
-- Audit columns on clinical tables: created_at/by, updated_at/by, version
-- (version drives JPA optimistic locking).
-- =====================================================================

--changeset careconnect:001-sequences
CREATE SEQUENCE departments_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE app_users_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE refresh_tokens_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE patients_seq START WITH 1 INCREMENT BY 1;
-- Business number sequences (MRN, order and prescription numbers); seed data uses lower numbers
CREATE SEQUENCE mrn_seq START WITH 100 INCREMENT BY 1;
CREATE SEQUENCE order_no_seq START WITH 100 INCREMENT BY 1;
CREATE SEQUENCE rx_no_seq START WITH 100 INCREMENT BY 1;
CREATE SEQUENCE patient_allergies_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE patient_problems_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE immunizations_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE family_history_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE patient_documents_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE note_templates_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE encounters_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE clinical_notes_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE encounter_diagnoses_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE vitals_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE lab_tests_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE drugs_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE drug_interactions_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE clinical_orders_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE order_alerts_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE lab_results_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE prescriptions_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE med_admins_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE inventory_txns_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE provider_slots_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE appointments_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE messages_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE notifications_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE audit_logs_seq START WITH 1 INCREMENT BY 1;

--changeset careconnect:002-admin-tables
CREATE TABLE departments (
    id           NUMBER(19)     NOT NULL,
    code         VARCHAR2(20)   NOT NULL,
    name         VARCHAR2(100)  NOT NULL,
    description  VARCHAR2(500),
    active       NUMBER(1)      DEFAULT 1 NOT NULL,
    created_at   TIMESTAMP      DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by   VARCHAR2(50),
    updated_at   TIMESTAMP,
    updated_by   VARCHAR2(50),
    version      NUMBER(10)     DEFAULT 0 NOT NULL,
    CONSTRAINT pk_departments PRIMARY KEY (id),
    CONSTRAINT uk_departments_code UNIQUE (code),
    CONSTRAINT ck_departments_active CHECK (active IN (0, 1))
);

CREATE TABLE app_users (
    id             NUMBER(19)    NOT NULL,
    username       VARCHAR2(50)  NOT NULL,
    email          VARCHAR2(120),
    password_hash  VARCHAR2(100) NOT NULL,
    full_name      VARCHAR2(120) NOT NULL,
    user_role      VARCHAR2(20)  NOT NULL,
    department_id  NUMBER(19),
    phone          VARCHAR2(20),
    specialty      VARCHAR2(100),
    license_no     VARCHAR2(50),
    patient_id     NUMBER(19),
    active         NUMBER(1)     DEFAULT 1 NOT NULL,
    last_login_at  TIMESTAMP,
    created_at     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by     VARCHAR2(50),
    updated_at     TIMESTAMP,
    updated_by     VARCHAR2(50),
    version        NUMBER(10)    DEFAULT 0 NOT NULL,
    CONSTRAINT pk_app_users PRIMARY KEY (id),
    CONSTRAINT uk_app_users_username UNIQUE (username),
    CONSTRAINT uk_app_users_email UNIQUE (email),
    CONSTRAINT uk_app_users_patient UNIQUE (patient_id),
    CONSTRAINT fk_app_users_dept FOREIGN KEY (department_id) REFERENCES departments (id),
    CONSTRAINT ck_app_users_role CHECK (user_role IN ('ADMIN', 'DOCTOR', 'NURSE', 'PHARMACIST', 'RECEPTIONIST', 'PATIENT')),
    CONSTRAINT ck_app_users_active CHECK (active IN (0, 1))
);
CREATE INDEX ix_app_users_role ON app_users (user_role);

CREATE TABLE refresh_tokens (
    id          NUMBER(19)   NOT NULL,
    user_id     NUMBER(19)   NOT NULL,
    token_hash  VARCHAR2(64) NOT NULL,
    expires_at  TIMESTAMP    NOT NULL,
    revoked     NUMBER(1)    DEFAULT 0 NOT NULL,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES app_users (id),
    CONSTRAINT ck_refresh_tokens_revoked CHECK (revoked IN (0, 1))
);
CREATE INDEX ix_refresh_tokens_user ON refresh_tokens (user_id);

--changeset careconnect:003-master-tables
CREATE TABLE icd10_codes (
    code         VARCHAR2(10)  NOT NULL,
    description  VARCHAR2(255) NOT NULL,
    category     VARCHAR2(100),
    active       NUMBER(1)     DEFAULT 1 NOT NULL,
    CONSTRAINT pk_icd10_codes PRIMARY KEY (code),
    CONSTRAINT ck_icd10_active CHECK (active IN (0, 1))
);
CREATE INDEX ix_icd10_description ON icd10_codes (description);

CREATE TABLE lab_tests (
    id                NUMBER(19)    NOT NULL,
    code              VARCHAR2(20)  NOT NULL,
    name              VARCHAR2(120) NOT NULL,
    category          VARCHAR2(60),
    specimen          VARCHAR2(60),
    unit              VARCHAR2(30),
    normal_low        NUMBER(12, 3),
    normal_high       NUMBER(12, 3),
    critical_low      NUMBER(12, 3),
    critical_high     NUMBER(12, 3),
    turnaround_hours  NUMBER(4),
    active            NUMBER(1)     DEFAULT 1 NOT NULL,
    CONSTRAINT pk_lab_tests PRIMARY KEY (id),
    CONSTRAINT uk_lab_tests_code UNIQUE (code),
    CONSTRAINT ck_lab_tests_active CHECK (active IN (0, 1))
);

CREATE TABLE drugs (
    id             NUMBER(19)     NOT NULL,
    code           VARCHAR2(20)   NOT NULL,
    name           VARCHAR2(120)  NOT NULL,
    generic_name   VARCHAR2(120)  NOT NULL,
    drug_class     VARCHAR2(100),
    dosage_form    VARCHAR2(30)   NOT NULL,
    strength       VARCHAR2(40),
    default_route  VARCHAR2(20),
    unit_price     NUMBER(10, 2)  DEFAULT 0 NOT NULL,
    stock_qty      NUMBER(10)     DEFAULT 0 NOT NULL,
    reorder_level  NUMBER(10)     DEFAULT 0 NOT NULL,
    active         NUMBER(1)      DEFAULT 1 NOT NULL,
    created_at     TIMESTAMP      DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by     VARCHAR2(50),
    updated_at     TIMESTAMP,
    updated_by     VARCHAR2(50),
    version        NUMBER(10)     DEFAULT 0 NOT NULL,
    CONSTRAINT pk_drugs PRIMARY KEY (id),
    CONSTRAINT uk_drugs_code UNIQUE (code),
    CONSTRAINT ck_drugs_stock CHECK (stock_qty >= 0),
    CONSTRAINT ck_drugs_active CHECK (active IN (0, 1))
);
CREATE INDEX ix_drugs_name ON drugs (name);

CREATE TABLE drug_interactions (
    id           NUMBER(19)    NOT NULL,
    drug_a_id    NUMBER(19)    NOT NULL,
    drug_b_id    NUMBER(19)    NOT NULL,
    severity     VARCHAR2(20)  NOT NULL,
    description  VARCHAR2(500) NOT NULL,
    CONSTRAINT pk_drug_interactions PRIMARY KEY (id),
    CONSTRAINT uk_drug_interactions UNIQUE (drug_a_id, drug_b_id),
    CONSTRAINT fk_drug_int_a FOREIGN KEY (drug_a_id) REFERENCES drugs (id),
    CONSTRAINT fk_drug_int_b FOREIGN KEY (drug_b_id) REFERENCES drugs (id),
    CONSTRAINT ck_drug_int_distinct CHECK (drug_a_id <> drug_b_id),
    CONSTRAINT ck_drug_int_severity CHECK (severity IN ('MINOR', 'MODERATE', 'MAJOR', 'CONTRAINDICATED'))
);
CREATE INDEX ix_drug_int_b ON drug_interactions (drug_b_id);

CREATE TABLE note_templates (
    id          NUMBER(19)     NOT NULL,
    code        VARCHAR2(30)   NOT NULL,
    name        VARCHAR2(120)  NOT NULL,
    specialty   VARCHAR2(100),
    subjective  VARCHAR2(2000),
    objective   VARCHAR2(2000),
    assessment  VARCHAR2(2000),
    plan_text   VARCHAR2(2000),
    active      NUMBER(1)      DEFAULT 1 NOT NULL,
    CONSTRAINT pk_note_templates PRIMARY KEY (id),
    CONSTRAINT uk_note_templates_code UNIQUE (code),
    CONSTRAINT ck_note_templates_active CHECK (active IN (0, 1))
);

--changeset careconnect:004-patient-tables
CREATE TABLE patients (
    id                    NUMBER(19)     NOT NULL,
    mrn                   VARCHAR2(20)   NOT NULL,
    first_name            VARCHAR2(60)   NOT NULL,
    last_name             VARCHAR2(60)   NOT NULL,
    date_of_birth         DATE           NOT NULL,
    gender                VARCHAR2(10)   NOT NULL,
    blood_group           VARCHAR2(5),
    phone                 VARCHAR2(20)   NOT NULL,
    email                 VARCHAR2(120),
    address_line          VARCHAR2(200),
    city                  VARCHAR2(60),
    state_name            VARCHAR2(60),
    postal_code           VARCHAR2(12),
    emergency_name        VARCHAR2(120),
    emergency_phone       VARCHAR2(20),
    emergency_relation    VARCHAR2(40),
    insurance_provider    VARCHAR2(100),
    insurance_policy_no   VARCHAR2(50),
    insurance_valid_till  DATE,
    medical_history       VARCHAR2(4000),
    primary_provider_id   NUMBER(19),
    deleted               NUMBER(1)      DEFAULT 0 NOT NULL,
    deleted_at            TIMESTAMP,
    deleted_by            VARCHAR2(50),
    created_at            TIMESTAMP      DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by            VARCHAR2(50),
    updated_at            TIMESTAMP,
    updated_by            VARCHAR2(50),
    version               NUMBER(10)     DEFAULT 0 NOT NULL,
    CONSTRAINT pk_patients PRIMARY KEY (id),
    CONSTRAINT uk_patients_mrn UNIQUE (mrn),
    CONSTRAINT fk_patients_provider FOREIGN KEY (primary_provider_id) REFERENCES app_users (id),
    CONSTRAINT ck_patients_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    CONSTRAINT ck_patients_deleted CHECK (deleted IN (0, 1))
);
CREATE INDEX ix_patients_name ON patients (last_name, first_name);
CREATE INDEX ix_patients_phone ON patients (phone);
CREATE INDEX ix_patients_dob ON patients (date_of_birth);
CREATE INDEX ix_patients_provider ON patients (primary_provider_id);

ALTER TABLE app_users ADD CONSTRAINT fk_app_users_patient FOREIGN KEY (patient_id) REFERENCES patients (id);

CREATE TABLE patient_allergies (
    id            NUMBER(19)    NOT NULL,
    patient_id    NUMBER(19)    NOT NULL,
    allergen      VARCHAR2(100) NOT NULL,
    allergy_type  VARCHAR2(20)  NOT NULL,
    reaction      VARCHAR2(200),
    severity      VARCHAR2(10)  NOT NULL,
    status        VARCHAR2(10)  DEFAULT 'ACTIVE' NOT NULL,
    noted_on      DATE,
    created_at    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by    VARCHAR2(50),
    updated_at    TIMESTAMP,
    updated_by    VARCHAR2(50),
    version       NUMBER(10)    DEFAULT 0 NOT NULL,
    CONSTRAINT pk_patient_allergies PRIMARY KEY (id),
    CONSTRAINT fk_allergies_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT ck_allergies_type CHECK (allergy_type IN ('DRUG', 'FOOD', 'ENVIRONMENT', 'OTHER')),
    CONSTRAINT ck_allergies_severity CHECK (severity IN ('MILD', 'MODERATE', 'SEVERE')),
    CONSTRAINT ck_allergies_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
CREATE INDEX ix_allergies_patient ON patient_allergies (patient_id);

CREATE TABLE patient_problems (
    id             NUMBER(19)    NOT NULL,
    patient_id     NUMBER(19)    NOT NULL,
    icd10_code     VARCHAR2(10),
    description    VARCHAR2(255) NOT NULL,
    status         VARCHAR2(10)  DEFAULT 'ACTIVE' NOT NULL,
    onset_date     DATE,
    resolved_date  DATE,
    created_at     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by     VARCHAR2(50),
    updated_at     TIMESTAMP,
    updated_by     VARCHAR2(50),
    version        NUMBER(10)    DEFAULT 0 NOT NULL,
    CONSTRAINT pk_patient_problems PRIMARY KEY (id),
    CONSTRAINT fk_problems_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_problems_icd10 FOREIGN KEY (icd10_code) REFERENCES icd10_codes (code),
    CONSTRAINT ck_problems_status CHECK (status IN ('ACTIVE', 'RESOLVED'))
);
CREATE INDEX ix_problems_patient ON patient_problems (patient_id);

CREATE TABLE immunizations (
    id               NUMBER(19)    NOT NULL,
    patient_id       NUMBER(19)    NOT NULL,
    vaccine_name     VARCHAR2(120) NOT NULL,
    dose_number      NUMBER(2),
    administered_on  DATE          NOT NULL,
    administered_by  VARCHAR2(120),
    lot_number       VARCHAR2(40),
    notes            VARCHAR2(500),
    created_at       TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by       VARCHAR2(50),
    updated_at       TIMESTAMP,
    updated_by       VARCHAR2(50),
    version          NUMBER(10)    DEFAULT 0 NOT NULL,
    CONSTRAINT pk_immunizations PRIMARY KEY (id),
    CONSTRAINT fk_immunizations_patient FOREIGN KEY (patient_id) REFERENCES patients (id)
);
CREATE INDEX ix_immunizations_patient ON immunizations (patient_id);

CREATE TABLE family_history (
    id              NUMBER(19)    NOT NULL,
    patient_id      NUMBER(19)    NOT NULL,
    relation        VARCHAR2(40)  NOT NULL,
    condition_name  VARCHAR2(200) NOT NULL,
    notes           VARCHAR2(500),
    created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by      VARCHAR2(50),
    updated_at      TIMESTAMP,
    updated_by      VARCHAR2(50),
    version         NUMBER(10)    DEFAULT 0 NOT NULL,
    CONSTRAINT pk_family_history PRIMARY KEY (id),
    CONSTRAINT fk_family_history_patient FOREIGN KEY (patient_id) REFERENCES patients (id)
);
CREATE INDEX ix_family_history_patient ON family_history (patient_id);

CREATE TABLE patient_documents (
    id            NUMBER(19)    NOT NULL,
    patient_id    NUMBER(19)    NOT NULL,
    category      VARCHAR2(20)  NOT NULL,
    file_name     VARCHAR2(255) NOT NULL,
    content_type  VARCHAR2(100) NOT NULL,
    size_bytes    NUMBER(19)    NOT NULL,
    storage_key   VARCHAR2(255) NOT NULL,
    description   VARCHAR2(500),
    deleted       NUMBER(1)     DEFAULT 0 NOT NULL,
    created_at    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by    VARCHAR2(50),
    updated_at    TIMESTAMP,
    updated_by    VARCHAR2(50),
    version       NUMBER(10)    DEFAULT 0 NOT NULL,
    CONSTRAINT pk_patient_documents PRIMARY KEY (id),
    CONSTRAINT uk_patient_documents_key UNIQUE (storage_key),
    CONSTRAINT fk_documents_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT ck_documents_category CHECK (category IN ('LAB_REPORT', 'IMAGING', 'REFERRAL', 'CONSENT', 'OTHER')),
    CONSTRAINT ck_documents_deleted CHECK (deleted IN (0, 1))
);
CREATE INDEX ix_documents_patient ON patient_documents (patient_id);

--changeset careconnect:005-scheduling-tables
CREATE TABLE provider_slots (
    id           NUMBER(19)   NOT NULL,
    provider_id  NUMBER(19)   NOT NULL,
    slot_start   TIMESTAMP    NOT NULL,
    slot_end     TIMESTAMP    NOT NULL,
    slot_status  VARCHAR2(10) DEFAULT 'AVAILABLE' NOT NULL,
    created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by   VARCHAR2(50),
    updated_at   TIMESTAMP,
    updated_by   VARCHAR2(50),
    version      NUMBER(10)   DEFAULT 0 NOT NULL,
    CONSTRAINT pk_provider_slots PRIMARY KEY (id),
    CONSTRAINT uk_provider_slots UNIQUE (provider_id, slot_start),
    CONSTRAINT fk_slots_provider FOREIGN KEY (provider_id) REFERENCES app_users (id),
    CONSTRAINT ck_slots_status CHECK (slot_status IN ('AVAILABLE', 'BOOKED', 'BLOCKED')),
    CONSTRAINT ck_slots_range CHECK (slot_end > slot_start)
);
CREATE INDEX ix_slots_start ON provider_slots (slot_start);

CREATE TABLE appointments (
    id                NUMBER(19)    NOT NULL,
    patient_id        NUMBER(19)    NOT NULL,
    provider_id       NUMBER(19)    NOT NULL,
    slot_id           NUMBER(19),
    start_time        TIMESTAMP     NOT NULL,
    end_time          TIMESTAMP     NOT NULL,
    appointment_type  VARCHAR2(15)  DEFAULT 'IN_PERSON' NOT NULL,
    reason            VARCHAR2(500),
    appt_status       VARCHAR2(12)  DEFAULT 'BOOKED' NOT NULL,
    booked_via        VARCHAR2(10)  DEFAULT 'STAFF' NOT NULL,
    cancel_reason     VARCHAR2(500),
    created_at        TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by        VARCHAR2(50),
    updated_at        TIMESTAMP,
    updated_by        VARCHAR2(50),
    version           NUMBER(10)    DEFAULT 0 NOT NULL,
    CONSTRAINT pk_appointments PRIMARY KEY (id),
    CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_appointments_provider FOREIGN KEY (provider_id) REFERENCES app_users (id),
    CONSTRAINT fk_appointments_slot FOREIGN KEY (slot_id) REFERENCES provider_slots (id),
    CONSTRAINT ck_appointments_type CHECK (appointment_type IN ('IN_PERSON', 'TELEHEALTH')),
    CONSTRAINT ck_appointments_status CHECK (appt_status IN ('BOOKED', 'CHECKED_IN', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    CONSTRAINT ck_appointments_via CHECK (booked_via IN ('STAFF', 'PORTAL'))
);
CREATE INDEX ix_appointments_patient ON appointments (patient_id);
CREATE INDEX ix_appointments_provider ON appointments (provider_id, start_time);
CREATE INDEX ix_appointments_slot ON appointments (slot_id);

--changeset careconnect:006-clinical-tables
CREATE TABLE encounters (
    id               NUMBER(19)    NOT NULL,
    patient_id       NUMBER(19)    NOT NULL,
    provider_id      NUMBER(19)    NOT NULL,
    department_id    NUMBER(19),
    appointment_id   NUMBER(19),
    encounter_type   VARCHAR2(15)  NOT NULL,
    status           VARCHAR2(10)  DEFAULT 'OPEN' NOT NULL,
    encounter_date   TIMESTAMP     NOT NULL,
    chief_complaint  VARCHAR2(500),
    closed_at        TIMESTAMP,
    created_at       TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by       VARCHAR2(50),
    updated_at       TIMESTAMP,
    updated_by       VARCHAR2(50),
    version          NUMBER(10)    DEFAULT 0 NOT NULL,
    CONSTRAINT pk_encounters PRIMARY KEY (id),
    CONSTRAINT fk_encounters_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_encounters_provider FOREIGN KEY (provider_id) REFERENCES app_users (id),
    CONSTRAINT fk_encounters_dept FOREIGN KEY (department_id) REFERENCES departments (id),
    CONSTRAINT fk_encounters_appt FOREIGN KEY (appointment_id) REFERENCES appointments (id),
    CONSTRAINT ck_encounters_type CHECK (encounter_type IN ('OUTPATIENT', 'INPATIENT', 'EMERGENCY', 'TELEHEALTH', 'FOLLOW_UP')),
    CONSTRAINT ck_encounters_status CHECK (status IN ('OPEN', 'CLOSED', 'CANCELLED'))
);
CREATE INDEX ix_encounters_patient ON encounters (patient_id, encounter_date);
CREATE INDEX ix_encounters_provider ON encounters (provider_id, status);

CREATE TABLE clinical_notes (
    id              NUMBER(19)     NOT NULL,
    encounter_id    NUMBER(19)     NOT NULL,
    patient_id      NUMBER(19)     NOT NULL,
    author_id       NUMBER(19)     NOT NULL,
    note_type       VARCHAR2(10)   DEFAULT 'PROGRESS' NOT NULL,
    parent_note_id  NUMBER(19),
    template_code   VARCHAR2(30),
    subjective      VARCHAR2(4000),
    objective       VARCHAR2(4000),
    assessment      VARCHAR2(4000),
    plan_text       VARCHAR2(4000),
    status          VARCHAR2(10)   DEFAULT 'DRAFT' NOT NULL,
    signed_at       TIMESTAMP,
    signed_by       NUMBER(19),
    created_at      TIMESTAMP      DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by      VARCHAR2(50),
    updated_at      TIMESTAMP,
    updated_by      VARCHAR2(50),
    version         NUMBER(10)     DEFAULT 0 NOT NULL,
    CONSTRAINT pk_clinical_notes PRIMARY KEY (id),
    CONSTRAINT fk_notes_encounter FOREIGN KEY (encounter_id) REFERENCES encounters (id),
    CONSTRAINT fk_notes_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_notes_author FOREIGN KEY (author_id) REFERENCES app_users (id),
    CONSTRAINT fk_notes_parent FOREIGN KEY (parent_note_id) REFERENCES clinical_notes (id),
    CONSTRAINT fk_notes_signed_by FOREIGN KEY (signed_by) REFERENCES app_users (id),
    CONSTRAINT ck_notes_type CHECK (note_type IN ('PROGRESS', 'ADDENDUM')),
    CONSTRAINT ck_notes_status CHECK (status IN ('DRAFT', 'SIGNED'))
);
CREATE INDEX ix_notes_encounter ON clinical_notes (encounter_id);
CREATE INDEX ix_notes_patient ON clinical_notes (patient_id);

CREATE TABLE encounter_diagnoses (
    id              NUMBER(19)    NOT NULL,
    encounter_id    NUMBER(19)    NOT NULL,
    icd10_code      VARCHAR2(10)  NOT NULL,
    diagnosis_type  VARCHAR2(10)  DEFAULT 'SECONDARY' NOT NULL,
    notes           VARCHAR2(500),
    created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by      VARCHAR2(50),
    updated_at      TIMESTAMP,
    updated_by      VARCHAR2(50),
    version         NUMBER(10)    DEFAULT 0 NOT NULL,
    CONSTRAINT pk_encounter_diagnoses PRIMARY KEY (id),
    CONSTRAINT uk_encounter_diagnoses UNIQUE (encounter_id, icd10_code),
    CONSTRAINT fk_diagnoses_encounter FOREIGN KEY (encounter_id) REFERENCES encounters (id),
    CONSTRAINT fk_diagnoses_icd10 FOREIGN KEY (icd10_code) REFERENCES icd10_codes (code),
    CONSTRAINT ck_diagnoses_type CHECK (diagnosis_type IN ('PRIMARY', 'SECONDARY'))
);

CREATE TABLE vitals (
    id            NUMBER(19)    NOT NULL,
    patient_id    NUMBER(19)    NOT NULL,
    encounter_id  NUMBER(19),
    recorded_by   NUMBER(19)    NOT NULL,
    recorded_at   TIMESTAMP     NOT NULL,
    systolic      NUMBER(3),
    diastolic     NUMBER(3),
    pulse         NUMBER(3),
    temperature   NUMBER(4, 1),
    resp_rate     NUMBER(3),
    spo2          NUMBER(3),
    weight_kg     NUMBER(5, 1),
    height_cm     NUMBER(5, 1),
    bmi           NUMBER(4, 1),
    notes         VARCHAR2(500),
    created_at    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by    VARCHAR2(50),
    updated_at    TIMESTAMP,
    updated_by    VARCHAR2(50),
    version       NUMBER(10)    DEFAULT 0 NOT NULL,
    CONSTRAINT pk_vitals PRIMARY KEY (id),
    CONSTRAINT fk_vitals_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_vitals_encounter FOREIGN KEY (encounter_id) REFERENCES encounters (id),
    CONSTRAINT fk_vitals_recorded_by FOREIGN KEY (recorded_by) REFERENCES app_users (id),
    CONSTRAINT ck_vitals_spo2 CHECK (spo2 IS NULL OR spo2 BETWEEN 0 AND 100)
);
CREATE INDEX ix_vitals_patient ON vitals (patient_id, recorded_at);

--changeset careconnect:007-order-tables
CREATE TABLE clinical_orders (
    id                     NUMBER(19)     NOT NULL,
    order_no               VARCHAR2(20)   NOT NULL,
    patient_id             NUMBER(19)     NOT NULL,
    encounter_id           NUMBER(19)     NOT NULL,
    ordered_by             NUMBER(19)     NOT NULL,
    order_type             VARCHAR2(12)   NOT NULL,
    status                 VARCHAR2(12)   DEFAULT 'DRAFT' NOT NULL,
    priority               VARCHAR2(10)   DEFAULT 'ROUTINE' NOT NULL,
    lab_test_id            NUMBER(19),
    imaging_study          VARCHAR2(200),
    body_site              VARCHAR2(100),
    referral_specialty     VARCHAR2(100),
    referral_to            VARCHAR2(120),
    clinical_indication    VARCHAR2(500),
    instructions           VARCHAR2(1000),
    alert_override_reason  VARCHAR2(500),
    placed_at              TIMESTAMP,
    started_at             TIMESTAMP,
    completed_at           TIMESTAMP,
    cancelled_at           TIMESTAMP,
    cancel_reason          VARCHAR2(500),
    created_at             TIMESTAMP      DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by             VARCHAR2(50),
    updated_at             TIMESTAMP,
    updated_by             VARCHAR2(50),
    version                NUMBER(10)     DEFAULT 0 NOT NULL,
    CONSTRAINT pk_clinical_orders PRIMARY KEY (id),
    CONSTRAINT uk_clinical_orders_no UNIQUE (order_no),
    CONSTRAINT fk_orders_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_orders_encounter FOREIGN KEY (encounter_id) REFERENCES encounters (id),
    CONSTRAINT fk_orders_ordered_by FOREIGN KEY (ordered_by) REFERENCES app_users (id),
    CONSTRAINT fk_orders_lab_test FOREIGN KEY (lab_test_id) REFERENCES lab_tests (id),
    CONSTRAINT ck_orders_type CHECK (order_type IN ('LAB', 'IMAGING', 'MEDICATION', 'REFERRAL')),
    CONSTRAINT ck_orders_status CHECK (status IN ('DRAFT', 'PLACED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_orders_priority CHECK (priority IN ('ROUTINE', 'URGENT', 'STAT'))
);
CREATE INDEX ix_orders_patient ON clinical_orders (patient_id, status);
CREATE INDEX ix_orders_encounter ON clinical_orders (encounter_id);
CREATE INDEX ix_orders_status_type ON clinical_orders (status, order_type);

CREATE TABLE order_alerts (
    id          NUMBER(19)    NOT NULL,
    order_id    NUMBER(19)    NOT NULL,
    alert_type  VARCHAR2(20)  NOT NULL,
    severity    VARCHAR2(10)  NOT NULL,
    message     VARCHAR2(500) NOT NULL,
    overridden  NUMBER(1)     DEFAULT 0 NOT NULL,
    created_at  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pk_order_alerts PRIMARY KEY (id),
    CONSTRAINT fk_order_alerts_order FOREIGN KEY (order_id) REFERENCES clinical_orders (id),
    CONSTRAINT ck_order_alerts_type CHECK (alert_type IN ('DRUG_ALLERGY', 'DRUG_INTERACTION', 'DUPLICATE_ORDER')),
    CONSTRAINT ck_order_alerts_severity CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL')),
    CONSTRAINT ck_order_alerts_overridden CHECK (overridden IN (0, 1))
);
CREATE INDEX ix_order_alerts_order ON order_alerts (order_id);

CREATE TABLE lab_results (
    id               NUMBER(19)     NOT NULL,
    order_id         NUMBER(19)     NOT NULL,
    lab_test_id      NUMBER(19),
    value_numeric    NUMBER(12, 3),
    value_text       VARCHAR2(4000),
    unit             VARCHAR2(30),
    reference_range  VARCHAR2(60),
    flag             VARCHAR2(15)   DEFAULT 'NORMAL' NOT NULL,
    result_status    VARCHAR2(12)   DEFAULT 'FINAL' NOT NULL,
    comments         VARCHAR2(1000),
    resulted_at      TIMESTAMP      NOT NULL,
    resulted_by      NUMBER(19)     NOT NULL,
    created_at       TIMESTAMP      DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by       VARCHAR2(50),
    updated_at       TIMESTAMP,
    updated_by       VARCHAR2(50),
    version          NUMBER(10)     DEFAULT 0 NOT NULL,
    CONSTRAINT pk_lab_results PRIMARY KEY (id),
    CONSTRAINT uk_lab_results_order UNIQUE (order_id),
    CONSTRAINT fk_lab_results_order FOREIGN KEY (order_id) REFERENCES clinical_orders (id),
    CONSTRAINT fk_lab_results_test FOREIGN KEY (lab_test_id) REFERENCES lab_tests (id),
    CONSTRAINT fk_lab_results_by FOREIGN KEY (resulted_by) REFERENCES app_users (id),
    CONSTRAINT ck_lab_results_flag CHECK (flag IN ('NORMAL', 'LOW', 'HIGH', 'CRITICAL_LOW', 'CRITICAL_HIGH', 'ABNORMAL')),
    CONSTRAINT ck_lab_results_status CHECK (result_status IN ('PRELIMINARY', 'FINAL', 'CORRECTED'))
);

--changeset careconnect:008-medication-tables
CREATE TABLE prescriptions (
    id                  NUMBER(19)    NOT NULL,
    rx_no               VARCHAR2(20)  NOT NULL,
    order_id            NUMBER(19)    NOT NULL,
    patient_id          NUMBER(19)    NOT NULL,
    encounter_id        NUMBER(19)    NOT NULL,
    prescriber_id       NUMBER(19)    NOT NULL,
    drug_id             NUMBER(19)    NOT NULL,
    dose                VARCHAR2(50)  NOT NULL,
    route               VARCHAR2(15)  NOT NULL,
    frequency           VARCHAR2(30)  NOT NULL,
    duration_days       NUMBER(4)     NOT NULL,
    quantity            NUMBER(6)     NOT NULL,
    instructions        VARCHAR2(500),
    status              VARCHAR2(22)  DEFAULT 'DRAFT' NOT NULL,
    verified_by         NUMBER(19),
    verified_at         TIMESTAMP,
    pharmacist_note     VARCHAR2(500),
    dispensed_by        NUMBER(19),
    dispensed_at        TIMESTAMP,
    dispensed_qty       NUMBER(6),
    discontinued_by     NUMBER(19),
    discontinued_at     TIMESTAMP,
    discontinue_reason  VARCHAR2(500),
    replaces_id         NUMBER(19),
    created_at          TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by          VARCHAR2(50),
    updated_at          TIMESTAMP,
    updated_by          VARCHAR2(50),
    version             NUMBER(10)    DEFAULT 0 NOT NULL,
    CONSTRAINT pk_prescriptions PRIMARY KEY (id),
    CONSTRAINT uk_prescriptions_rx_no UNIQUE (rx_no),
    CONSTRAINT uk_prescriptions_order UNIQUE (order_id),
    CONSTRAINT fk_rx_order FOREIGN KEY (order_id) REFERENCES clinical_orders (id),
    CONSTRAINT fk_rx_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_rx_encounter FOREIGN KEY (encounter_id) REFERENCES encounters (id),
    CONSTRAINT fk_rx_prescriber FOREIGN KEY (prescriber_id) REFERENCES app_users (id),
    CONSTRAINT fk_rx_drug FOREIGN KEY (drug_id) REFERENCES drugs (id),
    CONSTRAINT fk_rx_verified_by FOREIGN KEY (verified_by) REFERENCES app_users (id),
    CONSTRAINT fk_rx_dispensed_by FOREIGN KEY (dispensed_by) REFERENCES app_users (id),
    CONSTRAINT fk_rx_discontinued_by FOREIGN KEY (discontinued_by) REFERENCES app_users (id),
    CONSTRAINT fk_rx_replaces FOREIGN KEY (replaces_id) REFERENCES prescriptions (id),
    CONSTRAINT ck_rx_route CHECK (route IN ('ORAL', 'IV', 'IM', 'SC', 'TOPICAL', 'INHALED', 'SUBLINGUAL', 'RECTAL', 'OPHTHALMIC', 'OTHER')),
    CONSTRAINT ck_rx_status CHECK (status IN ('DRAFT', 'PENDING_VERIFICATION', 'VERIFIED', 'DISPENSED', 'REJECTED', 'DISCONTINUED')),
    CONSTRAINT ck_rx_quantity CHECK (quantity > 0 AND duration_days > 0)
);
CREATE INDEX ix_rx_patient ON prescriptions (patient_id, status);
CREATE INDEX ix_rx_status ON prescriptions (status);
CREATE INDEX ix_rx_drug ON prescriptions (drug_id);

CREATE TABLE medication_administrations (
    id               NUMBER(19)    NOT NULL,
    prescription_id  NUMBER(19)    NOT NULL,
    patient_id       NUMBER(19)    NOT NULL,
    administered_by  NUMBER(19)    NOT NULL,
    administered_at  TIMESTAMP     NOT NULL,
    dose_given       VARCHAR2(50),
    route            VARCHAR2(15),
    admin_status     VARCHAR2(10)  NOT NULL,
    notes            VARCHAR2(500),
    created_at       TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by       VARCHAR2(50),
    updated_at       TIMESTAMP,
    updated_by       VARCHAR2(50),
    version          NUMBER(10)    DEFAULT 0 NOT NULL,
    CONSTRAINT pk_med_admins PRIMARY KEY (id),
    CONSTRAINT fk_med_admins_rx FOREIGN KEY (prescription_id) REFERENCES prescriptions (id),
    CONSTRAINT fk_med_admins_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_med_admins_by FOREIGN KEY (administered_by) REFERENCES app_users (id),
    CONSTRAINT ck_med_admins_status CHECK (admin_status IN ('GIVEN', 'HELD', 'REFUSED', 'MISSED'))
);
CREATE INDEX ix_med_admins_rx ON medication_administrations (prescription_id);
CREATE INDEX ix_med_admins_patient ON medication_administrations (patient_id, administered_at);

CREATE TABLE inventory_transactions (
    id             NUMBER(19)    NOT NULL,
    drug_id        NUMBER(19)    NOT NULL,
    change_qty     NUMBER(10)    NOT NULL,
    balance_after  NUMBER(10)    NOT NULL,
    txn_type       VARCHAR2(12)  NOT NULL,
    reference      VARCHAR2(100),
    performed_by   NUMBER(19),
    created_at     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pk_inventory_txns PRIMARY KEY (id),
    CONSTRAINT fk_inventory_drug FOREIGN KEY (drug_id) REFERENCES drugs (id),
    CONSTRAINT fk_inventory_by FOREIGN KEY (performed_by) REFERENCES app_users (id),
    CONSTRAINT ck_inventory_type CHECK (txn_type IN ('RECEIPT', 'DISPENSE', 'ADJUSTMENT'))
);
CREATE INDEX ix_inventory_drug ON inventory_transactions (drug_id, created_at);

--changeset careconnect:009-communication-tables
CREATE TABLE messages (
    id            NUMBER(19)     NOT NULL,
    patient_id    NUMBER(19)     NOT NULL,
    sender_id     NUMBER(19)     NOT NULL,
    recipient_id  NUMBER(19)     NOT NULL,
    subject       VARCHAR2(200)  NOT NULL,
    body          VARCHAR2(4000) NOT NULL,
    read_at       TIMESTAMP,
    created_at    TIMESTAMP      DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pk_messages PRIMARY KEY (id),
    CONSTRAINT fk_messages_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES app_users (id),
    CONSTRAINT fk_messages_recipient FOREIGN KEY (recipient_id) REFERENCES app_users (id)
);
CREATE INDEX ix_messages_recipient ON messages (recipient_id, read_at);
CREATE INDEX ix_messages_patient ON messages (patient_id, created_at);

CREATE TABLE notifications (
    id          NUMBER(19)     NOT NULL,
    user_id     NUMBER(19)     NOT NULL,
    title       VARCHAR2(200)  NOT NULL,
    message     VARCHAR2(1000) NOT NULL,
    notif_type  VARCHAR2(30)   NOT NULL,
    link        VARCHAR2(255),
    is_read     NUMBER(1)      DEFAULT 0 NOT NULL,
    created_at  TIMESTAMP      DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES app_users (id),
    CONSTRAINT ck_notifications_read CHECK (is_read IN (0, 1))
);
CREATE INDEX ix_notifications_user ON notifications (user_id, is_read);

CREATE TABLE audit_logs (
    id             NUMBER(19)     NOT NULL,
    user_id        NUMBER(19),
    username       VARCHAR2(50),
    user_role      VARCHAR2(20),
    action         VARCHAR2(15)   NOT NULL,
    resource_type  VARCHAR2(50),
    resource_id    VARCHAR2(50),
    patient_id     NUMBER(19),
    http_method    VARCHAR2(10),
    request_path   VARCHAR2(255),
    status_code    NUMBER(3),
    ip_address     VARCHAR2(45),
    details        VARCHAR2(1000),
    created_at     TIMESTAMP      DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pk_audit_logs PRIMARY KEY (id),
    CONSTRAINT ck_audit_logs_action CHECK (action IN ('CREATE', 'READ', 'UPDATE', 'DELETE', 'LOGIN', 'LOGIN_FAILED', 'LOGOUT'))
);
CREATE INDEX ix_audit_logs_created ON audit_logs (created_at);
CREATE INDEX ix_audit_logs_patient ON audit_logs (patient_id);
CREATE INDEX ix_audit_logs_username ON audit_logs (username);
