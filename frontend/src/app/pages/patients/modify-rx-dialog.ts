import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Api } from '../../core/api.service';
import { CdsAlert, Prescription, ROUTES } from '../../core/models';
import { apiError, FREQUENCIES } from '../../core/util';
import { LabelPipe } from '../../shared/status-chip';
import { Ui } from '../../shared/ui.service';

/** Changes dose/frequency: the old prescription is discontinued and a replacement order is created. */
@Component({
  selector: 'cc-modify-rx',
  imports: [ReactiveFormsModule, LabelPipe, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatIconModule],
  template: `
    <h2 mat-dialog-title>Modify {{ rx.drugName }}</h2>
    <form [formGroup]="form" (ngSubmit)="save()">
      <mat-dialog-content>
        <p class="muted small">{{ rx.rxNo }} will be discontinued and replaced by a new prescription that goes back to the pharmacist for verification.</p>
        <div class="form-grid">
          <mat-form-field><mat-label>Dose</mat-label><input matInput formControlName="dose" /></mat-form-field>
          <mat-form-field><mat-label>Route</mat-label><mat-select formControlName="route">@for (r of routes; track r) { <mat-option [value]="r">{{ r | label }}</mat-option> }</mat-select></mat-form-field>
          <mat-form-field><mat-label>Frequency</mat-label><mat-select formControlName="frequency">@for (f of frequencies; track f) { <mat-option [value]="f">{{ f }}</mat-option> }</mat-select></mat-form-field>
          <mat-form-field><mat-label>Duration (days)</mat-label><input matInput type="number" formControlName="durationDays" /></mat-form-field>
          <mat-form-field><mat-label>Quantity</mat-label><input matInput type="number" formControlName="quantity" /></mat-form-field>
          <mat-form-field class="full"><mat-label>Instructions</mat-label><input matInput formControlName="instructions" /></mat-form-field>
        </div>
        @for (a of alerts(); track $index) {
          <div class="alert-box" [class]="'alert-box ' + a.severity"><mat-icon>warning</mat-icon>{{ a.message }}</div>
        }
        @if (alerts().length) {
          <mat-form-field class="full"><mat-label>Override reason</mat-label><input matInput formControlName="overrideReason" /></mat-form-field>
        }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancel</button>
        <button mat-flat-button type="submit" [disabled]="saving()">Save changes</button>
      </mat-dialog-actions>
    </form>
  `,
})
export class ModifyRxDialog {
  readonly rx = inject<Prescription>(MAT_DIALOG_DATA);
  private readonly api = inject(Api);
  private readonly ui = inject(Ui);
  private readonly ref = inject(MatDialogRef<ModifyRxDialog, boolean>);
  readonly routes = ROUTES;
  readonly frequencies = FREQUENCIES;
  readonly saving = signal(false);
  readonly alerts = signal<CdsAlert[]>([]);

  readonly form = inject(FormBuilder).group({
    dose: [this.rx.dose, Validators.required],
    route: [this.rx.route as string, Validators.required],
    frequency: [this.rx.frequency, Validators.required],
    durationDays: [this.rx.durationDays ?? 5, [Validators.required, Validators.min(1), Validators.max(365)]],
    quantity: [this.rx.quantity ?? 1, [Validators.required, Validators.min(1)]],
    instructions: [this.rx.instructions ?? ''],
    overrideReason: [''],
  });

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.saving.set(true);
    this.api.modifyRx(this.rx.id, { ...v, instructions: v.instructions || null, overrideReason: v.overrideReason || null }).subscribe({
      next: () => {
        this.ui.success('Prescription modified and sent for verification.');
        this.ref.close(true);
      },
      error: err => {
        this.saving.set(false);
        const e = apiError(err);
        if (e?.code === 'CDS_ALERTS') {
          this.alerts.set(e.details?.alerts ?? []);
          return;
        }
        this.ui.error(err);
      },
    });
  }
}
