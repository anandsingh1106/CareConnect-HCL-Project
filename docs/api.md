# CareConnect REST API

Base URL: `http://localhost:8090`. Interactive documentation (Swagger UI): <http://localhost:8090/swagger-ui.html> (OpenAPI JSON at `/v3/api-docs`).
A ready-made Postman collection is in [postman/CareConnect.postman_collection.json](postman/CareConnect.postman_collection.json).

Every endpoint except login and refresh needs the header `Authorization: Bearer <accessToken>`. The **Roles** column is enforced on the
server with `@PreAuthorize`; any other role gets **403**. List endpoints described as *paged* accept `page`, `size` and `sort`
(for example `sort=lastName,asc`) and return `{ content, page, size, totalElements, totalPages, last }`.

## Error format

All errors share one JSON shape:

```json
{
  "timestamp": "2026-09-30T10:15:30Z", "status": 409, "error": "Conflict", "code": "CDS_ALERTS",
  "message": "Decision support raised alerts that need an override reason.", "path": "/api/orders",
  "fieldErrors": [],
  "details": { "alerts": [ { "type": "DRUG_ALLERGY", "severity": "CRITICAL", "message": "...", "overridable": true } ] }
}
```

| Status | When |
|---|---|
| 400 | Validation failed (`fieldErrors` lists each field) or a business rule was broken |
| 401 | Missing or invalid token. `code` is `TOKEN_EXPIRED` when the access token expired: call `/api/auth/refresh` |
| 403 | The user's role may not call this endpoint |
| 404 | Record not found (also returned for another patient's record in the portal) |
| 409 | Conflict: `CDS_ALERTS`, `CONCURRENT_UPDATE` (stale `version`), `SLOT_TAKEN`, `INSUFFICIENT_STOCK`, `NOTE_SIGNED`, `UNSIGNED_NOTES` and others |

## Authentication

| Method | Path | Roles | Description |
|---|---|---|---|
| POST | `/api/auth/login` | Public | Log in and receive an access token (15 min) and a refresh token (7 days) |
| POST | `/api/auth/refresh` | Public | Exchange a refresh token for a new token pair (the old refresh token is revoked) |
| POST | `/api/auth/logout` | Any signed-in user | Revoke the given refresh token |
| GET | `/api/auth/me` | Any signed-in user | Current user profile |
| POST | `/api/auth/change-password` | Any signed-in user | Change own password (signs out other sessions) |

## Dashboard

| Method | Path | Roles | Description |
|---|---|---|---|
| GET | `/api/dashboard` | Any signed-in user | Stat cards, charts and work list for the logged-in user's role |

## Users & departments

| Method | Path | Roles | Description |
|---|---|---|---|
| GET | `/api/departments` | Any signed-in user | List departments |
| POST | `/api/departments` | Admin | Add a department |
| PUT | `/api/departments/{id}` | Admin | Update a department |
| GET | `/api/admin/users` | Admin | List users (admin) |
| GET | `/api/admin/users/{id}` | Admin | User details |
| POST | `/api/admin/users` | Admin | Create a user account (admin) |
| PUT | `/api/admin/users/{id}` | Admin | Update a user |
| PATCH | `/api/admin/users/{id}/status` | Admin | Activate or deactivate a user (deactivation ends their sessions) |
| POST | `/api/admin/users/{id}/reset-password` | Admin | Reset a user password |
| GET | `/api/users/providers` | Any signed-in user | Doctors directory (for appointments and messaging) |

## Patients

| Method | Path | Roles | Description |
|---|---|---|---|
| GET | `/api/patients/{patientId}/documents` | Doctor, Nurse, Pharmacist | Patient's documents |
| POST | `/api/patients/{patientId}/documents` | Doctor, Nurse, Receptionist | Upload a lab report, scan or other document (PDF/PNG/JPEG/WEBP/TXT, max 10 MB) |
| GET | `/api/documents/{id}/download` | Doctor, Nurse, Pharmacist | Download a document |
| DELETE | `/api/documents/{id}` | Doctor, Admin | Delete a document |
| GET | `/api/patients` | Doctor, Nurse, Pharmacist, Receptionist, Admin | Search patients by name, MRN, date of birth or phone (paged, sortable) |
| POST | `/api/patients` | Receptionist, Doctor | Register a new patient (an MRN is generated) |
| GET | `/api/patients/{id}` | Doctor, Nurse, Pharmacist, Receptionist | Patient demographics |
| PUT | `/api/patients/{id}` | Receptionist, Doctor | Update demographics (send the last seen 'version' to detect concurrent edits) |
| DELETE | `/api/patients/{id}` | Admin | Soft-delete a patient record (admin) |
| GET | `/api/patients/{patientId}/allergies` | Doctor, Nurse, Pharmacist | Allergies |
| POST | `/api/patients/{patientId}/allergies` | Doctor, Nurse | Add an allergy |
| PUT | `/api/allergies/{id}` | Doctor, Nurse | Update an allergy |
| GET | `/api/patients/{patientId}/problems` | Doctor, Nurse, Pharmacist | Problem list |
| POST | `/api/patients/{patientId}/problems` | Doctor | Add a problem |
| PUT | `/api/problems/{id}` | Doctor | Update a problem |
| GET | `/api/patients/{patientId}/immunizations` | Doctor, Nurse, Pharmacist | Immunizations |
| POST | `/api/patients/{patientId}/immunizations` | Doctor, Nurse | Add an immunization |
| PUT | `/api/immunizations/{id}` | Doctor, Nurse | Update an immunization |
| GET | `/api/patients/{patientId}/family-history` | Doctor, Nurse, Pharmacist | Family history |
| POST | `/api/patients/{patientId}/family-history` | Doctor, Nurse | Add family history |
| PUT | `/api/family-history/{id}` | Doctor, Nurse | Update family history |

## Clinical documentation

| Method | Path | Roles | Description |
|---|---|---|---|
| GET | `/api/icd10` | Doctor, Nurse, Pharmacist, Admin | Search ICD-10 codes by code prefix or description |
| POST | `/api/icd10` | Admin | Add an ICD-10 code |
| PUT | `/api/icd10/{code}` | Admin | Update an ICD-10 code |
| GET | `/api/note-templates` | Doctor, Nurse, Pharmacist, Admin | SOAP note templates |
| POST | `/api/encounters` | Doctor, Nurse | Open an encounter (visit) for a patient |
| GET | `/api/encounters` | Doctor, Nurse | Worklist: my encounters (doctor) or all encounters (nurse) with the given status |
| GET | `/api/encounters/{id}` | Doctor, Nurse, Pharmacist | Encounter with notes, diagnoses and vitals |
| GET | `/api/patients/{patientId}/encounters` | Doctor, Nurse, Pharmacist | Patient's encounters (paged) |
| POST | `/api/encounters/{id}/close` | Doctor | Close an encounter (all notes must be signed) |
| POST | `/api/encounters/{id}/diagnoses` | Doctor | Add an ICD-10 diagnosis (PRIMARY replaces the current primary) |
| DELETE | `/api/diagnoses/{id}` | Doctor | Remove a diagnosis from an open encounter |
| GET | `/api/patients/{patientId}/notes` | Doctor, Nurse, Pharmacist | Patient's clinical notes |
| POST | `/api/encounters/{encounterId}/notes` | Doctor | Start a DRAFT SOAP note on an open encounter |
| PUT | `/api/notes/{id}` | Doctor | Edit a DRAFT note (author only) |
| POST | `/api/notes/{id}/sign` | Doctor | Sign a note - it becomes read-only |
| POST | `/api/notes/{id}/addenda` | Doctor | Add a signed addendum to a signed note |
| POST | `/api/patients/{patientId}/vitals` | Doctor, Nurse | Record vital signs (BMI is calculated automatically) |
| GET | `/api/patients/{patientId}/vitals` | Doctor, Nurse, Pharmacist | Vital sign history, oldest first (for trend charts) |

## Orders (CPOE) & results

| Method | Path | Roles | Description |
|---|---|---|---|
| POST | `/api/orders/check` | Doctor | Run decision support without saving (allergy, interaction, duplicate checks) |
| POST | `/api/orders` | Doctor | Create an order. Returns 409 CDS_ALERTS (with alerts in 'details') when an override reason is needed |
| POST | `/api/orders/{id}/place` | Doctor | Place a DRAFT order (runs decision support) |
| GET | `/api/orders` | Doctor, Nurse, Pharmacist | Order worklist with filters (paged) |
| GET | `/api/orders/{id}` | Doctor, Nurse, Pharmacist | Order with alerts, result and prescription |
| PATCH | `/api/orders/{id}/status` | Doctor, Nurse | Move a lab/imaging/referral order forward (e.g. PLACED -> IN_PROGRESS when the sample is collected) |
| POST | `/api/orders/{id}/cancel` | Doctor | Cancel an order (reason required) |
| POST | `/api/orders/{id}/results` | Doctor, Nurse | Enter a lab value or imaging report; the value is flagged against normal/critical ranges |
| GET | `/api/patients/{patientId}/orders` | Doctor, Nurse, Pharmacist | Patient's orders |
| GET | `/api/encounters/{encounterId}/orders` | Doctor, Nurse, Pharmacist | Encounter's orders |
| GET | `/api/patients/{patientId}/lab-results` | Doctor, Nurse, Pharmacist | Patient's lab and imaging results |
| GET | `/api/lab-tests` | Doctor, Nurse, Pharmacist, Admin | Lab test catalogue |
| POST | `/api/lab-tests` | Admin | Add a lab test |
| PUT | `/api/lab-tests/{id}` | Admin | Update a lab test |

## Prescriptions & MAR

| Method | Path | Roles | Description |
|---|---|---|---|
| GET | `/api/patients/{patientId}/summary` | Doctor, Nurse, Pharmacist | Chart header: demographics, active allergies, problems, medicines, latest vitals, open encounters |
| GET | `/api/patients/{patientId}/mar/due` | Doctor, Nurse | Active, verified medicines that can be administered to the patient |
| GET | `/api/patients/{patientId}/mar` | Doctor, Nurse, Pharmacist | Medication administration history |
| POST | `/api/mar` | Nurse | Document a dose given / held / refused / missed |
| GET | `/api/prescriptions` | Doctor, Nurse, Pharmacist | Prescription queue, e.g. status=PENDING_VERIFICATION for the pharmacist |
| GET | `/api/prescriptions/{id}` | Doctor, Nurse, Pharmacist | Prescription with its decision-support alerts and the patient's allergies |
| GET | `/api/patients/{patientId}/medications` | Doctor, Nurse, Pharmacist | Medication list of a patient (activeOnly=true for current medicines) |
| POST | `/api/prescriptions/{id}/verify` | Pharmacist | Pharmacist verifies a prescription |
| POST | `/api/prescriptions/{id}/reject` | Pharmacist | Pharmacist rejects a prescription (reason required) |
| POST | `/api/prescriptions/{id}/dispense` | Pharmacist | Dispense a verified prescription (reduces stock) |
| POST | `/api/prescriptions/{id}/discontinue` | Doctor | Doctor stops a medicine (reason required) |
| POST | `/api/prescriptions/{id}/modify` | Doctor | Change dose/frequency: discontinues this prescription and places a new medication order |

## Drugs & inventory

| Method | Path | Roles | Description |
|---|---|---|---|
| GET | `/api/drugs` | Doctor, Nurse, Pharmacist, Admin | Search the drug formulary (paged) |
| GET | `/api/drugs/{id}` | Doctor, Nurse, Pharmacist, Admin | Drug details and stock |
| GET | `/api/drugs/low-stock` | Pharmacist, Admin | Drugs at or below their reorder level |
| POST | `/api/drugs` | Pharmacist, Admin | Add a drug |
| PUT | `/api/drugs/{id}` | Pharmacist, Admin | Update a drug |
| POST | `/api/drugs/{id}/stock` | Pharmacist, Admin | Receive stock or adjust it (writes an inventory ledger entry) |
| GET | `/api/drugs/{id}/transactions` | Pharmacist, Admin | Last 50 stock movements |
| GET | `/api/drug-interactions` | Doctor, Nurse, Pharmacist, Admin | Drug-drug interaction table |
| POST | `/api/drug-interactions` | Pharmacist, Admin | Add an interaction |
| DELETE | `/api/drug-interactions/{id}` | Admin | Delete an interaction |

## Scheduling

| Method | Path | Roles | Description |
|---|---|---|---|
| GET | `/api/slots` | Receptionist, Doctor, Nurse, Admin | A doctor's slots on a date (availableOnly=true for booking) |
| POST | `/api/slots/generate` | Receptionist, Doctor, Nurse, Admin | Generate availability slots for a doctor over a date range |
| POST | `/api/slots/{id}/block` | Receptionist, Doctor, Nurse, Admin | Block a slot |
| POST | `/api/slots/{id}/unblock` | Receptionist, Doctor, Nurse, Admin | Unblock a slot |
| GET | `/api/appointments` | Receptionist, Doctor, Nurse, Admin | Search appointments (provider, patient, status, date range) |
| GET | `/api/patients/{patientId}/appointments` | Receptionist, Doctor, Nurse, Admin | Patient's appointments |
| POST | `/api/appointments` | Receptionist, Doctor, Nurse, Admin | Book an appointment into a free slot |
| POST | `/api/appointments/{id}/cancel` | Receptionist, Doctor, Nurse, Admin | Cancel an appointment (reason required) |
| PATCH | `/api/appointments/{id}/status` | Receptionist, Doctor, Nurse, Admin | Check in, complete or mark no-show |

## Messaging

| Method | Path | Roles | Description |
|---|---|---|---|
| GET | `/api/messages/conversations` | Doctor, Patient | My conversations (one per doctor or patient), newest first |
| GET | `/api/messages/with/{userId}` | Doctor, Patient | Messages exchanged with one user (marks them read) |
| POST | `/api/messages` | Doctor, Patient | Send a message (patient <-> doctor) |
| GET | `/api/messages/unread-count` | Doctor, Patient | Unread message count |

## Notifications

| Method | Path | Roles | Description |
|---|---|---|---|
| GET | `/api/notifications` | Any signed-in user | My notifications (paged) |
| GET | `/api/notifications/unread-count` | Any signed-in user | Unread notification count |
| POST | `/api/notifications/{id}/read` | Any signed-in user | Mark one notification read |
| POST | `/api/notifications/read-all` | Any signed-in user | Mark all notifications read |

## Patient portal

| Method | Path | Roles | Description |
|---|---|---|---|
| GET | `/api/portal/profile` | Patient | My demographics |
| GET | `/api/portal/allergies` | Patient | My allergies |
| GET | `/api/portal/problems` | Patient | My conditions |
| GET | `/api/portal/medications` | Patient | My medicines |
| GET | `/api/portal/lab-results` | Patient | My lab results |
| GET | `/api/portal/vitals` | Patient | My vitals |
| GET | `/api/portal/visits` | Patient | Visit summaries (signed notes only) |
| GET | `/api/portal/visits/{encounterId}/pdf` | Patient | Download a visit summary as PDF |
| GET | `/api/portal/prescriptions/{prescriptionId}/pdf` | Patient | Download a prescription as PDF |
| GET | `/api/portal/appointments` | Patient | My appointments |
| GET | `/api/portal/slots` | Patient | Free slots for a doctor on a date |
| POST | `/api/portal/appointments` | Patient | Book an appointment |
| POST | `/api/portal/appointments/{id}/cancel` | Patient | Cancel my appointment |

## Audit

| Method | Path | Roles | Description |
|---|---|---|---|
| GET | `/api/admin/audit-logs` | Admin | Search the audit trail (newest first) |

_122 endpoints in total._
