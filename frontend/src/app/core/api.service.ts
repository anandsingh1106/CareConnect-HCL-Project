import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom, map, Observable } from 'rxjs';
import {
  Administration, Allergy, Appointment, AppNotification, AuditLog, CdsResult, ChartSummary, Conversation, Dashboard,
  Department, Diagnosis, Drug, Encounter, EncounterDetail, FamilyHistory, Icd10, Immunization, InventoryTransaction,
  Interaction, LabResult, LabTest, Message, Note, NoteTemplate, Order, OrderDetail, Page, Patient, PatientDocument,
  PatientSummary, Prescription, PrescriptionDetail, Problem, Provider, Slot, User, VisitSummary, Vital,
} from './models';

type Params = Record<string, string | number | boolean | null | undefined>;

/** Typed wrapper around the CareConnect REST API (see Swagger UI at /swagger-ui.html). */
@Injectable({ providedIn: 'root' })
export class Api {
  private readonly http = inject(HttpClient);

  // ---------- generic helpers ----------
  get<T>(url: string, params?: Params): Observable<T> {
    return this.http.get<T>(url, { params: toParams(params) });
  }
  post<T>(url: string, body: unknown = {}): Observable<T> {
    return this.http.post<T>(url, body);
  }
  put<T>(url: string, body: unknown): Observable<T> {
    return this.http.put<T>(url, body);
  }
  patch<T>(url: string, body: unknown): Observable<T> {
    return this.http.patch<T>(url, body);
  }
  delete(url: string): Observable<void> {
    return this.http.delete<void>(url);
  }

  /** Downloads a protected file (PDF, document) through HttpClient so the bearer token is attached. */
  async download(url: string, fallbackName: string): Promise<void> {
    const res: HttpResponse<Blob> = await firstValueFrom(this.http.get(url, { observe: 'response', responseType: 'blob' }));
    const disposition = res.headers.get('Content-Disposition') ?? '';
    const match = /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(disposition);
    const name = match ? decodeURIComponent(match[1]) : fallbackName;
    const href = URL.createObjectURL(res.body!);
    const a = document.createElement('a');
    a.href = href;
    a.download = name;
    a.click();
    setTimeout(() => URL.revokeObjectURL(href), 10_000);
  }

  // ---------- dashboard / directory ----------
  dashboard() { return this.get<Dashboard>('/api/dashboard'); }
  providers(departmentId?: number) { return this.get<Provider[]>('/api/users/providers', { departmentId }); }
  departments() { return this.get<Department[]>('/api/departments'); }

  // ---------- patients ----------
  searchPatients(p: Params) { return this.get<Page<PatientSummary>>('/api/patients', p); }
  patient(id: number) { return this.get<Patient>(`/api/patients/${id}`); }
  registerPatient(body: unknown) { return this.post<Patient>('/api/patients', body); }
  updatePatient(id: number, body: unknown) { return this.put<Patient>(`/api/patients/${id}`, body); }
  deletePatient(id: number) { return this.delete(`/api/patients/${id}`); }
  chartSummary(id: number) { return this.get<ChartSummary>(`/api/patients/${id}/summary`); }

  allergies(pid: number) { return this.get<Allergy[]>(`/api/patients/${pid}/allergies`); }
  saveAllergy(pid: number, body: unknown, id?: number) {
    return id ? this.put<Allergy>(`/api/allergies/${id}`, body) : this.post<Allergy>(`/api/patients/${pid}/allergies`, body);
  }
  problems(pid: number) { return this.get<Problem[]>(`/api/patients/${pid}/problems`); }
  saveProblem(pid: number, body: unknown, id?: number) {
    return id ? this.put<Problem>(`/api/problems/${id}`, body) : this.post<Problem>(`/api/patients/${pid}/problems`, body);
  }
  immunizations(pid: number) { return this.get<Immunization[]>(`/api/patients/${pid}/immunizations`); }
  saveImmunization(pid: number, body: unknown, id?: number) {
    return id ? this.put<Immunization>(`/api/immunizations/${id}`, body) : this.post<Immunization>(`/api/patients/${pid}/immunizations`, body);
  }
  familyHistory(pid: number) { return this.get<FamilyHistory[]>(`/api/patients/${pid}/family-history`); }
  saveFamilyHistory(pid: number, body: unknown, id?: number) {
    return id ? this.put<FamilyHistory>(`/api/family-history/${id}`, body) : this.post<FamilyHistory>(`/api/patients/${pid}/family-history`, body);
  }
  documents(pid: number) { return this.get<PatientDocument[]>(`/api/patients/${pid}/documents`); }
  uploadDocument(pid: number, file: File, category: string, description: string) {
    const form = new FormData();
    form.append('file', file);
    form.append('category', category);
    if (description) form.append('description', description);
    return this.http.post<PatientDocument>(`/api/patients/${pid}/documents`, form);
  }
  deleteDocument(id: number) { return this.delete(`/api/documents/${id}`); }

  // ---------- clinical ----------
  encounterWorklist(status?: string) { return this.get<Encounter[]>('/api/encounters', { status }); }
  patientEncounters(pid: number, page = 0, size = 10) {
    return this.get<Page<Encounter>>(`/api/patients/${pid}/encounters`, { page, size, sort: 'encounterDate,desc' });
  }
  encounter(id: number) { return this.get<EncounterDetail>(`/api/encounters/${id}`); }
  openEncounter(body: unknown) { return this.post<Encounter>('/api/encounters', body); }
  closeEncounter(id: number) { return this.post<Encounter>(`/api/encounters/${id}/close`); }
  addDiagnosis(encounterId: number, body: unknown) { return this.post<Diagnosis>(`/api/encounters/${encounterId}/diagnoses`, body); }
  removeDiagnosis(id: number) { return this.delete(`/api/diagnoses/${id}`); }
  patientNotes(pid: number) { return this.get<Note[]>(`/api/patients/${pid}/notes`); }
  createNote(encounterId: number, body: unknown) { return this.post<Note>(`/api/encounters/${encounterId}/notes`, body); }
  updateNote(id: number, body: unknown) { return this.put<Note>(`/api/notes/${id}`, body); }
  signNote(id: number) { return this.post<Note>(`/api/notes/${id}/sign`); }
  addendum(id: number, body: unknown) { return this.post<Note>(`/api/notes/${id}/addenda`, body); }
  vitals(pid: number) { return this.get<Vital[]>(`/api/patients/${pid}/vitals`); }
  recordVitals(pid: number, body: unknown) { return this.post<Vital>(`/api/patients/${pid}/vitals`, body); }
  icd10(q: string, activeOnly = true, page = 0, size = 15) { return this.get<Page<Icd10>>('/api/icd10', { q, activeOnly, page, size }); }
  saveIcd10(body: Icd10, isNew: boolean) {
    return isNew ? this.post<Icd10>('/api/icd10', body) : this.put<Icd10>(`/api/icd10/${encodeURIComponent(body.code)}`, body);
  }
  noteTemplates() { return this.get<NoteTemplate[]>('/api/note-templates'); }

  // ---------- orders ----------
  checkOrder(body: unknown) { return this.post<CdsResult>('/api/orders/check', body); }
  createOrder(body: unknown) { return this.post<OrderDetail>('/api/orders', body); }
  placeOrder(id: number, overrideReason?: string) { return this.post<OrderDetail>(`/api/orders/${id}/place`, { overrideReason }); }
  orders(p: Params) { return this.get<Page<Order>>('/api/orders', p); }
  order(id: number) { return this.get<OrderDetail>(`/api/orders/${id}`); }
  orderStatus(id: number, status: string, note?: string) { return this.patch<OrderDetail>(`/api/orders/${id}/status`, { status, note }); }
  cancelOrder(id: number, reason: string) { return this.post<OrderDetail>(`/api/orders/${id}/cancel`, { reason }); }
  enterResult(id: number, body: unknown) { return this.post<LabResult>(`/api/orders/${id}/results`, body); }
  patientOrders(pid: number) { return this.get<Order[]>(`/api/patients/${pid}/orders`); }
  encounterOrders(encounterId: number) { return this.get<Order[]>(`/api/encounters/${encounterId}/orders`); }
  labResults(pid: number) { return this.get<LabResult[]>(`/api/patients/${pid}/lab-results`); }
  labTests(activeOnly = true) { return this.get<LabTest[]>('/api/lab-tests', { activeOnly }); }
  saveLabTest(body: unknown, id?: number) { return id ? this.put<LabTest>(`/api/lab-tests/${id}`, body) : this.post<LabTest>('/api/lab-tests', body); }

  // ---------- medication ----------
  prescriptions(p: Params) { return this.get<Page<Prescription>>('/api/prescriptions', p); }
  prescription(id: number) { return this.get<PrescriptionDetail>(`/api/prescriptions/${id}`); }
  patientMedications(pid: number, activeOnly = false) { return this.get<Prescription[]>(`/api/patients/${pid}/medications`, { activeOnly }); }
  verifyRx(id: number, note?: string) { return this.post<Prescription>(`/api/prescriptions/${id}/verify`, { note }); }
  rejectRx(id: number, reason: string) { return this.post<Prescription>(`/api/prescriptions/${id}/reject`, { reason }); }
  dispenseRx(id: number, quantity?: number) { return this.post<Prescription>(`/api/prescriptions/${id}/dispense`, { quantity }); }
  discontinueRx(id: number, reason: string) { return this.post<Prescription>(`/api/prescriptions/${id}/discontinue`, { reason }); }
  modifyRx(id: number, body: unknown) { return this.post<OrderDetail>(`/api/prescriptions/${id}/modify`, body); }
  marDue(pid: number) { return this.get<Prescription[]>(`/api/patients/${pid}/mar/due`); }
  marHistory(pid: number) { return this.get<Administration[]>(`/api/patients/${pid}/mar`); }
  recordMar(body: unknown) { return this.post<Administration>('/api/mar', body); }
  drugs(p: Params) { return this.get<Page<Drug>>('/api/drugs', p); }
  lowStock() { return this.get<Drug[]>('/api/drugs/low-stock'); }
  saveDrug(body: unknown, id?: number) { return id ? this.put<Drug>(`/api/drugs/${id}`, body) : this.post<Drug>('/api/drugs', body); }
  adjustStock(id: number, body: unknown) { return this.post<Drug>(`/api/drugs/${id}/stock`, body); }
  drugTransactions(id: number) { return this.get<InventoryTransaction[]>(`/api/drugs/${id}/transactions`); }
  interactions(drugId?: number) { return this.get<Interaction[]>('/api/drug-interactions', { drugId }); }
  addInteraction(body: unknown) { return this.post<Interaction>('/api/drug-interactions', body); }
  deleteInteraction(id: number) { return this.delete(`/api/drug-interactions/${id}`); }

  // ---------- scheduling ----------
  slots(providerId: number, date: string, availableOnly = false) { return this.get<Slot[]>('/api/slots', { providerId, date, availableOnly }); }
  generateSlots(body: unknown) { return this.post<{ created: number; skipped: number }>('/api/slots/generate', body); }
  blockSlot(id: number) { return this.post<Slot>(`/api/slots/${id}/block`); }
  unblockSlot(id: number) { return this.post<Slot>(`/api/slots/${id}/unblock`); }
  appointments(p: Params) { return this.get<Page<Appointment>>('/api/appointments', p); }
  patientAppointments(pid: number) { return this.get<Appointment[]>(`/api/patients/${pid}/appointments`); }
  bookAppointment(body: unknown) { return this.post<Appointment>('/api/appointments', body); }
  cancelAppointment(id: number, reason: string) { return this.post<Appointment>(`/api/appointments/${id}/cancel`, { reason }); }
  appointmentStatus(id: number, status: string) { return this.patch<Appointment>(`/api/appointments/${id}/status`, { status }); }

  // ---------- messaging / notifications ----------
  conversations() { return this.get<Conversation[]>('/api/messages/conversations'); }
  thread(userId: number) { return this.get<Message[]>(`/api/messages/with/${userId}`); }
  sendMessage(body: unknown) { return this.post<Message>('/api/messages', body); }
  notifications(unreadOnly = false, size = 10) { return this.get<Page<AppNotification>>('/api/notifications', { unreadOnly, size }); }
  unreadNotifications() { return this.get<{ count: number }>('/api/notifications/unread-count').pipe(map(r => r.count ?? 0)); }
  markNotificationRead(id: number) { return this.post<void>(`/api/notifications/${id}/read`); }
  markAllNotificationsRead() { return this.post<void>('/api/notifications/read-all'); }

  // ---------- admin ----------
  users(p: Params) { return this.get<Page<User>>('/api/admin/users', p); }
  createUser(body: unknown) { return this.post<User>('/api/admin/users', body); }
  updateUser(id: number, body: unknown) { return this.put<User>(`/api/admin/users/${id}`, body); }
  setUserActive(id: number, active: boolean) { return this.patch<User>(`/api/admin/users/${id}/status`, { active }); }
  resetPassword(id: number, newPassword: string) { return this.post<void>(`/api/admin/users/${id}/reset-password`, { newPassword }); }
  saveDepartment(body: unknown, id?: number) { return id ? this.put<Department>(`/api/departments/${id}`, body) : this.post<Department>('/api/departments', body); }
  auditLogs(p: Params) { return this.get<Page<AuditLog>>('/api/admin/audit-logs', p); }

  // ---------- patient portal ----------
  portalProfile() { return this.get<Patient>('/api/portal/profile'); }
  portalAllergies() { return this.get<Allergy[]>('/api/portal/allergies'); }
  portalProblems() { return this.get<Problem[]>('/api/portal/problems'); }
  portalMedications() { return this.get<Prescription[]>('/api/portal/medications'); }
  portalLabResults() { return this.get<LabResult[]>('/api/portal/lab-results'); }
  portalVitals() { return this.get<Vital[]>('/api/portal/vitals'); }
  portalVisits() { return this.get<VisitSummary[]>('/api/portal/visits'); }
  portalAppointments() { return this.get<Appointment[]>('/api/portal/appointments'); }
  portalSlots(providerId: number, date: string) { return this.get<Slot[]>('/api/portal/slots', { providerId, date }); }
  portalBook(body: unknown) { return this.post<Appointment>('/api/portal/appointments', body); }
  portalCancel(id: number, reason: string) { return this.post<Appointment>(`/api/portal/appointments/${id}/cancel`, { reason }); }
}

function toParams(params?: Params): HttpParams {
  let result = new HttpParams();
  Object.entries(params ?? {}).forEach(([key, value]) => {
    if (value !== null && value !== undefined && value !== '') {
      result = result.set(key, String(value));
    }
  });
  return result;
}
