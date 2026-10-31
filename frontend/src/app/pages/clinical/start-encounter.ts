import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Api } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Encounter, PatientSummary, Provider } from '../../core/models';
import { PatientPicker } from '../../shared/patient-picker';
import { Ui } from '../../shared/ui.service';

export interface StartEncounterData {
  patient?: { id: number; fullName: string; mrn: string };
  appointmentId?: number;
  providerId?: number;
  reason?: string;
}

/** Opens a new visit (encounter) for a patient. Nurses choose the treating doctor. */
@Component({
  selector: 'cc-start-encounter',
  imports: [ReactiveFormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule, PatientPicker],
  template: `
    <h2 mat-dialog-title>Start encounter</h2>
    <form [formGroup]="form" (ngSubmit)="save()">
      <mat-dialog-content>
        @if (patient(); as p) {
          <p class="alert-box info">Patient: <strong>&nbsp;{{ p.fullName }}</strong>&nbsp;({{ p.mrn }})</p>
        } @else {
          <cc-patient-picker (selected)="pick($event)" />
        }
        <div class="form-grid">
          <mat-form-field>
            <mat-label>Visit type</mat-label>
            <mat-select formControlName="encounterType">
              @for (t of types; track t) { <mat-option [value]="t">{{ t.replace('_', ' ') }}</mat-option> }
            </mat-select>
          </mat-form-field>
          @if (!isDoctor) {
            <mat-form-field>
              <mat-label>Treating doctor</mat-label>
              <mat-select formControlName="providerId">
                @for (p of providers(); track p.id) { <mat-option [value]="p.id">{{ p.fullName }}</mat-option> }
              </mat-select>
              <mat-error>Choose a doctor</mat-error>
            </mat-form-field>
          }
          <mat-form-field class="full">
            <mat-label>Chief complaint</mat-label>
            <input matInput formControlName="chiefComplaint" maxlength="500" />
            <mat-error>Chief complaint is required</mat-error>
          </mat-form-field>
        </div>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancel</button>
        <button mat-flat-button type="submit" [disabled]="saving() || !patient()">Start encounter</button>
      </mat-dialog-actions>
    </form>
  `,
})
export class StartEncounterDialog {
  readonly data = inject<StartEncounterData>(MAT_DIALOG_DATA, { optional: true }) ?? {};
  private readonly api = inject(Api);
  private readonly ui = inject(Ui);
  private readonly ref = inject(MatDialogRef<StartEncounterDialog, Encounter>);
  readonly isDoctor = inject(AuthService).hasRole('DOCTOR');

  readonly types = ['OUTPATIENT', 'FOLLOW_UP', 'EMERGENCY', 'TELEHEALTH', 'INPATIENT'];
  readonly patient = signal(this.data.patient ?? null);
  readonly providers = signal<Provider[]>([]);
  readonly saving = signal(false);

  readonly form = inject(FormBuilder).group({
    encounterType: ['OUTPATIENT', Validators.required],
    providerId: [this.data.providerId ?? (null as number | null), this.isDoctor ? [] : [Validators.required]],
    chiefComplaint: [this.data.reason ?? '', [Validators.required, Validators.maxLength(500)]],
  });

  constructor() {
    if (!this.isDoctor) this.api.providers().subscribe(p => this.providers.set(p));
  }

  pick(p: PatientSummary): void {
    this.patient.set(p);
  }

  save(): void {
    const patient = this.patient();
    if (this.form.invalid || !patient) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    const v = this.form.getRawValue();
    this.api.openEncounter({
      patientId: patient.id,
      providerId: this.isDoctor ? null : v.providerId,
      encounterType: v.encounterType,
      chiefComplaint: v.chiefComplaint,
      appointmentId: this.data.appointmentId,
    }).subscribe({
      next: e => {
        this.ui.success('Encounter started.');
        this.ref.close(e);
      },
      error: err => {
        this.saving.set(false);
        this.ui.error(err);
      },
    });
  }
}
