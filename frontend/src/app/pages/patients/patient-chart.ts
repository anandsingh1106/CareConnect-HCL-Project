import { DatePipe } from '@angular/common';
import { Component, computed, inject, input, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router, RouterLink } from '@angular/router';
import { ChartConfiguration } from 'chart.js';
import { Api } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import {
  Administration, Allergy, Appointment, ChartSummary, Encounter, FamilyHistory, Immunization, LabResult, Order, Patient,
  PatientDocument, Prescription, Problem, Vital,
} from '../../core/models';
import { fileSize } from '../../core/util';
import { ChartComponent } from '../../shared/chart';
import { PatientBanner } from '../../shared/patient-banner';
import { LabelPipe, StatusChip } from '../../shared/status-chip';
import { Ui } from '../../shared/ui.service';
import { VitalsDialog } from '../../shared/vitals-dialog';
import { StartEncounterDialog } from '../clinical/start-encounter';
import { OrderActions } from '../orders/order-actions';
import { OrderDialog } from '../orders/order-dialog';
import { HistoryDialog, HistoryKind } from './history-dialog';
import { ModifyRxDialog } from './modify-rx-dialog';

@Component({
  selector: 'cc-patient-chart',
  imports: [
    RouterLink, DatePipe, FormsModule, LabelPipe, StatusChip, PatientBanner, ChartComponent, MatTabsModule, MatButtonModule,
    MatIconModule, MatMenuModule, MatProgressBarModule, MatTooltipModule, MatSlideToggleModule, MatFormFieldModule, MatSelectModule, MatInputModule,
  ],
  providers: [OrderActions],
  templateUrl: './patient-chart.html',
  styleUrl: './patient-chart.scss',
})
export class PatientChart implements OnInit {
  private readonly api = inject(Api);
  private readonly ui = inject(Ui);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  readonly orderActions = inject(OrderActions);
  readonly fileSize = fileSize;

  readonly id = input.required<string>();
  get pid(): number { return +this.id(); }

  readonly isDoctor = this.auth.hasRole('DOCTOR');
  readonly isNurse = this.auth.hasRole('NURSE');
  readonly clinical = this.auth.hasRole('DOCTOR', 'NURSE', 'PHARMACIST');
  readonly canEditHistory = this.auth.hasRole('DOCTOR', 'NURSE');
  readonly canEditDemographics = this.auth.hasRole('DOCTOR', 'RECEPTIONIST');
  readonly canSchedule = this.auth.hasRole('DOCTOR', 'NURSE', 'RECEPTIONIST');
  readonly canUpload = this.auth.hasRole('DOCTOR', 'NURSE', 'RECEPTIONIST');
  readonly canDeleteDocs = this.auth.hasRole('DOCTOR');

  readonly patient = signal<Patient | null>(null);
  readonly summary = signal<ChartSummary | null>(null);
  readonly allergies = signal<Allergy[]>([]);
  readonly problems = signal<Problem[]>([]);
  readonly immunizations = signal<Immunization[]>([]);
  readonly family = signal<FamilyHistory[]>([]);
  readonly encounters = signal<Encounter[]>([]);
  readonly vitals = signal<Vital[]>([]);
  readonly orders = signal<Order[]>([]);
  readonly results = signal<LabResult[]>([]);
  readonly meds = signal<Prescription[]>([]);
  readonly mar = signal<Administration[]>([]);
  readonly documents = signal<PatientDocument[]>([]);
  readonly appointments = signal<Appointment[]>([]);
  readonly activeMedsOnly = signal(false);
  readonly uploading = signal(false);
  uploadCategory = 'LAB_REPORT';
  uploadDescription = '';

  readonly activeAllergies = computed(() => this.clinical ? this.allergies().filter(a => a.status === 'ACTIVE') : null);
  readonly openEncounters = computed(() => this.encounters().filter(e => e.status === 'OPEN'));
  readonly shownMeds = computed(() => this.activeMedsOnly() ? this.meds().filter(m => m.currentlyActive) : this.meds());

  readonly vitalsChart = computed<ChartConfiguration | null>(() => {
    const list = [...this.vitals()].reverse();
    if (list.length < 1) return null;
    const labels = list.map(v => new Date(v.recordedAt).toLocaleDateString('en-GB', { day: '2-digit', month: 'short' }));
    const ds = (label: string, data: (number | undefined)[], color: string, axis = 'y') => ({
      label, data: data.map(x => x ?? null), borderColor: color, backgroundColor: color, tension: .3, spanGaps: true, yAxisID: axis, pointRadius: 4,
    });
    return {
      type: 'line',
      data: {
        labels,
        datasets: [
          ds('Systolic', list.map(v => v.systolic), '#c62828'),
          ds('Diastolic', list.map(v => v.diastolic), '#f29d38'),
          ds('Pulse', list.map(v => v.pulse), '#0b5cad'),
          ds('SpO2 %', list.map(v => v.spo2), '#0e9f8e'),
          ds('Weight kg', list.map(v => v.weightKg), '#7e57c2', 'y2'),
        ],
      },
      options: {
        interaction: { mode: 'index', intersect: false },
        scales: { y: { title: { display: true, text: 'mmHg / bpm / %' } }, y2: { position: 'right', grid: { drawOnChartArea: false }, title: { display: true, text: 'kg' } } },
      },
    } as ChartConfiguration;
  });

  ngOnInit(): void {
    this.api.patient(this.pid).subscribe({ next: p => this.patient.set(p), error: err => this.ui.error(err) });
    if (this.clinical) this.loadClinical();
    if (this.canSchedule) this.loadAppointments();
  }

  loadClinical(): void {
    const id = this.pid;
    this.api.chartSummary(id).subscribe(s => this.summary.set(s));
    this.loadHistory();
    this.api.patientEncounters(id, 0, 50).subscribe(p => this.encounters.set(p.content));
    this.api.vitals(id).subscribe(v => this.vitals.set(v));
    this.loadOrders();
    this.loadMeds();
    this.loadDocuments();
  }

  loadHistory(): void {
    const id = this.pid;
    this.api.allergies(id).subscribe(a => this.allergies.set(a));
    this.api.problems(id).subscribe(p => this.problems.set(p));
    this.api.immunizations(id).subscribe(i => this.immunizations.set(i));
    this.api.familyHistory(id).subscribe(f => this.family.set(f));
  }

  loadOrders(): void {
    this.api.patientOrders(this.pid).subscribe(o => this.orders.set(o));
    this.api.labResults(this.pid).subscribe(r => this.results.set(r));
  }

  loadMeds(): void {
    this.api.patientMedications(this.pid).subscribe(m => this.meds.set(m));
    this.api.marHistory(this.pid).subscribe(m => this.mar.set(m));
    this.api.chartSummary(this.pid).subscribe(s => this.summary.set(s));
  }

  loadDocuments(): void {
    this.api.documents(this.pid).subscribe(d => this.documents.set(d));
  }

  loadAppointments(): void {
    this.api.patientAppointments(this.pid).subscribe({ next: a => this.appointments.set(a), error: () => undefined });
  }

  readonly reloadOrders = () => this.loadOrders();

  // ---------- actions ----------
  editHistory(kind: HistoryKind, record?: unknown): void {
    this.dialog.open(HistoryDialog, { data: { kind, patientId: this.pid, record }, width: '620px', maxWidth: '96vw' })
      .afterClosed().subscribe(ok => {
        if (ok) {
          this.loadHistory();
          this.api.chartSummary(this.pid).subscribe(s => this.summary.set(s));
        }
      });
  }

  startEncounter(): void {
    const p = this.patient()!;
    this.dialog.open(StartEncounterDialog, { data: { patient: p }, width: '560px', maxWidth: '96vw' })
      .afterClosed().subscribe((e?: Encounter) => e && this.router.navigate(['/encounters', e.id]));
  }

  recordVitals(): void {
    const p = this.patient()!;
    const enc = this.openEncounters()[0];
    this.dialog.open(VitalsDialog, { data: { patientId: p.id, patientName: p.fullName, encounterId: enc?.id }, width: '720px', maxWidth: '96vw' })
      .afterClosed().subscribe(v => {
        if (v) {
          this.api.vitals(this.pid).subscribe(list => this.vitals.set(list));
          this.api.chartSummary(this.pid).subscribe(s => this.summary.set(s));
        }
      });
  }

  newOrder(encounter: Encounter): void {
    const p = this.patient()!;
    this.dialog.open(OrderDialog, { data: { patientId: p.id, patientName: p.fullName, encounterId: encounter.id }, maxWidth: '96vw' })
      .afterClosed().subscribe(o => {
        if (o) {
          this.loadOrders();
          this.loadMeds();
        }
      });
  }

  async discontinue(rx: Prescription): Promise<void> {
    const reason = await this.ui.reason({
      title: `Discontinue ${rx.drugName}?`, message: `${rx.rxNo}: ${rx.dose} ${rx.frequency}. The pharmacy and nurses will stop this medicine.`,
      confirmText: 'Discontinue', danger: true,
    });
    if (reason === null) return;
    this.api.discontinueRx(rx.id, reason).subscribe({
      next: () => {
        this.ui.success(`${rx.drugName} discontinued.`);
        this.loadMeds();
      },
      error: err => this.ui.error(err),
    });
  }

  modify(rx: Prescription): void {
    this.dialog.open(ModifyRxDialog, { data: rx, width: '620px', maxWidth: '96vw' }).afterClosed().subscribe(ok => ok && this.loadMeds());
  }

  onFile(event: Event): void {
    const inputEl = event.target as HTMLInputElement;
    const file = inputEl.files?.[0];
    inputEl.value = '';
    if (!file) return;
    if (file.size > 10 * 1024 * 1024) {
      this.ui.error(new Error('Files must be 10 MB or smaller.'));
      return;
    }
    this.uploading.set(true);
    this.api.uploadDocument(this.pid, file, this.uploadCategory, this.uploadDescription.trim()).subscribe({
      next: () => {
        this.uploading.set(false);
        this.uploadDescription = '';
        this.ui.success(`${file.name} uploaded.`);
        this.loadDocuments();
      },
      error: err => {
        this.uploading.set(false);
        this.ui.error(err);
      },
    });
  }

  download(d: PatientDocument): void {
    this.api.download(`/api/documents/${d.id}/download`, d.fileName).catch(err => this.ui.error(err));
  }

  async deleteDocument(d: PatientDocument): Promise<void> {
    if (!(await this.ui.confirm({ title: 'Delete document?', message: `${d.fileName} will be removed from the chart.`, confirmText: 'Delete', danger: true }))) return;
    this.api.deleteDocument(d.id).subscribe({ next: () => this.loadDocuments(), error: err => this.ui.error(err) });
  }

  bookAppointment(): void {
    this.router.navigate(['/appointments'], { queryParams: { patientId: this.pid } });
  }
}
