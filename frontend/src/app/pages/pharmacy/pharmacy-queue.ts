import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { RouterLink } from '@angular/router';
import { Api } from '../../core/api.service';
import { Prescription, PrescriptionDetail, PrescriptionStatus } from '../../core/models';
import { LabelPipe, StatusChip } from '../../shared/status-chip';
import { Ui } from '../../shared/ui.service';

const TABS: { status: PrescriptionStatus; label: string; icon: string }[] = [
  { status: 'PENDING_VERIFICATION', label: 'To verify', icon: 'fact_check' },
  { status: 'VERIFIED', label: 'To dispense', icon: 'local_pharmacy' },
  { status: 'DISPENSED', label: 'Dispensed', icon: 'task_alt' },
  { status: 'REJECTED', label: 'Rejected', icon: 'block' },
];

/** Pharmacist workflow: review the e-prescription with its CDS alerts, verify or reject, then dispense from stock. */
@Component({
  selector: 'cc-pharmacy-queue',
  imports: [FormsModule, RouterLink, DatePipe, LabelPipe, StatusChip, MatTabsModule, MatButtonModule, MatIconModule, MatProgressBarModule, MatFormFieldModule, MatInputModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div><h1>Pharmacy</h1><p>Verify e-prescriptions, then dispense. Stock is deducted automatically.</p></div>
        <a mat-stroked-button routerLink="/pharmacy/inventory"><mat-icon>inventory_2</mat-icon>Inventory</a>
      </div>
      <mat-tab-group mat-stretch-tabs="false" [selectedIndex]="tabIndex()" (selectedIndexChange)="selectTab($event)">
        @for (t of tabs; track t.status) {
          <mat-tab><ng-template mat-tab-label><mat-icon class="ti">{{ t.icon }}</mat-icon>{{ t.label }}
            @if (counts()[t.status]) { <span class="chip primary cnt">{{ counts()[t.status] }}</span> }</ng-template></mat-tab>
        }
      </mat-tab-group>

      <div class="layout">
        <section class="list card">
          @if (loading()) { <mat-progress-bar mode="indeterminate" /> }
          @for (rx of rows(); track rx.id) {
            <button class="item" [class.sel]="selected()?.prescription?.id === rx.id" (click)="open(rx)">
              <div class="row1"><strong>{{ rx.drugName }}</strong><span class="mono small">{{ rx.rxNo }}</span></div>
              <div class="muted small">{{ rx.patientName }} &middot; {{ rx.patientMrn }}</div>
              <div class="muted small">{{ rx.dose }} {{ rx.frequency }} &times; {{ rx.durationDays }}d &middot; qty {{ rx.quantity }} &middot; {{ rx.createdAt | date: 'dd MMM HH:mm' }}</div>
            </button>
          } @empty {
            @if (!loading()) { <div class="empty"><mat-icon>inbox</mat-icon>Queue is empty.</div> }
          }
        </section>

        <section class="detail">
          @if (selected(); as d) {
            @let p = d.prescription;
            <div class="card">
              <div class="card-title"><span>{{ p.drugName }} <span class="muted small">{{ p.genericName }}</span></span><cc-status [value]="p.status" /></div>
              <dl class="kv">
                <dt>Prescription</dt><dd class="mono">{{ p.rxNo }} ({{ p.orderNo }})</dd>
                <dt>Patient</dt><dd><a [routerLink]="['/patients', p.patientId]">{{ p.patientName }}</a> &middot; {{ p.patientMrn }}</dd>
                <dt>Prescriber</dt><dd>{{ p.prescriberName }} &middot; {{ p.createdAt | date: 'dd MMM yyyy, HH:mm' }}</dd>
                <dt>Sig</dt><dd>{{ p.dose }} &middot; {{ p.route | label }} &middot; {{ p.frequency }} for {{ p.durationDays }} days</dd>
                <dt>Quantity</dt><dd>{{ p.quantity }} &middot; stock available <strong [class.low]="(p.drugStock ?? 0) < (p.quantity ?? 0)">{{ p.drugStock }}</strong></dd>
                @if (p.instructions) { <dt>Instructions</dt><dd>{{ p.instructions }}</dd> }
                @if (p.pharmacistNote) { <dt>Pharmacist note</dt><dd>{{ p.pharmacistNote }}</dd> }
                @if (p.verifiedByName) { <dt>Verified</dt><dd>{{ p.verifiedByName }} &middot; {{ p.verifiedAt | date: 'dd MMM, HH:mm' }}</dd> }
                @if (p.dispensedByName) { <dt>Dispensed</dt><dd>{{ p.dispensedQty }} by {{ p.dispensedByName }} &middot; {{ p.dispensedAt | date: 'dd MMM, HH:mm' }}</dd> }
              </dl>

              <h4>Patient allergies</h4>
              <div>
                @for (a of d.patientAllergies; track a) { <span class="chip danger">{{ a }}</span>&nbsp; } @empty { <span class="muted">No known allergies</span> }
              </div>

              <h4>Decision-support alerts</h4>
              @for (a of d.alerts; track a.id) {
                <div class="alert-box" [class]="'alert-box ' + a.severity"><mat-icon>warning</mat-icon>
                  <div><strong>{{ a.alertType | label }}</strong> - {{ a.message }}@if (a.overridden) { <em> (overridden by prescriber)</em> }</div></div>
              } @empty { <p class="muted">No alerts were raised for this order.</p> }

              @if (p.status === 'PENDING_VERIFICATION') {
                <mat-form-field class="w"><mat-label>Verification note (optional)</mat-label><input matInput [(ngModel)]="note" maxlength="500" /></mat-form-field>
                <div class="actions">
                  <button mat-flat-button (click)="verify(p)"><mat-icon>verified</mat-icon>Verify</button>
                  <button mat-stroked-button class="danger" (click)="reject(p)"><mat-icon>block</mat-icon>Reject</button>
                </div>
              }
              @if (p.status === 'VERIFIED') {
                <div class="actions">
                  <mat-form-field subscriptSizing="dynamic" class="qty"><mat-label>Quantity</mat-label><input matInput type="number" [(ngModel)]="qty" min="1" /></mat-form-field>
                  <button mat-flat-button (click)="dispense(p)"><mat-icon>local_pharmacy</mat-icon>Dispense</button>
                </div>
              }
            </div>
          } @else {
            <div class="card empty"><mat-icon>touch_app</mat-icon>Select a prescription to review it.</div>
          }
        </section>
      </div>
    </div>
  `,
  styles: `
    .ti { margin-right: 6px; } .cnt { margin-left: 8px; }
    .layout { display: grid; grid-template-columns: minmax(280px, 380px) 1fr; gap: 16px; margin-top: 16px; align-items: start; }
    .list { padding: 8px; max-height: 70vh; overflow: auto; }
    .item { display: block; width: 100%; text-align: left; border: 1px solid transparent; background: none; padding: 12px; border-radius: 10px; cursor: pointer; font: inherit; color: inherit;
      &:hover { background: #f2f7fd; } &.sel { background: #e8f1fc; border-color: #9cc3ec; } }
    .row1 { display: flex; justify-content: space-between; gap: 8px; }
    h4 { margin: 18px 0 8px; color: var(--cc-primary-dark); }
    .w { width: 100%; margin-top: 12px; } .qty { width: 130px; }
    .low { color: var(--cc-danger); }
    .danger { color: var(--cc-danger); }
    @media (max-width: 860px) { .layout { grid-template-columns: 1fr; } }
  `,
})
export class PharmacyQueue {
  private readonly api = inject(Api);
  private readonly ui = inject(Ui);
  readonly tabs = TABS;
  readonly tabIndex = signal(0);
  readonly rows = signal<Prescription[]>([]);
  readonly counts = signal<Record<string, number>>({});
  readonly selected = signal<PrescriptionDetail | null>(null);
  readonly loading = signal(false);
  note = '';
  qty: number | null = null;

  constructor() {
    this.load();
    this.refreshCounts();
  }

  selectTab(i: number): void {
    this.tabIndex.set(i);
    this.selected.set(null);
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.api.prescriptions({ status: TABS[this.tabIndex()].status, size: 50 }).subscribe({
      next: page => {
        this.rows.set(page.content);
        this.loading.set(false);
      },
      error: err => {
        this.loading.set(false);
        this.ui.error(err);
      },
    });
  }

  refreshCounts(): void {
    for (const s of ['PENDING_VERIFICATION', 'VERIFIED'] as PrescriptionStatus[]) {
      this.api.prescriptions({ status: s, size: 1 }).subscribe(p => this.counts.update(c => ({ ...c, [s]: p.totalElements })));
    }
  }

  open(rx: Prescription): void {
    this.api.prescription(rx.id).subscribe(d => {
      this.selected.set(d);
      this.note = '';
      this.qty = d.prescription.quantity ?? null;
    });
  }

  private after(message: string): void {
    this.ui.success(message);
    this.selected.set(null);
    this.load();
    this.refreshCounts();
  }

  async verify(p: Prescription): Promise<void> {
    const ok = await this.ui.confirm({ title: `Verify ${p.rxNo}?`, message: `${p.drugName} ${p.dose} ${p.frequency} for ${p.patientName} will be released for dispensing and administration.`, confirmText: 'Verify' });
    if (!ok) return;
    this.api.verifyRx(p.id, this.note || undefined).subscribe({ next: () => this.after(`${p.rxNo} verified.`), error: err => this.ui.error(err) });
  }

  async reject(p: Prescription): Promise<void> {
    const reason = await this.ui.reason({ title: `Reject ${p.rxNo}?`, message: 'The prescriber will be notified with your reason.', confirmText: 'Reject', danger: true });
    if (reason === null) return;
    this.api.rejectRx(p.id, reason).subscribe({ next: () => this.after(`${p.rxNo} rejected.`), error: err => this.ui.error(err) });
  }

  async dispense(p: Prescription): Promise<void> {
    const qty = this.qty ?? p.quantity ?? 0;
    const ok = await this.ui.confirm({ title: `Dispense ${p.drugName}?`, message: `${qty} units will be deducted from stock (${p.drugStock} available).`, confirmText: 'Dispense' });
    if (!ok) return;
    this.api.dispenseRx(p.id, qty).subscribe({ next: () => this.after(`${p.rxNo} dispensed.`), error: err => this.ui.error(err) });
  }
}
