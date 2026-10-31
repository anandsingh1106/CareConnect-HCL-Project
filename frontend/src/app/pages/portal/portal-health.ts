import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ChartConfiguration } from 'chart.js';
import { Api } from '../../core/api.service';
import { Allergy, LabResult, Patient, Prescription, Problem, VisitSummary, Vital } from '../../core/models';
import { ChartComponent } from '../../shared/chart';
import { LabelPipe, StatusChip } from '../../shared/status-chip';
import { Ui } from '../../shared/ui.service';

/** The patient's own record: medicines, results, visit summaries (with PDF), allergies and vitals. */
@Component({
  selector: 'cc-portal-health',
  imports: [DatePipe, LabelPipe, StatusChip, ChartComponent, MatTabsModule, MatButtonModule, MatIconModule, MatExpansionModule, MatTooltipModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div><h1>My health record</h1>
          @if (profile(); as p) { <p>{{ p.fullName }} &middot; MRN <span class="mono">{{ p.mrn }}</span> &middot; {{ p.age }} yrs @if (p.bloodGroup) { &middot; {{ p.bloodGroup }} }</p> }</div>
      </div>

      <mat-tab-group mat-stretch-tabs="false">
        <mat-tab label="Medicines">
          <div class="tab">
            @for (m of meds(); track m.id) {
              <div class="card med" [class.off]="!m.currentlyActive">
                <mat-icon class="big">medication</mat-icon>
                <div class="grow">
                  <strong>{{ m.drugName }}</strong>&nbsp;<span class="muted small">{{ m.genericName }}</span>
                  <div>{{ m.dose }} &middot; {{ m.route | label }} &middot; {{ m.frequency }} for {{ m.durationDays }} days</div>
                  @if (m.instructions) { <div class="muted small">{{ m.instructions }}</div> }
                  <div class="muted small">Prescribed by {{ m.prescriberName }} on {{ m.createdAt | date: 'dd MMM yyyy' }}</div>
                </div>
                <cc-status [value]="m.status" />
                <button mat-icon-button matTooltip="Download prescription PDF" (click)="rxPdf(m)"><mat-icon>picture_as_pdf</mat-icon></button>
              </div>
            } @empty { <div class="card empty"><mat-icon>medication</mat-icon>No medicines on record.</div> }
          </div>
        </mat-tab>

        <mat-tab label="Lab results">
          <div class="tab table-wrap">
            <table class="simple">
              <tr><th>Test</th><th>Result</th><th>Normal range</th><th>Flag</th><th>Date</th></tr>
              @for (r of results(); track r.id) {
                <tr><td><strong>{{ r.testName }}</strong></td><td>{{ r.valueNumeric ?? '' }} {{ r.unit }}@if (r.valueText) { <div class="small">{{ r.valueText }}</div> }</td>
                  <td>{{ r.referenceRange }}</td><td><cc-status [value]="r.flag" /></td><td>{{ r.resultedAt | date: 'dd MMM yyyy' }}</td></tr>
              } @empty { <tr><td colspan="5" class="muted">No results yet.</td></tr> }
            </table>
            <p class="muted small">Results marked High, Low or Critical are reviewed by your doctor. Please message them if you have questions.</p>
          </div>
        </mat-tab>

        <mat-tab label="Visit summaries">
          <div class="tab">
            <mat-accordion>
              @for (v of visits(); track v.encounterId) {
                <mat-expansion-panel>
                  <mat-expansion-panel-header>
                    <mat-panel-title>{{ v.encounterDate | date: 'dd MMM yyyy' }} &middot; {{ v.providerName }}</mat-panel-title>
                    <mat-panel-description>{{ v.chiefComplaint }}</mat-panel-description>
                  </mat-expansion-panel-header>
                  <dl class="kv">
                    <dt>Department</dt><dd>{{ v.departmentName }} &middot; {{ v.encounterType | label }}</dd>
                    @if (v.diagnoses.length) { <dt>Diagnosis</dt><dd>{{ v.diagnoses.join(', ') }}</dd> }
                    @if (v.assessment) { <dt>Assessment</dt><dd class="pre">{{ v.assessment }}</dd> }
                    @if (v.plan) { <dt>Plan</dt><dd class="pre">{{ v.plan }}</dd> }
                    @if (v.medications.length) { <dt>Medicines</dt><dd>{{ v.medications.join('; ') }}</dd> }
                    @if (v.vitals; as vt) { <dt>Vitals</dt><dd>BP {{ vt.systolic }}/{{ vt.diastolic }} &middot; pulse {{ vt.pulse }} &middot; SpO2 {{ vt.spo2 }}%</dd> }
                  </dl>
                  <button mat-stroked-button class="mt" (click)="visitPdf(v)"><mat-icon>picture_as_pdf</mat-icon>Download summary PDF</button>
                </mat-expansion-panel>
              } @empty { <div class="card empty"><mat-icon>clinical_notes</mat-icon>No visits yet.</div> }
            </mat-accordion>
          </div>
        </mat-tab>

        <mat-tab label="Allergies & conditions">
          <div class="tab grid grid-2">
            <section class="card">
              <h3 class="card-title">Allergies <mat-icon>warning</mat-icon></h3>
              @for (a of allergies(); track a.id) {
                <div class="line"><strong>{{ a.allergen }}</strong>&nbsp;<span class="muted">{{ a.reaction }}</span> <cc-status [value]="a.severity" /></div>
              } @empty { <p class="muted">No known allergies.</p> }
            </section>
            <section class="card">
              <h3 class="card-title">Health conditions <mat-icon>list_alt</mat-icon></h3>
              @for (p of problems(); track p.id) {
                <div class="line"><strong>{{ p.description }}</strong>&nbsp;<cc-status [value]="p.status" /></div>
              } @empty { <p class="muted">No conditions recorded.</p> }
            </section>
          </div>
        </mat-tab>

        <mat-tab label="Vitals">
          <div class="tab card">
            @if (chart(); as cfg) { <cc-chart [config]="cfg" height="300px" /> } @else { <div class="empty">No vitals yet.</div> }
          </div>
        </mat-tab>

        <mat-tab label="Profile">
          @if (profile(); as p) {
            <div class="tab card">
              <dl class="kv">
                <dt>Name</dt><dd>{{ p.fullName }}</dd><dt>Date of birth</dt><dd>{{ p.dateOfBirth | date: 'dd MMM yyyy' }}</dd>
                <dt>Phone</dt><dd>{{ p.phone }}</dd><dt>Email</dt><dd>{{ p.email || '-' }}</dd>
                <dt>Address</dt><dd>{{ p.addressLine }} {{ p.city }} {{ p.stateName }} {{ p.postalCode }}</dd>
                <dt>Emergency contact</dt><dd>{{ p.emergencyName || '-' }} {{ p.emergencyPhone ? '(' + p.emergencyPhone + ')' : '' }}</dd>
                <dt>Insurance</dt><dd>{{ p.insuranceProvider || '-' }} {{ p.insurancePolicyNo }}</dd>
                <dt>Primary doctor</dt><dd>{{ p.primaryProviderName || '-' }}</dd>
              </dl>
              <p class="muted small mt">To change these details please contact the reception desk.</p>
            </div>
          }
        </mat-tab>
      </mat-tab-group>
    </div>
  `,
  styles: `
    .tab { padding: 18px 2px; display: grid; gap: 12px; }
    .med { display: flex; align-items: center; gap: 14px; flex-wrap: wrap; &.off { opacity: .6; } }
    .big { color: var(--cc-teal); font-size: 32px; width: 32px; height: 32px; }
    .grow { flex: 1; min-width: 200px; }
    .line { padding: 8px 0; border-bottom: 1px solid var(--cc-border); display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
  `,
})
export class PortalHealth {
  private readonly api = inject(Api);
  private readonly ui = inject(Ui);
  readonly profile = signal<Patient | null>(null);
  readonly meds = signal<Prescription[]>([]);
  readonly results = signal<LabResult[]>([]);
  readonly visits = signal<VisitSummary[]>([]);
  readonly allergies = signal<Allergy[]>([]);
  readonly problems = signal<Problem[]>([]);
  readonly vitals = signal<Vital[]>([]);

  readonly chart = computed<ChartConfiguration | null>(() => {
    const list = [...this.vitals()].reverse();
    if (!list.length) return null;
    return {
      type: 'line',
      data: {
        labels: list.map(v => new Date(v.recordedAt).toLocaleDateString('en-GB', { day: '2-digit', month: 'short' })),
        datasets: [
          { label: 'Systolic BP', data: list.map(v => v.systolic ?? null), borderColor: '#c62828', backgroundColor: '#c62828', tension: .3 },
          { label: 'Diastolic BP', data: list.map(v => v.diastolic ?? null), borderColor: '#f29d38', backgroundColor: '#f29d38', tension: .3 },
          { label: 'Pulse', data: list.map(v => v.pulse ?? null), borderColor: '#0b5cad', backgroundColor: '#0b5cad', tension: .3 },
        ],
      },
    };
  });

  constructor() {
    const err = (e: unknown) => this.ui.error(e);
    this.api.portalProfile().subscribe({ next: p => this.profile.set(p), error: err });
    this.api.portalMedications().subscribe(m => this.meds.set(m));
    this.api.portalLabResults().subscribe(r => this.results.set(r));
    this.api.portalVisits().subscribe(v => this.visits.set(v));
    this.api.portalAllergies().subscribe(a => this.allergies.set(a));
    this.api.portalProblems().subscribe(p => this.problems.set(p));
    this.api.portalVitals().subscribe(v => this.vitals.set(v));
  }

  visitPdf(v: VisitSummary): void {
    this.api.download(`/api/portal/visits/${v.encounterId}/pdf`, `visit-${v.encounterId}.pdf`).catch(e => this.ui.error(e));
  }

  rxPdf(m: Prescription): void {
    this.api.download(`/api/portal/prescriptions/${m.id}/pdf`, `${m.rxNo}.pdf`).catch(e => this.ui.error(e));
  }
}
