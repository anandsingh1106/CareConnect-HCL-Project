import { DatePipe } from '@angular/common';
import { Component, computed, inject, input, OnInit, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialog, MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router, RouterLink } from '@angular/router';
import { Api } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Appointment, Encounter, PatientSummary, Provider, Slot } from '../../core/models';
import { addDays, isoDate } from '../../core/util';
import { PatientPicker } from '../../shared/patient-picker';
import { LabelPipe, StatusChip } from '../../shared/status-chip';
import { Ui } from '../../shared/ui.service';
import { StartEncounterDialog } from '../clinical/start-encounter';

/** Creates a block of equal slots for a doctor over a date range. */
@Component({
  selector: 'cc-generate-slots',
  imports: [ReactiveFormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatDatepickerModule],
  template: `
    <h2 mat-dialog-title>Generate appointment slots</h2>
    <form [formGroup]="form" (ngSubmit)="save()">
      <mat-dialog-content>
        <div class="form-grid">
          <mat-form-field class="full"><mat-label>Doctor</mat-label>
            <mat-select formControlName="providerId">@for (p of data.providers; track p.id) { <mat-option [value]="p.id">{{ p.fullName }}</mat-option> }</mat-select></mat-form-field>
          <mat-form-field><mat-label>From</mat-label><input matInput [matDatepicker]="f" formControlName="date" [min]="today" /><mat-datepicker-toggle matSuffix [for]="f" /><mat-datepicker #f /></mat-form-field>
          <mat-form-field><mat-label>Until</mat-label><input matInput [matDatepicker]="u" formControlName="untilDate" [min]="today" /><mat-datepicker-toggle matSuffix [for]="u" /><mat-datepicker #u /></mat-form-field>
          <mat-form-field><mat-label>Start time</mat-label><input matInput type="time" formControlName="startTime" /></mat-form-field>
          <mat-form-field><mat-label>End time</mat-label><input matInput type="time" formControlName="endTime" /></mat-form-field>
          <mat-form-field><mat-label>Slot length</mat-label>
            <mat-select formControlName="slotMinutes">@for (m of [10, 15, 20, 30, 45, 60]; track m) { <mat-option [value]="m">{{ m }} minutes</mat-option> }</mat-select></mat-form-field>
        </div>
        <p class="muted small">Sundays are skipped. Existing slots at the same time are left unchanged.</p>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancel</button>
        <button mat-flat-button type="submit">Generate</button>
      </mat-dialog-actions>
    </form>
  `,
})
export class GenerateSlotsDialog {
  readonly data = inject<{ providers: Provider[]; providerId?: number }>(MAT_DIALOG_DATA);
  private readonly api = inject(Api);
  private readonly ui = inject(Ui);
  private readonly ref = inject(MatDialogRef<GenerateSlotsDialog, boolean>);
  readonly today = new Date();
  readonly form = inject(FormBuilder).group({
    providerId: [this.data.providerId ?? null, Validators.required],
    date: [new Date(), Validators.required],
    untilDate: [addDays(new Date(), 6)],
    startTime: ['09:00', Validators.required],
    endTime: ['13:00', Validators.required],
    slotMinutes: [30, Validators.required],
  });

  save(): void {
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    this.api.generateSlots({ ...v, date: isoDate(v.date), untilDate: isoDate(v.untilDate) }).subscribe({
      next: r => {
        this.ui.success(`${r.created} slots created${r.skipped ? `, ${r.skipped} already existed` : ''}.`);
        this.ref.close(true);
      },
      error: err => this.ui.error(err),
    });
  }
}

@Component({
  selector: 'cc-appointments',
  imports: [
    FormsModule, RouterLink, DatePipe, LabelPipe, StatusChip, PatientPicker, MatButtonModule, MatButtonToggleModule, MatDatepickerModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatIconModule, MatMenuModule, MatTooltipModule,
  ],
  templateUrl: './appointments.html',
  styleUrl: './appointments.scss',
})
export class Appointments implements OnInit {
  private readonly api = inject(Api);
  private readonly ui = inject(Ui);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  readonly patientId = input<string>();
  readonly isDoctor = this.auth.hasRole('DOCTOR');
  readonly canManageSlots = this.auth.hasRole('RECEPTIONIST', 'ADMIN', 'DOCTOR');

  readonly providers = signal<Provider[]>([]);
  readonly providerId = signal<number | null>(null);
  readonly date = signal(new Date());
  readonly slots = signal<Slot[]>([]);
  readonly appointments = signal<Appointment[]>([]);
  readonly selectedSlot = signal<Slot | null>(null);
  readonly bookPatient = signal<PatientSummary | null>(null);
  bookType: 'IN_PERSON' | 'TELEHEALTH' = 'IN_PERSON';
  bookReason = '';

  readonly provider = computed(() => this.providers().find(p => p.id === this.providerId()) ?? null);
  readonly freeCount = computed(() => this.slots().filter(s => s.status === 'AVAILABLE' && new Date(s.slotStart) > new Date()).length);

  ngOnInit(): void {
    this.api.providers().subscribe(list => {
      this.providers.set(list);
      const me = this.auth.user();
      this.providerId.set(this.isDoctor && me ? me.id : list[0]?.id ?? null);
      this.load();
    });
    const pid = this.patientId();
    if (pid) this.api.patient(+pid).subscribe(p => this.bookPatient.set(p));
  }

  today(): Date {
    return new Date();
  }

  shift(days: number): void {
    this.date.set(addDays(this.date(), days));
    this.load();
  }

  setDate(d: Date | null): void {
    if (d) {
      this.date.set(d);
      this.load();
    }
  }

  setProvider(id: number): void {
    this.providerId.set(id);
    this.load();
  }

  load(): void {
    const pid = this.providerId();
    const day = isoDate(this.date())!;
    this.selectedSlot.set(null);
    if (pid) this.api.slots(pid, day).subscribe({ next: s => this.slots.set(s), error: err => this.ui.error(err) });
    this.api.appointments({ providerId: pid, from: day, to: day, size: 100, sort: 'startTime,asc' })
      .subscribe({ next: p => this.appointments.set(p.content), error: err => this.ui.error(err) });
  }

  isPast(s: Slot): boolean {
    return new Date(s.slotStart) < new Date();
  }

  clickSlot(s: Slot): void {
    if (s.status === 'AVAILABLE' && !this.isPast(s)) this.selectedSlot.set(s);
  }

  book(): void {
    const slot = this.selectedSlot();
    const patient = this.bookPatient();
    if (!slot || !patient) {
      this.ui.error(new Error('Choose a patient and a free slot.'));
      return;
    }
    this.api.bookAppointment({ patientId: patient.id, slotId: slot.id, appointmentType: this.bookType, reason: this.bookReason || null }).subscribe({
      next: a => {
        this.ui.success(`Booked ${a.patientName} at ${new Date(a.startTime).toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })}.`);
        this.bookReason = '';
        this.load();
      },
      error: err => this.ui.error(err),
    });
  }

  toggleBlock(s: Slot): void {
    const req = s.status === 'BLOCKED' ? this.api.unblockSlot(s.id) : this.api.blockSlot(s.id);
    req.subscribe({ next: () => this.load(), error: err => this.ui.error(err) });
  }

  setStatus(a: Appointment, status: string): void {
    this.api.appointmentStatus(a.id, status).subscribe({
      next: () => {
        this.ui.success(`${a.patientName}: ${status.replace('_', ' ').toLowerCase()}.`);
        this.load();
      },
      error: err => this.ui.error(err),
    });
  }

  async cancel(a: Appointment): Promise<void> {
    const reason = await this.ui.reason({ title: 'Cancel appointment?', message: `${a.patientName} with ${a.providerName}. The slot will become free again.`, confirmText: 'Cancel appointment', danger: true });
    if (reason === null) return;
    this.api.cancelAppointment(a.id, reason).subscribe({ next: () => this.load(), error: err => this.ui.error(err) });
  }

  startVisit(a: Appointment): void {
    this.dialog.open(StartEncounterDialog, {
      data: { patient: { id: a.patientId, fullName: a.patientName, mrn: a.patientMrn }, appointmentId: a.id, providerId: a.providerId, reason: a.reason },
      width: '560px', maxWidth: '96vw',
    }).afterClosed().subscribe((e?: Encounter) => e && this.router.navigate(['/encounters', e.id]));
  }

  generate(): void {
    this.dialog.open(GenerateSlotsDialog, { data: { providers: this.providers(), providerId: this.providerId() }, width: '560px', maxWidth: '96vw' })
      .afterClosed().subscribe(ok => ok && this.load());
  }
}
