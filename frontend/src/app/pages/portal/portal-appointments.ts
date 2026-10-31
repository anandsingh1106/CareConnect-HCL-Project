import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Api } from '../../core/api.service';
import { Appointment, Provider, Slot } from '../../core/models';
import { addDays, isoDate } from '../../core/util';
import { LabelPipe, StatusChip } from '../../shared/status-chip';
import { Ui } from '../../shared/ui.service';

/** Patients book a free slot with a doctor, see upcoming visits and cancel their own bookings. */
@Component({
  selector: 'cc-portal-appointments',
  imports: [FormsModule, DatePipe, LabelPipe, StatusChip, MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatDatepickerModule],
  template: `
    <div class="page">
      <div class="page-header"><div><h1>My appointments</h1><p>Book an in-person or video visit with your doctor.</p></div></div>
      <div class="layout">
        <section class="card">
          <h3 class="card-title">Book a visit <mat-icon>calendar_add_on</mat-icon></h3>
          <div class="form-grid">
            <mat-form-field class="full"><mat-label>Doctor</mat-label>
              <mat-select [(ngModel)]="providerId" (selectionChange)="loadSlots()">
                @for (p of providers(); track p.id) { <mat-option [value]="p.id">{{ p.fullName }} &middot; {{ p.specialty }}</mat-option> }
              </mat-select></mat-form-field>
            <mat-form-field><mat-label>Date</mat-label><input matInput [matDatepicker]="dp" [(ngModel)]="date" (dateChange)="loadSlots()" [min]="today" [max]="maxDate" />
              <mat-datepicker-toggle matSuffix [for]="dp" /><mat-datepicker #dp /></mat-form-field>
            <mat-form-field><mat-label>Visit type</mat-label>
              <mat-select [(ngModel)]="type"><mat-option value="IN_PERSON">In person</mat-option><mat-option value="TELEHEALTH">Video (telehealth)</mat-option></mat-select></mat-form-field>
          </div>
          <div class="slots">
            @for (s of slots(); track s.id) {
              <button class="slot" [class.sel]="slot()?.id === s.id" (click)="slot.set(s)">{{ s.slotStart | date: 'HH:mm' }}</button>
            } @empty { <p class="muted">{{ providerId ? 'No free times on this day - try another date.' : 'Choose a doctor to see free times.' }}</p> }
          </div>
          <mat-form-field class="w mt"><mat-label>Reason for visit</mat-label><input matInput [(ngModel)]="reason" maxlength="500" /></mat-form-field>
          <button mat-flat-button [disabled]="!slot()" (click)="book()"><mat-icon>event_available</mat-icon>{{ slot() ? 'Book ' + (slot()!.slotStart | date: 'EEE dd MMM, HH:mm') : 'Pick a time' }}</button>
        </section>

        <section class="card">
          <h3 class="card-title">Upcoming</h3>
          @for (a of upcoming(); track a.id) {
            <div class="appt">
              <div class="date"><b>{{ a.startTime | date: 'dd' }}</b><small>{{ a.startTime | date: 'MMM' }}</small></div>
              <div class="grow"><strong>{{ a.startTime | date: 'EEE, HH:mm' }}</strong> &middot; {{ a.providerName }}
                <div class="muted small">{{ a.appointmentType | label }}{{ a.reason ? ' · ' + a.reason : '' }}</div></div>
              <cc-status [value]="a.status" />
              @if (a.status === 'BOOKED') { <button mat-button (click)="cancel(a)">Cancel</button> }
            </div>
          } @empty { <p class="muted">No upcoming appointments.</p> }
          <h3 class="card-title mt">Past &amp; cancelled</h3>
          @for (a of past(); track a.id) {
            <div class="appt past"><div class="grow">{{ a.startTime | date: 'dd MMM yyyy, HH:mm' }} &middot; {{ a.providerName }}</div><cc-status [value]="a.status" /></div>
          } @empty { <p class="muted">Nothing yet.</p> }
        </section>
      </div>
    </div>
  `,
  styles: `
    .layout { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; align-items: start; }
    .slots { display: grid; grid-template-columns: repeat(auto-fill, minmax(76px, 1fr)); gap: 8px; }
    .slot { padding: 10px; border-radius: 10px; border: 1px solid #b7dcc4; background: #e3f4ea; color: var(--cc-success); font: inherit; font-weight: 600; cursor: pointer;
      &.sel { background: var(--cc-primary); color: #fff; border-color: var(--cc-primary); } }
    .w { width: 100%; }
    .appt { display: flex; align-items: center; gap: 12px; padding: 10px 0; border-bottom: 1px solid var(--cc-border); flex-wrap: wrap; &.past { opacity: .7; } }
    .date { width: 46px; text-align: center; background: #e3eefa; color: var(--cc-primary); border-radius: 10px; padding: 4px;
      b { display: block; font-size: 1.2rem; } small { font-size: .7rem; text-transform: uppercase; } }
    .grow { flex: 1; min-width: 160px; }
    @media (max-width: 900px) { .layout { grid-template-columns: 1fr; } }
  `,
})
export class PortalAppointments {
  private readonly api = inject(Api);
  private readonly ui = inject(Ui);
  readonly providers = signal<Provider[]>([]);
  readonly slots = signal<Slot[]>([]);
  readonly slot = signal<Slot | null>(null);
  readonly appointments = signal<Appointment[]>([]);
  readonly upcoming = computed(() => this.appointments().filter(a => (a.status === 'BOOKED' || a.status === 'CHECKED_IN') && new Date(a.startTime) >= new Date())
    .sort((a, b) => a.startTime.localeCompare(b.startTime)));
  readonly past = computed(() => this.appointments().filter(a => !this.upcoming().includes(a)));
  readonly today = new Date();
  readonly maxDate = addDays(new Date(), 60);
  providerId: number | null = null;
  date: Date = addDays(new Date(), 1);
  type: 'IN_PERSON' | 'TELEHEALTH' = 'IN_PERSON';
  reason = '';

  constructor() {
    this.api.providers().subscribe(p => this.providers.set(p));
    this.load();
  }

  load(): void {
    this.api.portalAppointments().subscribe({ next: a => this.appointments.set(a), error: e => this.ui.error(e) });
  }

  loadSlots(): void {
    this.slot.set(null);
    if (!this.providerId || !this.date) return;
    this.api.portalSlots(this.providerId, isoDate(this.date)!).subscribe({ next: s => this.slots.set(s), error: e => this.ui.error(e) });
  }

  async book(): Promise<void> {
    const s = this.slot();
    if (!s) return;
    const ok = await this.ui.confirm({ title: 'Confirm booking', message: `${s.providerName}, ${new Date(s.slotStart).toLocaleString('en-GB', { dateStyle: 'full', timeStyle: 'short' })}`, confirmText: 'Book' });
    if (!ok) return;
    this.api.portalBook({ slotId: s.id, appointmentType: this.type, reason: this.reason || null }).subscribe({
      next: () => {
        this.ui.success('Appointment booked. You will get a reminder notification.');
        this.reason = '';
        this.load();
        this.loadSlots();
      },
      error: e => this.ui.error(e),
    });
  }

  async cancel(a: Appointment): Promise<void> {
    const reason = await this.ui.reason({ title: 'Cancel appointment?', message: `${a.providerName} on ${new Date(a.startTime).toLocaleString('en-GB')}`, confirmText: 'Cancel appointment', danger: true });
    if (reason === null) return;
    this.api.portalCancel(a.id, reason).subscribe({ next: () => { this.ui.success('Appointment cancelled.'); this.load(); }, error: e => this.ui.error(e) });
  }
}
