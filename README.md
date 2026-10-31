# CareConnect – Patient-Provider EHR

CareConnect is an electronic health record system for an outpatient clinic. Doctors, nurses, pharmacists, receptionists,
administrators and patients each get their own role-based workspace on top of one PostgreSQL database.

| | |
|---|---|
| **Backend** | Java 17, Spring Boot 3.5 (Web, Data JPA, Security, Validation), JWT (jjwt), MapStruct, Lombok, Liquibase, springdoc OpenAPI, OpenPDF |
| **Frontend** | Angular 21 (standalone components, signals, reactive forms, guards, interceptors), Angular Material, Chart.js |
| **Database** | PostgreSQL 16+ with sequences, constraints, indexes, text columns |
| **Tests** | JUnit 5, Mockito, Spring MockMvc, H2 in PostgreSQL mode (39 tests) |

## Features

- **Patient records** – registration with auto-generated MRN (`CC-2026-000001`), search by name/MRN/DOB/phone, allergies,
  problem list (ICD-10), immunizations, family history, document upload, soft delete, optimistic locking.
- **Clinical documentation** – encounters, SOAP notes from templates, e-signature with locking, addenda, ICD-10 diagnoses,
  vitals with automatic BMI and trend charts.
- **CPOE** – lab, imaging, medication and referral orders with **clinical decision support**: drug-allergy, drug-drug
  interaction and duplicate-order alerts, override reasons, hard stops for contraindicated combinations; lab results with
  automatic normal/abnormal/critical flagging.
- **Medication management** – e-prescriptions, pharmacist verify/reject and dispense queues, stock deduction with ledger,
  low-stock alerts, drug inventory, MAR (nurse documents given/held/refused doses), discontinue and modify.
- **Patient portal** – own medicines, lab results, visit summaries, PDF downloads (visit summary, prescription), book and
  cancel appointments, secure messaging with doctors.
- **Cross-cutting** – appointment slots and booking, role dashboards with charts, in-app notifications, audit log of every
  patient-data access and login, confirmation dialogs, responsive UI.

## Project structure

```
demo/
├── backend/                 Spring Boot REST API  -> http://localhost:8090
│   └── src/main/resources/db/changelog/   PostgreSQL DDL + seed data (Liquibase)
├── frontend/                Angular app           -> http://localhost:4200
└── docs/
    ├── architecture.md      architecture, ER, use-case, sequence and state diagrams (Mermaid)
    ├── api.md               list of all 122 REST endpoints with roles
    ├── test-cases.md        test case table
    ├── postman/             Postman collection
    └── db/                  create-schema-user.sql (PostgreSQL user setup / reset)
```

## Prerequisites

| Tool | Version |
|---|---|
| JDK | 17 or newer (`java -version`) |
| Node.js | 20.19+ or 22+ (`node -v`) |
| PostgreSQL Database | 16 or newer, listening on port 5432 |

Maven does not need to be installed – the project ships with the Maven wrapper (`mvnw` / `mvnw.cmd`).

## 1. PostgreSQL setup

Create the local database and role by running `psql -U postgres -f docs/db/create-schema-user.sql`. Liquibase creates the tables, sequences, constraints, indexes, master data, and demo data when the backend starts.

Set `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, and `DB_PASSWORD` to connect to another PostgreSQL instance.

## 2. Run the backend

```bash
cd backend
./mvnw spring-boot:run          # Windows: .\mvnw.cmd spring-boot:run
```

It starts on **http://localhost:8090** (PORT can override the default port 8090). Useful URLs:

- Swagger UI: <http://localhost:8090/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8090/v3/api-docs>

The `dev` profile is active by default (local PostgreSQL, demo data, appointment slots for the next 7 days are generated on startup).

## 3. Run the frontend

```bash
cd frontend
npm install        # first time only
npm start          # ng serve with proxy: /api -> http://localhost:8090
```

Open **http://localhost:4200**.

## Demo accounts

All demo users have the password **`Password@123`**. The login page has one-click buttons for each role.

| Role | Username | What to try |
|---|---|---|
| Doctor | `dr.mehta` (also `dr.rao`) | Open Ravi Kumar, start an encounter, order Amoxicillin → allergy alert; sign a SOAP note |
| Nurse | `nurse.priya` | Record vitals, Medication (MAR) round, enter lab results |
| Pharmacist | `pharm.arjun` | Verify and dispense prescriptions, inventory and low-stock alerts |
| Receptionist | `recep.neha` | Register a patient, book and check in appointments |
| Admin | `admin` | Users, departments, ICD-10 / lab master data, audit log |
| Patient | `patient.ravi` (also `patient.sita`) | Health record, PDFs, book an appointment, message the doctor |

## Configuration and profiles

All secrets come from environment variables; nothing sensitive is hard-coded for production.

| Variable | Used for | Dev default |
|---|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL connection | `localhost`, `5432`, `careconnect`, `careconnect`, `careconnect` |
| `JWT_SECRET` | HMAC key for tokens (min. 32 characters) | a dev-only key in `application-dev.yml` |
| `CORS_ALLOWED_ORIGINS` | Allowed browser origins | `http://localhost:4200` |
| `UPLOAD_DIR` | Where uploaded documents are stored | `./uploads` |

Profiles: `dev` (default – local PostgreSQL, demo data, Swagger), `prod` (everything from the environment, Swagger off,
`--spring.profiles.active=prod`), `test` (automated tests on H2).

## Tests

```bash
cd backend
./mvnw test
```

39 unit and integration tests. See [docs/test-cases.md](docs/test-cases.md) for the full test case table.

## Build for production

```bash
cd backend && ./mvnw clean package          # -> backend/target/careconnect-backend.jar
cd frontend && npm run build                # -> frontend/dist/careconnect-ui/browser
java -jar backend/target/careconnect-backend.jar --spring.profiles.active=prod
```

Serve the frontend files from any web server that forwards `/api` to the backend and falls back to `index.html` for
unknown paths (Angular routing).

## Documentation

- [Architecture, ER diagram, use cases and sequence diagrams](docs/architecture.md)
- [REST API reference](docs/api.md) and [Postman collection](docs/postman/CareConnect.postman_collection.json)
- [Test cases](docs/test-cases.md)

## Deploy backend on Render

The root render.yaml defines a Docker-based Spring Boot service and PostgreSQL database. Import the repository as a Render Blueprint. The free database expires after 30 days and the free web service can sleep, so use this setup for a demo only. Set CORS_ALLOWED_ORIGINS to the deployed frontend origin before connecting the Vercel app.

## Troubleshooting

| Problem | Fix |
|---|---|
| `PostgreSQL authentication failed` | Run `docs/db/create-schema-user.sql` again, or set `DB_USERNAME` / `DB_PASSWORD` |
| `Connection refused` | Confirm PostgreSQL is running and `DB_HOST` / `DB_PORT` point to it |
| Port 8090 already in use | Stop the other process or start with `--server.port=8091` and update `frontend/proxy.conf.json` |
| Want fresh demo data | Stop the backend, run `create-schema-user.sql`, start the backend again |
| Maven "Network is unreachable" | Already handled by `backend/.mvn/jvm.config` (forces IPv4) |
