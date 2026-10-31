# CareConnect - Test Cases

Three levels of testing were used:

1. **Automated backend tests** (JUnit 5 + Mockito + Spring MockMvc) - `cd backend && mvnw test`. They run against an in-memory
   H2 database in Oracle mode using the same Liquibase scripts and the same Hibernate dialect as production. **39 tests, all passing.**
2. **API end-to-end run** against the real Oracle database (76 scripted checks covering every module, all passing).
3. **UI end-to-end run** in a headless browser: every screen for all six roles, plus the full order -> CDS alert -> override ->
   sign note -> verify -> dispense flow (no console errors).

## 1. Functional test cases

Status column: **Pass** = verified in this build (automated test and/or scripted E2E run).

| ID | Module | Test case | Steps / input | Expected result | Status |
|---|---|---|---|---|---|
| TC-01 | Auth | Valid login | `dr.mehta` / `Password@123` | 200, access + refresh token, role DOCTOR, redirect to dashboard | Pass |
| TC-02 | Auth | Wrong password | `dr.mehta` / `wrong` | 401 `INVALID_CREDENTIALS`, generic message, LOGIN_FAILED audited | Pass |
| TC-03 | Auth | No token | `GET /api/patients` without header | 401 | Pass |
| TC-04 | Auth | Refresh rotation | Call refresh with a valid refresh token | New pair returned; old refresh token no longer valid | Pass |
| TC-05 | Auth | Refresh token reuse | Re-use an already rotated token | 401 and all sessions of the user revoked | Pass |
| TC-06 | Auth | Expired access token (UI) | Wait > 15 min then click any screen | Interceptor refreshes silently and repeats the request | Pass |
| TC-07 | RBAC | Patient searches patients | `patient.ravi` -> `GET /api/patients` | 403 | Pass |
| TC-08 | RBAC | Receptionist opens clinical chart | `recep.neha` -> `/patients/1/summary` | 403 (UI shows demographics + appointments only) | Pass |
| TC-09 | RBAC | Nurse places an order | `nurse.priya` -> `POST /api/orders` | 403 | Pass |
| TC-10 | RBAC | Doctor reads audit log | `dr.mehta` -> `/api/admin/audit-logs` | 403; menu item hidden in UI | Pass |
| TC-11 | Patients | Register patient | Valid form (name, DOB, gender, phone) | 201; MRN `CC-2026-NNNNNN` generated | Pass |
| TC-12 | Patients | Registration validation | Blank first name, invalid phone | 400 with `fieldErrors`; fields highlighted in the form | Pass |
| TC-13 | Patients | Search | Name "kumar", MRN (case-insensitive), DOB, phone | Paged, sortable results | Pass |
| TC-14 | Patients | Concurrent edit | Update with an old `version` | 409 `CONCURRENT_UPDATE` | Pass |
| TC-15 | Patients | Soft delete | Admin deletes a patient | 204; record hidden from search and chart (404), row kept in DB | Pass |
| TC-16 | Patients | Chart summary | Doctor opens Ravi Kumar | Allergy banner (Penicillin), problems, active meds, latest vitals | Pass |
| TC-17 | Clinical | Open encounter | Doctor starts visit with chief complaint | 201, status OPEN | Pass |
| TC-18 | Clinical | Vitals + BMI | 78 kg, 175 cm | BMI 25.5 stored and shown | Pass |
| TC-19 | Clinical | Implausible vitals | Systolic 900 | 400 | Pass |
| TC-20 | Clinical | SOAP note from template | Choose template, save draft | DRAFT note, template text applied | Pass |
| TC-21 | Clinical | Sign note | Sign draft | SIGNED, signer and time recorded, read-only | Pass |
| TC-22 | Clinical | Edit signed note | PUT on signed note | 409 `NOTE_SIGNED` | Pass |
| TC-23 | Clinical | Other doctor edits draft | `dr.rao` edits `dr.mehta`'s draft | 403 | Pass |
| TC-24 | Clinical | Addendum | Add addendum to signed note | New ADDENDUM note linked to the original, signed immediately | Pass |
| TC-25 | Clinical | Close with unsigned note | Close encounter with a draft | 409 `UNSIGNED_NOTES` | Pass |
| TC-26 | Clinical | One primary diagnosis | Add two PRIMARY ICD-10 codes | Second becomes primary, first becomes secondary | Pass |
| TC-27 | Clinical | ICD-10 search | "diab" | Matching codes (E11.x ...) | Pass |
| TC-28 | CPOE | Drug-allergy alert | Amoxicillin for patient allergic to Penicillin | CRITICAL `DRUG_ALLERGY` alert; 409 until override reason given | Pass |
| TC-29 | CPOE | Drug-drug interaction | Aspirin for patient on Warfarin | `DRUG_INTERACTION` alert (MAJOR -> critical, overridable) | Pass |
| TC-30 | CPOE | Contraindicated pair | Contraindicated combination | Hard stop, cannot be overridden | Pass |
| TC-31 | CPOE | Duplicate order | Same lab test / same active drug again | `DUPLICATE_ORDER` warning | Pass |
| TC-32 | CPOE | Override stored | Place with override reason | 201; alert saved with `overridden = true`; reason visible to pharmacist | Pass |
| TC-33 | CPOE | Order lifecycle | Try COMPLETED -> PLACED | Rejected (only forward moves allowed) | Pass |
| TC-34 | Results | Numeric result flagging | WBC 14.2 (normal 4-11) | Flag HIGH; order COMPLETED; doctor notified | Pass |
| TC-35 | Results | Critical values | Values beyond critical limits | CRITICAL_LOW / CRITICAL_HIGH | Pass |
| TC-36 | Pharmacy | Verification queue | Pharmacist opens "To verify" | Pending prescriptions with alerts and patient allergies | Pass |
| TC-37 | Pharmacy | Dispense before verify | Dispense a pending Rx | 409 | Pass |
| TC-38 | Pharmacy | Verify + dispense | Verify, then dispense 30 | Stock reduced by 30, ledger row written, order COMPLETED | Pass |
| TC-39 | Pharmacy | Insufficient stock | Dispense 5000 | 409 `INSUFFICIENT_STOCK` | Pass |
| TC-40 | Pharmacy | Low-stock alert | Stock falls to or below reorder level | Pharmacists notified; drug listed as low stock | Pass |
| TC-41 | MAR | Held dose needs reason | Status HELD without notes | 400 | Pass |
| TC-42 | MAR | Dose given | Nurse records GIVEN | 201; appears in MAR history | Pass |
| TC-43 | Scheduling | Book slot | Receptionist books a free slot | 201 BOOKED, slot becomes BOOKED | Pass |
| TC-44 | Scheduling | Double booking | Book the same slot again | 409 `SLOT_TAKEN` | Pass |
| TC-45 | Portal | Own profile only | Patient opens profile / another patient's visit PDF | Own record 200; other patient's PDF 404 | Pass |
| TC-46 | Portal | PDF downloads | Visit summary and prescription PDF | Valid PDF files (`%PDF` header) | Pass |
| TC-47 | Portal | Book and cancel | Patient books then cancels with reason | BOOKED via PORTAL, then CANCELLED | Pass |
| TC-48 | Portal | Cancel someone else's booking | Patient cancels another patient's appointment | 404 | Pass |
| TC-49 | Messaging | Patient <-> doctor | Patient sends, doctor sees unread and replies | Thread shows both messages; notification created | Pass |
| TC-50 | Messaging | Nurse messaging | Nurse sends to patient | 403 | Pass |
| TC-51 | Audit | Chart access logged | Doctor opens a chart | Audit rows with user, action, patient id, path | Pass |
| TC-52 | Admin | Portal account | Admin creates PATIENT user linked to a patient | 201; second account for same patient -> 409 | Pass |
| TC-53 | UI | Role menus | Log in as each role | Only permitted menu items and screens | Pass |
| TC-54 | UI | Confirmation dialogs | Sign note, verify, dispense, cancel, delete | Confirmation shown before the action | Pass |

## 2. Automated test classes

| Class | Type | Tests | What it covers |
|---|---|---|---|
| `CdsServiceTest` | Unit (Mockito) | 7 | Allergy match by name/generic/class, food allergy ignored, major interaction, contraindicated hard stop, duplicate therapy, referrals skip checks |
| `ResultFlagTest` | Unit (parameterised) | 10 | Normal/low/high/critical flagging, text results, BMI formula, order state machine |
| `PrescriptionServiceTest` | Unit (Mockito) | 5 | Verify, dispense stock deduction + ledger, low-stock notification, insufficient stock, dispense only after verify |
| `NoteServiceTest` | Unit (Mockito) | 4 | Sign draft, signed note read-only, only the author edits, addendum |
| `AuthIntegrationTest` | Integration (MockMvc + H2) | 5 | Login, wrong password, token required, refresh rotation + reuse, `/me` |
| `PatientControllerIntegrationTest` | Integration | 6 | Registration + MRN, validation errors, paging/sorting, RBAC, chart summary |
| `OrderFlowIntegrationTest` | Integration | 2 | Allergy alert -> override -> verify -> dispense; lab result flagging completes order |
| **Total** | | **39** | |

Run them with:

```bash
cd backend
./mvnw test        # Windows: mvnw.cmd test
```
