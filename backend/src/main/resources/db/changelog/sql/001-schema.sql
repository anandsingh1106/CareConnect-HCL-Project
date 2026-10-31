--liquibase formatted sql

-- =====================================================================
-- CareConnect EHR - schema
-- PostgreSQL DDL for PostgreSQL 16+ (and H2 in
-- PostgreSQL mode for automated tests). Primary keys come from sequences,
-- booleans are SMALLINT with CHECK (0,1), and identifiers stay <= 30 chars.
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
    id           BIGINT     NOT NULL,
    code         VARCHAR(20)   NOT NULL,
    name         VARCHAR(100)  NOT NULL,
    description  VARCHAR(500),
    active       SMALLINT      DEFAULT 1 NOT NULL,
    created_at   TIMESTAMP      DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by   VARCHAR(50),
    updated_at   TIMESTAMP,
    updated_by   VARCHAR(50),
    version      INTEGER     DEFAULT 0 NOT NULL,
    CONSTRAINT pk_departments PRIMARY KEY (id),
    CONSTRAINT uk_departments_code UNIQUE (code),
    CONSTRAINT ck_departments_active CHECK (active IN (0, 1))
);

CREATE TABLE app_users (
    id             BIGINT    NOT NULL,
    username       VARCHAR(50)  NOT NULL,
    email          VARCHAR(120),
    password_hash  VARCHAR(100) NOT NULL,
    full_name      VARCHAR(120) NOT NULL,
    user_role      VARCHAR(20)  NOT NULL,
    department_id  BIGINT,
    phone          VARCHAR(20),
    specialty      VARCHAR(100),
    license_no     VARCHAR(50),
    patient_id     BIGINT,
    active         SMALLINT     DEFAULT 1 NOT NULL,
    last_login_at  TIMESTAMP,
    created_at     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by     VARCHAR(50),
    updated_at     TIMESTAMP,
    updated_by     VARCHAR(50),
    version        INTEGER    DEFAULT 0 NOT NULL,
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
    id          BIGINT   NOT NULL,
    user_id     BIGINT   NOT NULL,
    token_hash  VARCHAR(64) NOT NULL,
    expires_at  TIMESTAMP    NOT NULL,
    revoked     SMALLINT    DEFAULT 0 NOT NULL,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES app_users (id),
    CONSTRAINT ck_refresh_tokens_revoked CHECK (revoked IN (0, 1))
);
CREATE INDEX ix_refresh_tokens_user ON refresh_tokens (user_id);

--changeset careconnect:003-master-tables
CREATE TABLE icd10_codes (
    code         VARCHAR(10)  NOT NULL,
    description  VARCHAR(255) NOT NULL,
    category     VARCHAR(100),
    active       SMALLINT     DEFAULT 1 NOT NULL,
    CONSTRAINT pk_icd10_codes PRIMARY KEY (code),
    CONSTRAINT ck_icd10_active CHECK (active IN (0, 1))
);
CREATE INDEX ix_icd10_description ON icd10_codes (description);

CREATE TABLE lab_tests (
    id                BIGINT    NOT NULL,
    code              VARCHAR(20)  NOT NULL,
    name              VARCHAR(120) NOT NULL,
    category          VARCHAR(60),
    specimen          VARCHAR(60),
    unit              VARCHAR(30),
    normal_low        NUMERIC(12, 3),
    normal_high       NUMERIC(12, 3),
    critical_low      NUMERIC(12, 3),
    critical_high     NUMERIC(12, 3),
    turnaround_hours  INTEGER,
    active            SMALLINT     DEFAULT 1 NOT NULL,
    CONSTRAINT pk_lab_tests PRIMARY KEY (id),
    CONSTRAINT uk_lab_tests_code UNIQUE (code),
    CONSTRAINT ck_lab_tests_active CHECK (active IN (0, 1))
);

CREATE TABLE drugs (
    id             BIGINT     NOT NULL,
    code           VARCHAR(20)   NOT NULL,
    name           VARCHAR(120)  NOT NULL,
    generic_name   VARCHAR(120)  NOT NULL,
    drug_class     VARCHAR(100),
    dosage_form    VARCHAR(30)   NOT NULL,
    strength       VARCHAR(40),
    default_route  VARCHAR(20),
    unit_price     NUMERIC(10, 2)  DEFAULT 0 NOT NULL,
    stock_qty      INTEGER     DEFAULT 0 NOT NULL,
    reorder_level  INTEGER     DEFAULT 0 NOT NULL,
    active         SMALLINT      DEFAULT 1 NOT NULL,
    created_at     TIMESTAMP      DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by     VARCHAR(50),
    updated_at     TIMESTAMP,
    updated_by     VARCHAR(50),
    version        INTEGER     DEFAULT 0 NOT NULL,
    CONSTRAINT pk_drugs PRIMARY KEY (id),
    CONSTRAINT uk_drugs_code UNIQUE (code),
    CONSTRAINT ck_drugs_stock CHECK (stock_qty >= 0),
    CONSTRAINT ck_drugs_active CHECK (active IN (0, 1))
);
CREATE INDEX ix_drugs_name ON drugs (name);

CREATE TABLE drug_interactions (
    id           BIGINT    NOT NULL,
    drug_a_id    BIGINT    NOT NULL,
    drug_b_id    BIGINT    NOT NULL,
    severity     VARCHAR(20)  NOT NULL,
    description  VARCHAR(500) NOT NULL,
    CONSTRAINT pk_drug_interactions PRIMARY KEY (id),
    CONSTRAINT uk_drug_interactions UNIQUE (drug_a_id, drug_b_id),
    CONSTRAINT fk_drug_int_a FOREIGN KEY (drug_a_id) REFERENCES drugs (id),
    CONSTRAINT fk_drug_int_b FOREIGN KEY (drug_b_id) REFERENCES drugs (id),
    CONSTRAINT ck_drug_int_distinct CHECK (drug_a_id <> drug_b_id),
    CONSTRAINT ck_drug_int_severity CHECK (severity IN ('MINOR', 'MODERATE', 'MAJOR', 'CONTRAINDICATED'))
);
CREATE INDEX ix_drug_int_b ON drug_interactions (drug_b_id);

CREATE TABLE note_templates (
    id          BIGINT     NOT NULL,
    code        VARCHAR(30)   NOT NULL,
    name        VARCHAR(120)  NOT NULL,
    specialty   VARCHAR(100),
    subjective  VARCHAR(2000),
    objective   VARCHAR(2000),
    assessment  VARCHAR(2000),
    plan_text   VARCHAR(2000),
    active      SMALLINT      DEFAULT 1 NOT NULL,
    CONSTRAINT pk_note_templates PRIMARY KEY (id),
    CONSTRAINT uk_note_templates_code UNIQUE (code),
    CONSTRAINT ck_note_templates_active CHECK (active IN (0, 1))
);

--changeset careconnect:004-patient-tables
CREATE TABLE patients (
    id                    BIGINT     NOT NULL,
    mrn                   VARCHAR(20)   NOT NULL,
    first_name            VARCHAR(60)   NOT NULL,
    last_name             VARCHAR(60)   NOT NULL,
    date_of_birth         DATE           NOT NULL,
    gender                VARCHAR(10)   NOT NULL,
    blood_group           VARCHAR(5),
    phone                 VARCHAR(20)   NOT NULL,
    email                 VARCHAR(120),
    address_line          VARCHAR(200),
    city                  VARCHAR(60),
    state_name            VARCHAR(60),
    postal_code           VARCHAR(12),
    emergency_name        VARCHAR(120),
    emergency_phone       VARCHAR(20),
    emergency_relation    VARCHAR(40),
    insurance_provider    VARCHAR(100),
    insurance_policy_no   VARCHAR(50),
    insurance_valid_till  DATE,
    medical_history       VARCHAR(4000),
    primary_provider_id   BIGINT,
    deleted               SMALLINT      DEFAULT 0 NOT NULL,
    deleted_at            TIMESTAMP,
    deleted_by            VARCHAR(50),
    created_at            TIMESTAMP      DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by            VARCHAR(50),
    updated_at            TIMESTAMP,
    updated_by            VARCHAR(50),
    version               INTEGER     DEFAULT 0 NOT NULL,
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
    id            BIGINT    NOT NULL,
    patient_id    BIGINT    NOT NULL,
    allergen      VARCHAR(100) NOT NULL,
    allergy_type  VARCHAR(20)  NOT NULL,
    reaction      VARCHAR(200),
    severity      VARCHAR(10)  NOT NULL,
    status        VARCHAR(10)  DEFAULT 'ACTIVE' NOT NULL,
    noted_on      DATE,
    created_at    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by    VARCHAR(50),
    updated_at    TIMESTAMP,
    updated_by    VARCHAR(50),
    version       INTEGER    DEFAULT 0 NOT NULL,
    CONSTRAINT pk_patient_allergies PRIMARY KEY (id),
    CONSTRAINT fk_allergies_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT ck_allergies_type CHECK (allergy_type IN ('DRUG', 'FOOD', 'ENVIRONMENT', 'OTHER')),
    CONSTRAINT ck_allergies_severity CHECK (severity IN ('MILD', 'MODERATE', 'SEVERE')),
    CONSTRAINT ck_allergies_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
CREATE INDEX ix_allergies_patient ON patient_allergies (patient_id);

CREATE TABLE patient_problems (
    id             BIGINT    NOT NULL,
    patient_id     BIGINT    NOT NULL,
    icd10_code     VARCHAR(10),
    description    VARCHAR(255) NOT NULL,
    status         VARCHAR(10)  DEFAULT 'ACTIVE' NOT NULL,
    onset_date     DATE,
    resolved_date  DATE,
    created_at     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by     VARCHAR(50),
    updated_at     TIMESTAMP,
    updated_by     VARCHAR(50),
    version        INTEGER    DEFAULT 0 NOT NULL,
    CONSTRAINT pk_patient_problems PRIMARY KEY (id),
    CONSTRAINT fk_problems_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_problems_icd10 FOREIGN KEY (icd10_code) REFERENCES icd10_codes (code),
    CONSTRAINT ck_problems_status CHECK (status IN ('ACTIVE', 'RESOLVED'))
);
CREATE INDEX ix_problems_patient ON patient_problems (patient_id);

CREATE TABLE immunizations (
    id               BIGINT    NOT NULL,
    patient_id       BIGINT    NOT NULL,
    vaccine_name     VARCHAR(120) NOT NULL,
    dose_number      SMALLINT,
    administered_on  DATE          NOT NULL,
    administered_by  VARCHAR(120),
    lot_number       VARCHAR(40),
    notes            VARCHAR(500),
    created_at       TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by       VARCHAR(50),
    updated_at       TIMESTAMP,
    updated_by       VARCHAR(50),
    version          INTEGER    DEFAULT 0 NOT NULL,
    CONSTRAINT pk_immunizations PRIMARY KEY (id),
    CONSTRAINT fk_immunizations_patient FOREIGN KEY (patient_id) REFERENCES patients (id)
);
CREATE INDEX ix_immunizations_patient ON immunizations (patient_id);

CREATE TABLE family_history (
    id              BIGINT    NOT NULL,
    patient_id      BIGINT    NOT NULL,
    relation        VARCHAR(40)  NOT NULL,
    condition_name  VARCHAR(200) NOT NULL,
    notes           VARCHAR(500),
    created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by      VARCHAR(50),
    updated_at      TIMESTAMP,
    updated_by      VARCHAR(50),
    version         INTEGER    DEFAULT 0 NOT NULL,
    CONSTRAINT pk_family_history PRIMARY KEY (id),
    CONSTRAINT fk_family_history_patient FOREIGN KEY (patient_id) REFERENCES patients (id)
);
CREATE INDEX ix_family_history_patient ON family_history (patient_id);

CREATE TABLE patient_documents (
    id            BIGINT    NOT NULL,
    patient_id    BIGINT    NOT NULL,
    category      VARCHAR(20)  NOT NULL,
    file_name     VARCHAR(255) NOT NULL,
    content_type  VARCHAR(100) NOT NULL,
    size_bytes    BIGINT    NOT NULL,
    storage_key   VARCHAR(255) NOT NULL,
    description   VARCHAR(500),
    deleted       SMALLINT     DEFAULT 0 NOT NULL,
    created_at    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by    VARCHAR(50),
    updated_at    TIMESTAMP,
    updated_by    VARCHAR(50),
    version       INTEGER    DEFAULT 0 NOT NULL,
    CONSTRAINT pk_patient_documents PRIMARY KEY (id),
    CONSTRAINT uk_patient_documents_key UNIQUE (storage_key),
    CONSTRAINT fk_documents_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT ck_documents_category CHECK (category IN ('LAB_REPORT', 'IMAGING', 'REFERRAL', 'CONSENT', 'OTHER')),
    CONSTRAINT ck_documents_deleted CHECK (deleted IN (0, 1))
);
CREATE INDEX ix_documents_patient ON patient_documents (patient_id);

--changeset careconnect:005-scheduling-tables
CREATE TABLE provider_slots (
    id           BIGINT   NOT NULL,
    provider_id  BIGINT   NOT NULL,
    slot_start   TIMESTAMP    NOT NULL,
    slot_end     TIMESTAMP    NOT NULL,
    slot_status  VARCHAR(10) DEFAULT 'AVAILABLE' NOT NULL,
    created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by   VARCHAR(50),
    updated_at   TIMESTAMP,
    updated_by   VARCHAR(50),
    version      INTEGER   DEFAULT 0 NOT NULL,
    CONSTRAINT pk_provider_slots PRIMARY KEY (id),
    CONSTRAINT uk_provider_slots UNIQUE (provider_id, slot_start),
    CONSTRAINT fk_slots_provider FOREIGN KEY (provider_id) REFERENCES app_users (id),
    CONSTRAINT ck_slots_status CHECK (slot_status IN ('AVAILABLE', 'BOOKED', 'BLOCKED')),
    CONSTRAINT ck_slots_range CHECK (slot_end > slot_start)
);
CREATE INDEX ix_slots_start ON provider_slots (slot_start);

CREATE TABLE appointments (
    id                BIGINT    NOT NULL,
    patient_id        BIGINT    NOT NULL,
    provider_id       BIGINT    NOT NULL,
    slot_id           BIGINT,
    start_time        TIMESTAMP     NOT NULL,
    end_time          TIMESTAMP     NOT NULL,
    appointment_type  VARCHAR(15)  DEFAULT 'IN_PERSON' NOT NULL,
    reason            VARCHAR(500),
    appt_status       VARCHAR(12)  DEFAULT 'BOOKED' NOT NULL,
    booked_via        VARCHAR(10)  DEFAULT 'STAFF' NOT NULL,
    cancel_reason     VARCHAR(500),
    created_at        TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by        VARCHAR(50),
    updated_at        TIMESTAMP,
    updated_by        VARCHAR(50),
    version           INTEGER    DEFAULT 0 NOT NULL,
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
    id               BIGINT    NOT NULL,
    patient_id       BIGINT    NOT NULL,
    provider_id      BIGINT    NOT NULL,
    department_id    BIGINT,
    appointment_id   BIGINT,
    encounter_type   VARCHAR(15)  NOT NULL,
    status           VARCHAR(10)  DEFAULT 'OPEN' NOT NULL,
    encounter_date   TIMESTAMP     NOT NULL,
    chief_complaint  VARCHAR(500),
    closed_at        TIMESTAMP,
    created_at       TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by       VARCHAR(50),
    updated_at       TIMESTAMP,
    updated_by       VARCHAR(50),
    version          INTEGER    DEFAULT 0 NOT NULL,
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
    id              BIGINT     NOT NULL,
    encounter_id    BIGINT     NOT NULL,
    patient_id      BIGINT     NOT NULL,
    author_id       BIGINT     NOT NULL,
    note_type       VARCHAR(10)   DEFAULT 'PROGRESS' NOT NULL,
    parent_note_id  BIGINT,
    template_code   VARCHAR(30),
    subjective      VARCHAR(4000),
    objective       VARCHAR(4000),
    assessment      VARCHAR(4000),
    plan_text       VARCHAR(4000),
    status          VARCHAR(10)   DEFAULT 'DRAFT' NOT NULL,
    signed_at       TIMESTAMP,
    signed_by       BIGINT,
    created_at      TIMESTAMP      DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by      VARCHAR(50),
    updated_at      TIMESTAMP,
    updated_by      VARCHAR(50),
    version         INTEGER     DEFAULT 0 NOT NULL,
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
    id              BIGINT    NOT NULL,
    encounter_id    BIGINT    NOT NULL,
    icd10_code      VARCHAR(10)  NOT NULL,
    diagnosis_type  VARCHAR(10)  DEFAULT 'SECONDARY' NOT NULL,
    notes           VARCHAR(500),
    created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by      VARCHAR(50),
    updated_at      TIMESTAMP,
    updated_by      VARCHAR(50),
    version         INTEGER    DEFAULT 0 NOT NULL,
    CONSTRAINT pk_encounter_diagnoses PRIMARY KEY (id),
    CONSTRAINT uk_encounter_diagnoses UNIQUE (encounter_id, icd10_code),
    CONSTRAINT fk_diagnoses_encounter FOREIGN KEY (encounter_id) REFERENCES encounters (id),
    CONSTRAINT fk_diagnoses_icd10 FOREIGN KEY (icd10_code) REFERENCES icd10_codes (code),
    CONSTRAINT ck_diagnoses_type CHECK (diagnosis_type IN ('PRIMARY', 'SECONDARY'))
);

CREATE TABLE vitals (
    id            BIGINT    NOT NULL,
    patient_id    BIGINT    NOT NULL,
    encounter_id  BIGINT,
    recorded_by   BIGINT    NOT NULL,
    recorded_at   TIMESTAMP     NOT NULL,
    systolic      SMALLINT,
    diastolic     SMALLINT,
    pulse         SMALLINT,
    temperature   NUMERIC(4, 1),
    resp_rate     SMALLINT,
    spo2          SMALLINT,
    weight_kg     NUMERIC(5, 1),
    height_cm     NUMERIC(5, 1),
    bmi           NUMERIC(4, 1),
    notes         VARCHAR(500),
    created_at    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by    VARCHAR(50),
    updated_at    TIMESTAMP,
    updated_by    VARCHAR(50),
    version       INTEGER    DEFAULT 0 NOT NULL,
    CONSTRAINT pk_vitals PRIMARY KEY (id),
    CONSTRAINT fk_vitals_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_vitals_encounter FOREIGN KEY (encounter_id) REFERENCES encounters (id),
    CONSTRAINT fk_vitals_recorded_by FOREIGN KEY (recorded_by) REFERENCES app_users (id),
    CONSTRAINT ck_vitals_spo2 CHECK (spo2 IS NULL OR spo2 BETWEEN 0 AND 100)
);
CREATE INDEX ix_vitals_patient ON vitals (patient_id, recorded_at);

--changeset careconnect:007-order-tables
CREATE TABLE clinical_orders (
    id                     BIGINT     NOT NULL,
    order_no               VARCHAR(20)   NOT NULL,
    patient_id             BIGINT     NOT NULL,
    encounter_id           BIGINT     NOT NULL,
    ordered_by             BIGINT     NOT NULL,
    order_type             VARCHAR(12)   NOT NULL,
    status                 VARCHAR(12)   DEFAULT 'DRAFT' NOT NULL,
    priority               VARCHAR(10)   DEFAULT 'ROUTINE' NOT NULL,
    lab_test_id            BIGINT,
    imaging_study          VARCHAR(200),
    body_site              VARCHAR(100),
    referral_specialty     VARCHAR(100),
    referral_to            VARCHAR(120),
    clinical_indication    VARCHAR(500),
    instructions           VARCHAR(1000),
    alert_override_reason  VARCHAR(500),
    placed_at              TIMESTAMP,
    started_at             TIMESTAMP,
    completed_at           TIMESTAMP,
    cancelled_at           TIMESTAMP,
    cancel_reason          VARCHAR(500),
    created_at             TIMESTAMP      DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by             VARCHAR(50),
    updated_at             TIMESTAMP,
    updated_by             VARCHAR(50),
    version                INTEGER     DEFAULT 0 NOT NULL,
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
    id          BIGINT    NOT NULL,
    order_id    BIGINT    NOT NULL,
    alert_type  VARCHAR(20)  NOT NULL,
    severity    VARCHAR(10)  NOT NULL,
    message     VARCHAR(500) NOT NULL,
    overridden  SMALLINT     DEFAULT 0 NOT NULL,
    created_at  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pk_order_alerts PRIMARY KEY (id),
    CONSTRAINT fk_order_alerts_order FOREIGN KEY (order_id) REFERENCES clinical_orders (id),
    CONSTRAINT ck_order_alerts_type CHECK (alert_type IN ('DRUG_ALLERGY', 'DRUG_INTERACTION', 'DUPLICATE_ORDER')),
    CONSTRAINT ck_order_alerts_severity CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL')),
    CONSTRAINT ck_order_alerts_overridden CHECK (overridden IN (0, 1))
);
CREATE INDEX ix_order_alerts_order ON order_alerts (order_id);

CREATE TABLE lab_results (
    id               BIGINT     NOT NULL,
    order_id         BIGINT     NOT NULL,
    lab_test_id      BIGINT,
    value_numeric    NUMERIC(12, 3),
    value_text       VARCHAR(4000),
    unit             VARCHAR(30),
    reference_range  VARCHAR(60),
    flag             VARCHAR(15)   DEFAULT 'NORMAL' NOT NULL,
    result_status    VARCHAR(12)   DEFAULT 'FINAL' NOT NULL,
    comments         VARCHAR(1000),
    resulted_at      TIMESTAMP      NOT NULL,
    resulted_by      BIGINT     NOT NULL,
    created_at       TIMESTAMP      DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by       VARCHAR(50),
    updated_at       TIMESTAMP,
    updated_by       VARCHAR(50),
    version          INTEGER     DEFAULT 0 NOT NULL,
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
    id                  BIGINT    NOT NULL,
    rx_no               VARCHAR(20)  NOT NULL,
    order_id            BIGINT    NOT NULL,
    patient_id          BIGINT    NOT NULL,
    encounter_id        BIGINT    NOT NULL,
    prescriber_id       BIGINT    NOT NULL,
    drug_id             BIGINT    NOT NULL,
    dose                VARCHAR(50)  NOT NULL,
    route               VARCHAR(15)  NOT NULL,
    frequency           VARCHAR(30)  NOT NULL,
    duration_days       INTEGER     NOT NULL,
    quantity            INTEGER     NOT NULL,
    instructions        VARCHAR(500),
    status              VARCHAR(22)  DEFAULT 'DRAFT' NOT NULL,
    verified_by         BIGINT,
    verified_at         TIMESTAMP,
    pharmacist_note     VARCHAR(500),
    dispensed_by        BIGINT,
    dispensed_at        TIMESTAMP,
    dispensed_qty       INTEGER,
    discontinued_by     BIGINT,
    discontinued_at     TIMESTAMP,
    discontinue_reason  VARCHAR(500),
    replaces_id         BIGINT,
    created_at          TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by          VARCHAR(50),
    updated_at          TIMESTAMP,
    updated_by          VARCHAR(50),
    version             INTEGER    DEFAULT 0 NOT NULL,
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
    id               BIGINT    NOT NULL,
    prescription_id  BIGINT    NOT NULL,
    patient_id       BIGINT    NOT NULL,
    administered_by  BIGINT    NOT NULL,
    administered_at  TIMESTAMP     NOT NULL,
    dose_given       VARCHAR(50),
    route            VARCHAR(15),
    admin_status     VARCHAR(10)  NOT NULL,
    notes            VARCHAR(500),
    created_at       TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by       VARCHAR(50),
    updated_at       TIMESTAMP,
    updated_by       VARCHAR(50),
    version          INTEGER    DEFAULT 0 NOT NULL,
    CONSTRAINT pk_med_admins PRIMARY KEY (id),
    CONSTRAINT fk_med_admins_rx FOREIGN KEY (prescription_id) REFERENCES prescriptions (id),
    CONSTRAINT fk_med_admins_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_med_admins_by FOREIGN KEY (administered_by) REFERENCES app_users (id),
    CONSTRAINT ck_med_admins_status CHECK (admin_status IN ('GIVEN', 'HELD', 'REFUSED', 'MISSED'))
);
CREATE INDEX ix_med_admins_rx ON medication_administrations (prescription_id);
CREATE INDEX ix_med_admins_patient ON medication_administrations (patient_id, administered_at);

CREATE TABLE inventory_transactions (
    id             BIGINT    NOT NULL,
    drug_id        BIGINT    NOT NULL,
    change_qty     INTEGER    NOT NULL,
    balance_after  INTEGER    NOT NULL,
    txn_type       VARCHAR(12)  NOT NULL,
    reference      VARCHAR(100),
    performed_by   BIGINT,
    created_at     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pk_inventory_txns PRIMARY KEY (id),
    CONSTRAINT fk_inventory_drug FOREIGN KEY (drug_id) REFERENCES drugs (id),
    CONSTRAINT fk_inventory_by FOREIGN KEY (performed_by) REFERENCES app_users (id),
    CONSTRAINT ck_inventory_type CHECK (txn_type IN ('RECEIPT', 'DISPENSE', 'ADJUSTMENT'))
);
CREATE INDEX ix_inventory_drug ON inventory_transactions (drug_id, created_at);

--changeset careconnect:009-communication-tables
CREATE TABLE messages (
    id            BIGINT     NOT NULL,
    patient_id    BIGINT     NOT NULL,
    sender_id     BIGINT     NOT NULL,
    recipient_id  BIGINT     NOT NULL,
    subject       VARCHAR(200)  NOT NULL,
    body          VARCHAR(4000) NOT NULL,
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
    id          BIGINT     NOT NULL,
    user_id     BIGINT     NOT NULL,
    title       VARCHAR(200)  NOT NULL,
    message     VARCHAR(1000) NOT NULL,
    notif_type  VARCHAR(30)   NOT NULL,
    link        VARCHAR(255),
    is_read     SMALLINT      DEFAULT 0 NOT NULL,
    created_at  TIMESTAMP      DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES app_users (id),
    CONSTRAINT ck_notifications_read CHECK (is_read IN (0, 1))
);
CREATE INDEX ix_notifications_user ON notifications (user_id, is_read);

CREATE TABLE audit_logs (
    id             BIGINT     NOT NULL,
    user_id        BIGINT,
    username       VARCHAR(50),
    user_role      VARCHAR(20),
    action         VARCHAR(15)   NOT NULL,
    resource_type  VARCHAR(50),
    resource_id    VARCHAR(50),
    patient_id     BIGINT,
    http_method    VARCHAR(10),
    request_path   VARCHAR(255),
    status_code    SMALLINT,
    ip_address     VARCHAR(45),
    details        VARCHAR(1000),
    created_at     TIMESTAMP      DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pk_audit_logs PRIMARY KEY (id),
    CONSTRAINT ck_audit_logs_action CHECK (action IN ('CREATE', 'READ', 'UPDATE', 'DELETE', 'LOGIN', 'LOGIN_FAILED', 'LOGOUT'))
);
CREATE INDEX ix_audit_logs_created ON audit_logs (created_at);
CREATE INDEX ix_audit_logs_patient ON audit_logs (patient_id);
CREATE INDEX ix_audit_logs_username ON audit_logs (username);
