# CareConnect - Architecture and Design

This document holds the design diagrams for the project report. All diagrams use [Mermaid](https://mermaid.js.org/);
GitHub and VS Code (with a Mermaid extension) render them directly.

## 1. System architecture

```mermaid
flowchart LR
    subgraph Client["Browser"]
        UI["Angular 21 SPA<br/>Angular Material + Chart.js<br/>(standalone components, signals)"]
    end

    subgraph API["Spring Boot 3.5 backend :8090"]
        direction TB
        SEC["Security filter chain<br/>JWT filter + CORS + @PreAuthorize"]
        CTRL["REST controllers<br/>(DTOs, Bean Validation)"]
        SVC["Services<br/>business rules, CDS engine,<br/>pharmacy workflow, PDF"]
        AUD["Audit interceptor<br/>(every patient-data request)"]
        REPO["Spring Data JPA repositories<br/>(Hibernate, PostgreSQLDialect)"]
        LQB["Liquibase migrations<br/>(schema + seed data)"]
        SEC --> CTRL --> SVC --> REPO
        CTRL -.-> AUD
    end

    DB[("PostgreSQL<br/>schema CARECONNECT<br/>sequences, constraints, indexes")]
    FS[("File storage<br/>./uploads")]

    UI -- "HTTPS / JSON<br/>Authorization: Bearer JWT" --> SEC
    REPO --> DB
    LQB --> DB
    AUD --> DB
    SVC --> FS
```

**Layers (backend, package `com.careconnect.<module>`)**

| Layer | Package | Responsibility |
|---|---|---|
| Web | `web` | REST controllers, request validation, `@PreAuthorize` role checks, Swagger annotations |
| DTO / mapper | `dto`, `mapper` | Request/response records; MapStruct maps entities to DTOs so entities never leave the service layer |
| Service | `service` | Transactions and business rules (MRN generation, CDS checks, order lifecycle, stock deduction) |
| Repository | `repository` | Spring Data JPA queries (paging, sorting, soft-delete filters) |
| Model | `model` | JPA entities with `@Version` optimistic locking and created/updated audit columns |
| Cross-cutting | `common`, `security`, `config`, `audit` | Global exception handler, JWT, CORS, audit trail, OpenAPI |

Modules: `auth`, `user`, `patient`, `clinical`, `order`, `medication`, `scheduling`, `messaging`, `notification`, `portal`, `dashboard`, `audit`.

**Frontend structure (`frontend/src/app`)**

| Folder | Contents |
|---|---|
| `core/` | `AuthService` (tokens, user signal), `authInterceptor` (Bearer header + automatic refresh on 401), `authGuard` / `guestGuard`, typed `Api` client, models |
| `layout/` | Shell with role-based side menu, notification bell (polls every 30 s), user menu, change-password dialog |
| `shared/` | Confirm dialog, status chips, Chart.js wrapper, patient picker, ICD-10 picker, patient banner, vitals dialog |
| `pages/` | Landing, login, dashboard and one folder per module (patients, clinical, orders, pharmacy, nursing, scheduling, messages, admin, portal) |

## 2. Security design

- **Authentication**: `POST /api/auth/login` checks the BCrypt hash and returns a 15-minute **access token** (JWT, HS256, claims: user id, role, patient id) and a 7-day **refresh token**.
- **Refresh token rotation**: refresh tokens are random values stored only as SHA-256 hashes. Each refresh revokes the old token and issues a new pair. Re-using a revoked token revokes *all* of that user's tokens (theft detection).
- **Authorization**: every controller method has `@PreAuthorize` with a rule from `Authz` (minimum-necessary access). The Angular `authGuard` hides screens the role cannot use, but the server is the source of truth.
- **Patient isolation**: portal endpoints take the patient id from the token, never from the request, so a patient cannot read another record (404).
- **Audit**: `AuditInterceptor` writes one row per patient-data request (user, role, action, patient id, path, status, IP) and every login / failed login.
- **Other**: stateless sessions, CSRF disabled (no cookies), CORS limited to configured origins, secrets from environment variables, soft deletes, optimistic locking (`version`).

## 3. Entity-relationship diagram

```mermaid
erDiagram
    DEPARTMENTS ||--o{ APP_USERS : "employs"
    APP_USERS ||--o{ REFRESH_TOKENS : "has"
    PATIENTS |o--o| APP_USERS : "portal account"
    APP_USERS ||--o{ PATIENTS : "primary doctor of"

    PATIENTS ||--o{ PATIENT_ALLERGIES : ""
    PATIENTS ||--o{ PATIENT_PROBLEMS : ""
    ICD10_CODES |o--o{ PATIENT_PROBLEMS : "coded as"
    PATIENTS ||--o{ IMMUNIZATIONS : ""
    PATIENTS ||--o{ FAMILY_HISTORY : ""
    PATIENTS ||--o{ PATIENT_DOCUMENTS : ""

    APP_USERS ||--o{ PROVIDER_SLOTS : "doctor"
    PROVIDER_SLOTS ||--o| APPOINTMENTS : "booked by"
    PATIENTS ||--o{ APPOINTMENTS : ""

    PATIENTS ||--o{ ENCOUNTERS : ""
    APP_USERS ||--o{ ENCOUNTERS : "provider"
    APPOINTMENTS |o--o| ENCOUNTERS : "starts"
    ENCOUNTERS ||--o{ CLINICAL_NOTES : ""
    CLINICAL_NOTES |o--o{ CLINICAL_NOTES : "addendum of"
    ENCOUNTERS ||--o{ ENCOUNTER_DIAGNOSES : ""
    ICD10_CODES ||--o{ ENCOUNTER_DIAGNOSES : ""
    PATIENTS ||--o{ VITALS : ""
    ENCOUNTERS |o--o{ VITALS : ""

    ENCOUNTERS ||--o{ CLINICAL_ORDERS : ""
    PATIENTS ||--o{ CLINICAL_ORDERS : ""
    LAB_TESTS |o--o{ CLINICAL_ORDERS : "test"
    CLINICAL_ORDERS ||--o{ ORDER_ALERTS : "CDS alerts"
    CLINICAL_ORDERS ||--o| LAB_RESULTS : "result"
    CLINICAL_ORDERS ||--o| PRESCRIPTIONS : "e-Rx"

    DRUGS ||--o{ PRESCRIPTIONS : ""
    DRUGS ||--o{ DRUG_INTERACTIONS : "drug A / B"
    PRESCRIPTIONS ||--o{ MEDICATION_ADMINISTRATIONS : "MAR"
    DRUGS ||--o{ INVENTORY_TRANSACTIONS : "stock ledger"

    APP_USERS ||--o{ MESSAGES : "sender / recipient"
    PATIENTS ||--o{ MESSAGES : "about"
    APP_USERS ||--o{ NOTIFICATIONS : ""
    APP_USERS |o--o{ AUDIT_LOGS : ""

    PATIENTS {
        NUMBER id PK
        VARCHAR mrn UK "CC-YYYY-NNNNNN"
        VARCHAR first_name
        VARCHAR last_name
        DATE date_of_birth
        VARCHAR gender "CHECK"
        VARCHAR blood_group "CHECK"
        VARCHAR phone
        NUMBER primary_provider_id FK
        NUMBER deleted "soft delete 0/1"
        NUMBER version "optimistic lock"
    }
    APP_USERS {
        NUMBER id PK
        VARCHAR username UK
        VARCHAR password_hash "BCrypt"
        VARCHAR role "CHECK: 6 roles"
        NUMBER department_id FK
        NUMBER patient_id FK "UK, PATIENT role"
        NUMBER active
    }
    ENCOUNTERS {
        NUMBER id PK
        NUMBER patient_id FK
        NUMBER provider_id FK
        VARCHAR encounter_type
        VARCHAR status "OPEN/CLOSED/CANCELLED"
        TIMESTAMP encounter_date
    }
    CLINICAL_NOTES {
        NUMBER id PK
        NUMBER encounter_id FK
        NUMBER parent_note_id FK "addendum"
        TEXT subjective
        TEXT objective
        TEXT assessment
        TEXT plan
        VARCHAR status "DRAFT/SIGNED"
        TIMESTAMP signed_at
    }
    CLINICAL_ORDERS {
        NUMBER id PK
        VARCHAR order_no UK "ORD-NNNNNN"
        VARCHAR order_type "LAB/IMAGING/MEDICATION/REFERRAL"
        VARCHAR status "DRAFT..COMPLETED"
        VARCHAR priority
        VARCHAR alert_override_reason
    }
    PRESCRIPTIONS {
        NUMBER id PK
        VARCHAR rx_no UK "RX-NNNNNN"
        NUMBER order_id FK
        NUMBER drug_id FK
        VARCHAR dose
        VARCHAR frequency
        NUMBER quantity
        VARCHAR status "PENDING_VERIFICATION..DISPENSED"
    }
    DRUGS {
        NUMBER id PK
        VARCHAR code UK
        VARCHAR generic_name
        VARCHAR drug_class
        NUMBER stock_qty "CHECK >= 0"
        NUMBER reorder_level
    }
    LAB_RESULTS {
        NUMBER id PK
        NUMBER order_id FK "UK"
        NUMBER value_numeric
        VARCHAR flag "NORMAL/LOW/HIGH/CRITICAL_*"
    }
```

Every table has its own sequence (`<table>_seq`); business numbers come from `mrn_seq`, `order_no_seq` and `rx_no_seq`.
All tables except code tables carry `created_at/created_by/updated_at/updated_by` and `version`. The full DDL is in
[`backend/src/main/resources/db/changelog/sql/001-schema.sql`](../backend/src/main/resources/db/changelog/sql/001-schema.sql).

## 4. Use-case diagram

```mermaid
flowchart LR
    Doctor(["👨‍⚕️ Doctor"])
    Nurse(["👩‍⚕️ Nurse"])
    Pharmacist(["💊 Pharmacist"])
    Receptionist(["🧾 Receptionist"])
    Admin(["🛡️ Admin"])
    Patient(["🧑 Patient"])

    subgraph CareConnect
        UC1([Register / search patient])
        UC2([Book & check in appointment])
        UC3([Record vitals])
        UC4([Write, sign SOAP note / addendum])
        UC5([Add ICD-10 diagnosis])
        UC6([Place order with CDS check])
        UC7([Enter lab result])
        UC8([Verify / reject e-prescription])
        UC9([Dispense & manage stock])
        UC10([Document dose on MAR])
        UC11([Manage users & master data])
        UC12([Review audit log])
        UC13([View records & download PDFs])
        UC14([Book / cancel own appointment])
        UC15([Secure messaging])
    end

    Receptionist --- UC1 & UC2
    Doctor --- UC1 & UC2 & UC4 & UC5 & UC6 & UC7 & UC15
    Nurse --- UC3 & UC7 & UC10 & UC2
    Doctor --- UC3
    Pharmacist --- UC8 & UC9
    Admin --- UC11 & UC12
    Patient --- UC13 & UC14 & UC15
```

## 5. Sequence diagrams

### 5.1 Login and token refresh

```mermaid
sequenceDiagram
    actor U as User
    participant A as Angular (AuthService + interceptor)
    participant S as Spring Security
    participant Auth as AuthService (backend)
    participant DB as PostgreSQL

    U->>A: username + password
    A->>Auth: POST /api/auth/login
    Auth->>DB: SELECT app_users WHERE username = ?
    Auth->>Auth: BCrypt.matches(password, hash)
    alt valid and active
        Auth->>DB: INSERT refresh_tokens (SHA-256 hash, expires +7d)
        Auth->>DB: INSERT audit_logs (LOGIN)
        Auth-->>A: 200 {accessToken (15 min JWT), refreshToken, user}
        A->>A: store tokens, set user signal, go to /dashboard
    else wrong credentials
        Auth->>DB: INSERT audit_logs (LOGIN_FAILED)
        Auth-->>A: 401 INVALID_CREDENTIALS
    end

    Note over A,S: later - any API call
    A->>S: GET /api/... Authorization: Bearer accessToken
    S-->>A: 401 TOKEN_EXPIRED
    A->>Auth: POST /api/auth/refresh {refreshToken}
    Auth->>DB: find token by hash, revoke it, insert new one
    Auth-->>A: 200 new token pair
    A->>S: retry original request with new token
    S-->>A: 200 data
```

### 5.2 Medication order with drug-interaction check

```mermaid
sequenceDiagram
    actor D as Doctor
    participant UI as Order dialog (Angular)
    participant OC as OrderController
    participant OS as OrderService
    participant CDS as CdsService
    participant DB as PostgreSQL
    participant N as NotificationService

    D->>UI: choose drug (e.g. Aspirin for a patient on Warfarin)
    UI->>OC: POST /api/orders/check {patientId, drugId}
    OC->>CDS: evaluate(patient, drug)
    CDS->>DB: active allergies, active prescriptions, drug_interactions
    CDS-->>UI: alerts [DRUG_INTERACTION MAJOR -> CRITICAL, overridable]
    UI-->>D: show red alert + "override reason" box

    D->>UI: Place order (no reason yet)
    UI->>OC: POST /api/orders
    OC->>OS: create(request)
    OS->>CDS: evaluate again (server is authoritative)
    OS-->>UI: 409 CDS_ALERTS {details.alerts}
    D->>UI: enter override reason, Place order
    UI->>OC: POST /api/orders {..., overrideReason}
    OS->>DB: INSERT clinical_orders (order_no from order_no_seq, PLACED)
    OS->>DB: INSERT order_alerts (overridden = 1)
    OS->>DB: INSERT prescriptions (rx_no, PENDING_VERIFICATION)
    OS->>N: notify pharmacists ("Prescription awaiting verification")
    OS-->>UI: 201 OrderDetail
    Note over CDS: CONTRAINDICATED = hard stop (not overridable);<br/>allergy match = CRITICAL; duplicate therapy = WARNING
```

### 5.3 Pharmacist verification and dispensing

```mermaid
sequenceDiagram
    actor P as Pharmacist
    participant UI as Pharmacy queue (Angular)
    participant PC as PrescriptionController
    participant PS as PrescriptionService
    participant DB as PostgreSQL
    participant N as NotificationService

    P->>UI: open "To verify" tab
    UI->>PC: GET /api/prescriptions?status=PENDING_VERIFICATION
    P->>UI: select RX
    UI->>PC: GET /api/prescriptions/{id}
    PC-->>UI: prescription + CDS alerts + patient allergies + stock
    P->>UI: Verify (confirm dialog)
    UI->>PC: POST /api/prescriptions/{id}/verify {note}
    PS->>DB: status = VERIFIED, order -> IN_PROGRESS
    Note over PS,N: a rejection instead notifies the prescriber with the reason
    P->>UI: "To dispense" tab -> Dispense qty (confirm)
    UI->>PC: POST /api/prescriptions/{id}/dispense {quantity}
    PS->>DB: load drug (version column guards concurrent updates;<br/>CHECK stock_qty >= 0 in the table)
    alt enough stock
        PS->>DB: drugs.stock_qty -= qty
        PS->>DB: INSERT inventory_transactions (DISPENSE, balance)
        PS->>DB: prescription DISPENSED, order COMPLETED
        opt stock <= reorder level
            PS->>N: LOW_STOCK notification to pharmacists
        end
        PS-->>UI: 200 DISPENSED
    else insufficient stock
        PS-->>UI: 409 INSUFFICIENT_STOCK
    end
```

## 6. Order and prescription state machines

```mermaid
stateDiagram-v2
    [*] --> DRAFT: save as draft
    [*] --> PLACED: place (CDS passed / overridden)
    DRAFT --> PLACED
    DRAFT --> CANCELLED
    PLACED --> IN_PROGRESS
    PLACED --> COMPLETED
    PLACED --> CANCELLED
    IN_PROGRESS --> COMPLETED: result entered / dispensed
    IN_PROGRESS --> CANCELLED
    COMPLETED --> [*]
    CANCELLED --> [*]
```

```mermaid
stateDiagram-v2
    [*] --> PENDING_VERIFICATION
    PENDING_VERIFICATION --> VERIFIED: pharmacist verifies
    PENDING_VERIFICATION --> REJECTED: pharmacist rejects
    VERIFIED --> DISPENSED: dispense (stock deducted)
    PENDING_VERIFICATION --> DISCONTINUED: doctor stops / modifies
    VERIFIED --> DISCONTINUED
    DISPENSED --> DISCONTINUED
```
