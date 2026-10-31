// TypeScript mirrors of the backend DTOs (com.careconnect.*.dto). Dates arrive as ISO strings.

export type Role = 'ADMIN' | 'DOCTOR' | 'NURSE' | 'PHARMACIST' | 'RECEPTIONIST' | 'PATIENT';
export const ROLES: Role[] = ['ADMIN', 'DOCTOR', 'NURSE', 'PHARMACIST', 'RECEPTIONIST', 'PATIENT'];

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface ApiError {
  status: number;
  error: string;
  code: string;
  message: string;
  path: string;
  fieldErrors?: { field: string; message: string }[];
  details?: any;
}

// ---------- auth / users ----------
export interface UserInfo {
  id: number;
  username: string;
  fullName: string;
  email?: string;
  role: Role;
  patientId?: number;
  departmentName?: string;
  specialty?: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserInfo;
}

export interface User {
  id: number;
  username: string;
  email?: string;
  fullName: string;
  role: Role;
  departmentId?: number;
  departmentName?: string;
  phone?: string;
  specialty?: string;
  licenseNo?: string;
  patientId?: number;
  active: boolean;
  lastLoginAt?: string;
  createdAt: string;
}

export interface Provider {
  id: number;
  fullName: string;
  specialty?: string;
  departmentId?: number;
  departmentName?: string;
}

export interface Department {
  id: number;
  code: string;
  name: string;
  description?: string;
  active: boolean;
}

export interface AuditLog {
  id: number;
  userId?: number;
  username?: string;
  userRole?: string;
  action: string;
  resourceType?: string;
  resourceId?: string;
  patientId?: number;
  httpMethod?: string;
  requestPath?: string;
  statusCode?: number;
  ipAddress?: string;
  details?: string;
  createdAt: string;
}

// ---------- patients ----------
export type Gender = 'MALE' | 'FEMALE' | 'OTHER';

export interface PatientSummary {
  id: number;
  mrn: string;
  fullName: string;
  dateOfBirth?: string;
  age?: number;
  gender: Gender;
  phone?: string;
  city?: string;
  primaryProviderName?: string;
}

export interface Patient extends PatientSummary {
  firstName: string;
  lastName: string;
  bloodGroup?: string;
  email?: string;
  addressLine?: string;
  stateName?: string;
  postalCode?: string;
  emergencyName?: string;
  emergencyPhone?: string;
  emergencyRelation?: string;
  insuranceProvider?: string;
  insurancePolicyNo?: string;
  insuranceValidTill?: string;
  medicalHistory?: string;
  primaryProviderId?: number;
  createdAt: string;
  updatedAt?: string;
  version: number;
}

export interface Allergy {
  id: number;
  patientId: number;
  allergen: string;
  allergyType: 'DRUG' | 'FOOD' | 'ENVIRONMENT' | 'OTHER';
  reaction?: string;
  severity: 'MILD' | 'MODERATE' | 'SEVERE';
  status: 'ACTIVE' | 'INACTIVE';
  notedOn?: string;
  notes?: string;
  createdBy?: string;
}

export interface Problem {
  id: number;
  patientId: number;
  icd10Code?: string;
  icd10Description?: string;
  description: string;
  status: 'ACTIVE' | 'RESOLVED';
  onsetDate?: string;
  resolvedDate?: string;
  createdBy?: string;
}

export interface Immunization {
  id: number;
  patientId: number;
  vaccineName: string;
  doseNumber?: number;
  administeredOn: string;
  administeredBy?: string;
  lotNumber?: string;
  notes?: string;
}

export interface FamilyHistory {
  id: number;
  patientId: number;
  relation: string;
  conditionName: string;
  notes?: string;
}

export interface PatientDocument {
  id: number;
  patientId: number;
  category: string;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  description?: string;
  uploadedBy?: string;
  uploadedAt: string;
}

// ---------- clinical ----------
export type EncounterStatus = 'OPEN' | 'CLOSED' | 'CANCELLED';
export type EncounterType = 'OUTPATIENT' | 'INPATIENT' | 'EMERGENCY' | 'TELEHEALTH' | 'FOLLOW_UP';

export interface Encounter {
  id: number;
  patientId: number;
  patientName: string;
  patientMrn: string;
  providerId: number;
  providerName: string;
  departmentName?: string;
  appointmentId?: number;
  encounterType: EncounterType;
  status: EncounterStatus;
  encounterDate: string;
  chiefComplaint?: string;
  closedAt?: string;
  primaryDiagnosis?: string;
  createdAt: string;
}

export interface Note {
  id: number;
  encounterId: number;
  patientId: number;
  authorId: number;
  authorName: string;
  noteType: 'PROGRESS' | 'ADDENDUM';
  parentNoteId?: number;
  templateCode?: string;
  subjective?: string;
  objective?: string;
  assessment?: string;
  plan?: string;
  status: 'DRAFT' | 'SIGNED';
  signedAt?: string;
  signedByName?: string;
  createdAt: string;
  updatedAt?: string;
  version: number;
}

export interface Diagnosis {
  id: number;
  encounterId: number;
  icd10Code: string;
  description: string;
  diagnosisType: 'PRIMARY' | 'SECONDARY';
  notes?: string;
}

export interface Vital {
  id: number;
  patientId: number;
  encounterId?: number;
  recordedByName?: string;
  recordedAt: string;
  systolic?: number;
  diastolic?: number;
  pulse?: number;
  temperature?: number;
  respRate?: number;
  spo2?: number;
  weightKg?: number;
  heightCm?: number;
  bmi?: number;
  notes?: string;
}

export interface EncounterDetail {
  encounter: Encounter;
  notes: Note[];
  diagnoses: Diagnosis[];
  vitals: Vital[];
}

export interface Icd10 {
  code: string;
  description: string;
  category?: string;
  active: boolean;
}

export interface NoteTemplate {
  id: number;
  code: string;
  name: string;
  specialty?: string;
  subjective?: string;
  objective?: string;
  assessment?: string;
  plan?: string;
}

export interface ChartSummary {
  patient: Patient;
  allergies: Allergy[];
  problems: Problem[];
  activeMedications: Prescription[];
  latestVitals?: Vital;
  openEncounters: Encounter[];
}

// ---------- orders ----------
export type OrderType = 'LAB' | 'IMAGING' | 'MEDICATION' | 'REFERRAL';
export type OrderStatus = 'DRAFT' | 'PLACED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type OrderPriority = 'ROUTINE' | 'URGENT' | 'STAT';
export type AlertSeverity = 'INFO' | 'WARNING' | 'CRITICAL';

export interface CdsAlert {
  type: 'DRUG_ALLERGY' | 'DRUG_INTERACTION' | 'DUPLICATE_ORDER';
  severity: AlertSeverity;
  message: string;
  overridable: boolean;
}

export interface CdsResult {
  alerts: CdsAlert[];
  requiresOverride: boolean;
  blocked: boolean;
}

export interface Order {
  id: number;
  orderNo: string;
  patientId: number;
  patientName: string;
  patientMrn: string;
  encounterId?: number;
  orderedById: number;
  orderedByName: string;
  orderType: OrderType;
  status: OrderStatus;
  priority: OrderPriority;
  labTestId?: number;
  labTestName?: string;
  imagingStudy?: string;
  bodySite?: string;
  referralSpecialty?: string;
  referralTo?: string;
  clinicalIndication?: string;
  instructions?: string;
  alertOverrideReason?: string;
  summary: string;
  placedAt?: string;
  startedAt?: string;
  completedAt?: string;
  cancelledAt?: string;
  cancelReason?: string;
  createdAt: string;
}

export interface OrderAlert {
  id: number;
  alertType: string;
  severity: AlertSeverity;
  message: string;
  overridden: boolean;
  createdAt: string;
}

export type ResultFlag = 'NORMAL' | 'LOW' | 'HIGH' | 'CRITICAL_LOW' | 'CRITICAL_HIGH' | 'ABNORMAL';

export interface LabResult {
  id: number;
  orderId: number;
  orderNo: string;
  orderType: OrderType;
  patientId: number;
  labTestId?: number;
  labTestCode?: string;
  testName: string;
  valueNumeric?: number;
  valueText?: string;
  unit?: string;
  referenceRange?: string;
  flag: ResultFlag;
  resultStatus: 'PRELIMINARY' | 'FINAL' | 'CORRECTED';
  comments?: string;
  resultedAt: string;
  resultedByName?: string;
}

export interface OrderDetail {
  order: Order;
  alerts: OrderAlert[];
  result?: LabResult;
  prescription?: Prescription;
}

export interface LabTest {
  id: number;
  code: string;
  name: string;
  category?: string;
  specimen?: string;
  unit?: string;
  normalLow?: number;
  normalHigh?: number;
  criticalLow?: number;
  criticalHigh?: number;
  turnaroundHours?: number;
  active: boolean;
}

// ---------- medication ----------
export type Route = 'ORAL' | 'IV' | 'IM' | 'SC' | 'TOPICAL' | 'INHALED' | 'SUBLINGUAL' | 'RECTAL' | 'OPHTHALMIC' | 'OTHER';
export const ROUTES: Route[] = ['ORAL', 'IV', 'IM', 'SC', 'TOPICAL', 'INHALED', 'SUBLINGUAL', 'RECTAL', 'OPHTHALMIC', 'OTHER'];
export type PrescriptionStatus = 'DRAFT' | 'PENDING_VERIFICATION' | 'VERIFIED' | 'DISPENSED' | 'REJECTED' | 'DISCONTINUED';

export interface Prescription {
  id: number;
  rxNo: string;
  orderId: number;
  orderNo: string;
  patientId: number;
  patientName: string;
  patientMrn: string;
  encounterId?: number;
  prescriberId: number;
  prescriberName: string;
  drugId: number;
  drugCode: string;
  drugName: string;
  genericName?: string;
  drugClass?: string;
  drugStock?: number;
  dose: string;
  route: Route;
  frequency: string;
  durationDays?: number;
  quantity?: number;
  instructions?: string;
  status: PrescriptionStatus;
  currentlyActive: boolean;
  verifiedByName?: string;
  verifiedAt?: string;
  pharmacistNote?: string;
  dispensedByName?: string;
  dispensedAt?: string;
  dispensedQty?: number;
  discontinuedByName?: string;
  discontinuedAt?: string;
  discontinueReason?: string;
  replacesId?: number;
  createdAt: string;
}

export interface PrescriptionDetail {
  prescription: Prescription;
  alerts: OrderAlert[];
  patientAllergies: string[];
}

export interface Administration {
  id: number;
  prescriptionId: number;
  rxNo: string;
  drugName: string;
  patientId: number;
  administeredByName: string;
  administeredAt: string;
  doseGiven: string;
  route: Route;
  status: 'GIVEN' | 'HELD' | 'REFUSED' | 'MISSED';
  notes?: string;
}

export interface Drug {
  id: number;
  code: string;
  name: string;
  genericName: string;
  drugClass?: string;
  dosageForm: string;
  strength?: string;
  defaultRoute?: string;
  unitPrice?: number;
  stockQty: number;
  reorderLevel: number;
  lowStock: boolean;
  active: boolean;
}

export interface InventoryTransaction {
  id: number;
  drugId: number;
  changeQty: number;
  balanceAfter: number;
  txnType: 'RECEIPT' | 'DISPENSE' | 'ADJUSTMENT';
  reference?: string;
  performedByName?: string;
  createdAt: string;
}

export interface Interaction {
  id: number;
  drugAId: number;
  drugAName: string;
  drugBId: number;
  drugBName: string;
  severity: 'MINOR' | 'MODERATE' | 'MAJOR' | 'CONTRAINDICATED';
  description: string;
}

// ---------- scheduling ----------
export interface Slot {
  id: number;
  providerId: number;
  providerName: string;
  slotStart: string;
  slotEnd: string;
  status: 'AVAILABLE' | 'BOOKED' | 'BLOCKED';
}

export type AppointmentStatus = 'BOOKED' | 'CHECKED_IN' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW';

export interface Appointment {
  id: number;
  patientId: number;
  patientName: string;
  patientMrn: string;
  providerId: number;
  providerName: string;
  providerSpecialty?: string;
  slotId: number;
  startTime: string;
  endTime: string;
  appointmentType: 'IN_PERSON' | 'TELEHEALTH';
  reason?: string;
  status: AppointmentStatus;
  bookedVia: 'STAFF' | 'PORTAL';
  cancelReason?: string;
  createdAt: string;
}

// ---------- messaging / notifications ----------
export interface Message {
  id: number;
  patientId: number;
  senderId: number;
  senderName: string;
  recipientId: number;
  recipientName: string;
  subject: string;
  body: string;
  readAt?: string;
  createdAt: string;
  mine: boolean;
}

export interface Conversation {
  counterpartId: number;
  counterpartName: string;
  counterpartRole: Role;
  patientId: number;
  patientName: string;
  lastSubject: string;
  lastPreview: string;
  lastAt: string;
  unread: number;
}

export interface AppNotification {
  id: number;
  title: string;
  message: string;
  type: string;
  link?: string;
  read: boolean;
  createdAt: string;
}

// ---------- dashboard / portal ----------
export interface Dashboard {
  role: Role;
  cards: { key: string; label: string; value: number; icon: string; tone: string; link?: string }[];
  charts: { key: string; title: string; type: 'bar' | 'line' | 'doughnut' | 'pie'; labels: string[]; series: { name: string; data: number[] }[] }[];
  workList?: { title: string; items: { title: string; subtitle?: string; meta?: string; link?: string }[] };
}

export interface VisitSummary {
  encounterId: number;
  encounterDate: string;
  providerName: string;
  departmentName?: string;
  encounterType: EncounterType;
  status: EncounterStatus;
  chiefComplaint?: string;
  diagnoses: string[];
  assessment?: string;
  plan?: string;
  medications: string[];
  vitals?: Vital;
}
