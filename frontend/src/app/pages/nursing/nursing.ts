import { DatePipe } from '@angular/common';
import { Component, inject, input, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';
import { Api } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Administration, Encounter, Patient, Prescription, ROUTES } from '../../core/models';
import { PatientPicker } from '../../shared/patient-picker';
import { LabelPipe, StatusChip } from '../../shared/status-chip';
import { Ui } from '../../shared/ui.service';
import { VitalsDialog } from '../../shared/vitals-dialog';

/** Medication administration record: nurses document each dose of verified/dispensed medicines. */
@Component({
  selector: 'cc-nursing',
  imports: [FormsModule, RouterLink, DatePipe, LabelPipe, StatusChip, PatientPicker, MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div><h1>Medication administration (MAR)</h1><p>Choose a patient, then document each dose as given, held or refused.</p></div>
      </div>
      <div class="layout">
        <aside class="card">
          <cc-patient-picker (selected)="select($event.id)" />
          <h4>Patients with open encounters</h4>
          @for (e of openEncounters(); track e.id) {
            <button class="pt" [class.sel]="patient()?.id === e.patientId" (click)="select(e.patientId)">
              <strong>{{ e.patientName }}</strong><span class="muted small">{{ e.patientMrn }} &middot; {{ e.chiefComplaint }}</span>
            </button>
          } @empty { <p class="muted small">No open encounters.</p> }
        </aside>

        <section>
          @if (patient(); as p) {
            <div class="card">
              <div class="card-title">
                <span><a [routerLink]="['/patients', p.id]">{{ p.fullName }}</a>&nbsp;<span class="muted small">{{ p.mrn }} &middot; {{ p.age }}y</span></span>
                <button mat-stroked-button (click)="vitals(p)"><mat-icon>monitor_heart</mat-icon>Record vitals</button>
              </div>
              <h4>Medicines to administer</h4>
              @for (rx of due(); track rx.id) {
                <div class="dose">
                  <div class="grow">
                    <strong>{{ rx.drugName }}</strong>&nbsp;<span class="muted small">{{ rx.genericName }}</span>
                    <div>{{ rx.dose }} &middot; {{ rx.route | label }} &middot; <strong>{{ rx.frequency }}</strong>&nbsp;<cc-status [value]="rx.status" /></div>
                    @if (rx.instructions) { <div class="muted small">{{ rx.instructions }}</div> }
                  </div>
                  @if (canGive) {
                    <div class="actions">
                      <button mat-flat-button (click)="begin(rx, 'GIVEN')"><mat-icon>check</mat-icon>Given</button>
                      <button mat-stroked-button (click)="begin(rx, 'HELD')">Held</button>
                      <button mat-stroked-button (click)="begin(rx, 'REFUSED')">Refused</button>
                    </div>
                  }
                  @if (form?.rx?.id === rx.id) {
                    <div class="form full">
                      <mat-form-field subscriptSizing="dynamic"><mat-label>Dose given</mat-label><input matInput [(ngModel)]="form!.doseGiven" /></mat-form-field>
                      <mat-form-field subscriptSizing="dynamic"><mat-label>Route</mat-label>
                        <mat-select [(ngModel)]="form!.route">@for (r of routes; track r) { <mat-option [value]="r">{{ r | label }}</mat-option> }</mat-select></mat-form-field>
                      <mat-form-field subscriptSizing="dynamic" class="grow"><mat-label>{{ form!.status === 'GIVEN' ? 'Notes' : 'Reason (required)' }}</mat-label><input matInput [(ngModel)]="form!.notes" /></mat-form-field>
                      <button mat-flat-button (click)="save()"><mat-icon>save</mat-icon>Save</button>
                      <button mat-button (click)="form = null">Cancel</button>
                    </div>
                  }
                </div>
              } @empty { <p class="muted">No verified medicines are due for this patient.</p> }

              <h4>Administration history</h4>
              <table class="simple">
                <tr><th>Time</th><th>Medicine</th><th>Dose</th><th>Status</th><th>Nurse</th><th>Notes</th></tr>
                @for (a of history(); track a.id) {
                  <tr><td class="nowrap">{{ a.administeredAt | date: 'dd MMM, HH:mm' }}</td><td>{{ a.drugName }}</td><td>{{ a.doseGiven }} {{ a.route | label }}</td>
                    <td><cc-status [value]="a.status" /></td><td>{{ a.administeredByName }}</td><td>{{ a.notes }}</td></tr>
                } @empty { <tr><td colspan="6" class="muted">Nothing documented yet.</td></tr> }
              </table>
            </div>
          } @else {
            <div class="card empty"><mat-icon>medication</mat-icon>Select a patient to start the medication round.</div>
          }
        </section>
      </div>
    </div>
  `,
  styles: `
    .layout { display: grid; grid-template-columns: 320px 1fr; gap: 16px; align-items: start; }
    h4 { margin: 18px 0 8px; color: var(--cc-primary-dark); }
    .pt { display: flex; flex-direction: column; width: 100%; text-align: left; padding: 10px; border: 1px solid transparent; border-radius: 10px; background: none; cursor: pointer; font: inherit; color: inherit;
      &:hover { background: #f2f7fd; } &.sel { background: #e8f1fc; border-color: #9cc3ec; } }
    .dose { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; padding: 12px; border: 1px solid var(--cc-border); border-radius: 12px; margin-bottom: 10px; }
    .grow { flex: 1; min-width: 200px; }
    .form { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; width: 100%; padding-top: 10px; border-top: 1px dashed var(--cc-border);
      mat-form-field { width: 160px; } .grow { flex: 1; } }
    @media (max-width: 900px) { .layout { grid-template-columns: 1fr; } }
  `,
})
export class Nursing implements OnInit {
  private readonly api = inject(Api);
  private readonly ui = inject(Ui);
  private readonly dialog = inject(MatDialog);
  readonly canGive = inject(AuthService).hasRole('NURSE');
  readonly patientId = input<string>();
  readonly routes = ROUTES;
  readonly openEncounters = signal<Encounter[]>([]);
  readonly patient = signal<Patient | null>(null);
  readonly due = signal<Prescription[]>([]);
  readonly history = signal<Administration[]>([]);
  form: { rx: Prescription; status: 'GIVEN' | 'HELD' | 'REFUSED'; doseGiven: string; route: string; notes: string } | null = null;

  ngOnInit(): void {
    this.api.encounterWorklist('OPEN').subscribe(list => {
      const seen = new Set<number>();
      this.openEncounters.set(list.filter(e => !seen.has(e.patientId) && seen.add(e.patientId)));
    });
    const pid = this.patientId();
    if (pid) this.select(+pid);
  }

  select(id: number): void {
    this.form = null;
    this.api.patient(id).subscribe(p => this.patient.set(p));
    this.refresh(id);
  }

  refresh(id: number): void {
    this.api.marDue(id).subscribe(d => this.due.set(d));
    this.api.marHistory(id).subscribe(h => this.history.set(h));
  }

  begin(rx: Prescription, status: 'GIVEN' | 'HELD' | 'REFUSED'): void {
    this.form = { rx, status, doseGiven: rx.dose, route: rx.route, notes: '' };
  }

  save(): void {
    const f = this.form;
    if (!f) return;
    if (f.status !== 'GIVEN' && !f.notes.trim()) {
      this.ui.error(new Error('Please give a reason for a held or refused dose.'));
      return;
    }
    this.api.recordMar({ prescriptionId: f.rx.id, status: f.status, doseGiven: f.doseGiven, route: f.route, notes: f.notes || null }).subscribe({
      next: () => {
        this.ui.success(`${f.rx.drugName}: ${f.status.toLowerCase()} recorded.`);
        this.form = null;
        this.refresh(this.patient()!.id);
      },
      error: err => this.ui.error(err),
    });
  }

  vitals(p: Patient): void {
    const enc = this.openEncounters().find(e => e.patientId === p.id);
    this.dialog.open(VitalsDialog, { data: { patientId: p.id, patientName: p.fullName, encounterId: enc?.id }, width: '720px', maxWidth: '96vw' });
  }
}

