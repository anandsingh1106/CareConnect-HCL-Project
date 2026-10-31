import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Api } from '../core/api.service';
import { Vital } from '../core/models';
import { apiError } from '../core/util';
import { Ui } from './ui.service';

export interface VitalsDialogData {
  patientId: number;
  patientName: string;
  encounterId?: number;
}

/** Record a set of vital signs. Ranges mirror the backend's plausibility validation. */
@Component({
  selector: 'cc-vitals-dialog',
  imports: [ReactiveFormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>Record vitals &middot; {{ data.patientName }}</h2>
    <form [formGroup]="form" (ngSubmit)="save()">
      <mat-dialog-content>
        <div class="form-grid">
          <mat-form-field><mat-label>Systolic BP</mat-label><input matInput type="number" formControlName="systolic" /><span matTextSuffix>mmHg</span><mat-error>50-260</mat-error></mat-form-field>
          <mat-form-field><mat-label>Diastolic BP</mat-label><input matInput type="number" formControlName="diastolic" /><span matTextSuffix>mmHg</span><mat-error>30-160</mat-error></mat-form-field>
          <mat-form-field><mat-label>Pulse</mat-label><input matInput type="number" formControlName="pulse" /><span matTextSuffix>/min</span><mat-error>20-250</mat-error></mat-form-field>
          <mat-form-field><mat-label>Temperature</mat-label><input matInput type="number" step="0.1" formControlName="temperature" /><span matTextSuffix>&deg;C</span><mat-error>30-45</mat-error></mat-form-field>
          <mat-form-field><mat-label>Respiratory rate</mat-label><input matInput type="number" formControlName="respRate" /><span matTextSuffix>/min</span><mat-error>4-60</mat-error></mat-form-field>
          <mat-form-field><mat-label>SpO2</mat-label><input matInput type="number" formControlName="spo2" /><span matTextSuffix>%</span><mat-error>50-100</mat-error></mat-form-field>
          <mat-form-field><mat-label>Weight</mat-label><input matInput type="number" step="0.1" formControlName="weightKg" /><span matTextSuffix>kg</span><mat-error>0.5-400</mat-error></mat-form-field>
          <mat-form-field><mat-label>Height</mat-label><input matInput type="number" step="0.1" formControlName="heightCm" /><span matTextSuffix>cm</span><mat-error>30-250</mat-error></mat-form-field>
          <div class="bmi">BMI <strong>{{ bmi() ?? '-' }}</strong></div>
          <mat-form-field class="full"><mat-label>Notes</mat-label><input matInput formControlName="notes" maxlength="500" /></mat-form-field>
        </div>
        @if (error()) { <div class="alert-box danger">{{ error() }}</div> }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancel</button>
        <button mat-flat-button type="submit" [disabled]="saving()">Save vitals</button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `.bmi { display: flex; align-items: center; gap: 8px; padding: 0 12px 20px; color: var(--cc-muted); strong { font-size: 1.3rem; color: var(--cc-text); } }`,
})
export class VitalsDialog {
  readonly data = inject<VitalsDialogData>(MAT_DIALOG_DATA);
  private readonly api = inject(Api);
  private readonly ui = inject(Ui);
  private readonly ref = inject(MatDialogRef<VitalsDialog, Vital>);
  readonly saving = signal(false);
  readonly error = signal('');

  readonly form = inject(FormBuilder).group({
    systolic: [null as number | null, [Validators.min(50), Validators.max(260)]],
    diastolic: [null as number | null, [Validators.min(30), Validators.max(160)]],
    pulse: [null as number | null, [Validators.min(20), Validators.max(250)]],
    temperature: [null as number | null, [Validators.min(30), Validators.max(45)]],
    respRate: [null as number | null, [Validators.min(4), Validators.max(60)]],
    spo2: [null as number | null, [Validators.min(50), Validators.max(100)]],
    weightKg: [null as number | null, [Validators.min(0.5), Validators.max(400)]],
    heightCm: [null as number | null, [Validators.min(30), Validators.max(250)]],
    notes: [''],
  });

  private readonly values = toSignal(this.form.valueChanges, { initialValue: this.form.value });
  readonly bmi = computed(() => {
    const { weightKg, heightCm } = this.values();
    return weightKg && heightCm ? (weightKg / (heightCm / 100) ** 2).toFixed(1) : null;
  });

  save(): void {
    const v = this.form.getRawValue();
    const measured = Object.entries(v).some(([k, val]) => k !== 'notes' && val !== null && val !== undefined && val !== ('' as unknown));
    if (this.form.invalid || !measured) {
      this.form.markAllAsTouched();
      this.error.set(measured ? 'Some values are outside the plausible range.' : 'Enter at least one measurement.');
      return;
    }
    if ((v.systolic === null) !== (v.diastolic === null)) {
      this.error.set('Enter both systolic and diastolic blood pressure.');
      return;
    }
    this.error.set('');
    this.saving.set(true);
    this.api.recordVitals(this.data.patientId, { ...v, notes: v.notes || null, encounterId: this.data.encounterId }).subscribe({
      next: vital => {
        this.ui.success('Vitals recorded.');
        this.ref.close(vital);
      },
      error: err => {
        this.saving.set(false);
        this.error.set(apiError(err)?.message ?? 'Could not save vitals.');
        this.ui.error(err);
      },
    });
  }
}
