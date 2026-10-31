import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Observable } from 'rxjs';
import { Api } from '../../core/api.service';
import { Icd10 } from '../../core/models';
import { clean, parseDate } from '../../core/util';
import { Icd10Picker } from '../../shared/icd10-picker';
import { Ui } from '../../shared/ui.service';

export type HistoryKind = 'allergy' | 'problem' | 'immunization' | 'family';

export interface HistoryDialogData {
  kind: HistoryKind;
  patientId: number;
  /** Existing record when editing. */
  record?: any;
}

const TITLES: Record<HistoryKind, string> = {
  allergy: 'allergy', problem: 'problem', immunization: 'immunization', family: 'family history',
};

/** Add or edit an allergy, problem, immunization or family-history entry. */
@Component({
  selector: 'cc-history-dialog',
  imports: [ReactiveFormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatDatepickerModule, Icd10Picker],
  template: `
    <h2 mat-dialog-title>{{ data.record ? 'Edit' : 'Add' }} {{ title }}</h2>
    <form [formGroup]="form" (ngSubmit)="save()">
      <mat-dialog-content>
        <div class="form-grid">
          @switch (data.kind) {
            @case ('allergy') {
              <mat-form-field><mat-label>Allergen</mat-label><input matInput formControlName="allergen" placeholder="e.g. Penicillin" /><mat-error>Required</mat-error></mat-form-field>
              <mat-form-field><mat-label>Type</mat-label>
                <mat-select formControlName="allergyType">@for (t of ['DRUG','FOOD','ENVIRONMENT','OTHER']; track t) { <mat-option [value]="t">{{ t }}</mat-option> }</mat-select></mat-form-field>
              <mat-form-field><mat-label>Reaction</mat-label><input matInput formControlName="reaction" placeholder="e.g. Rash, anaphylaxis" /></mat-form-field>
              <mat-form-field><mat-label>Severity</mat-label>
                <mat-select formControlName="severity">@for (t of ['MILD','MODERATE','SEVERE']; track t) { <mat-option [value]="t">{{ t }}</mat-option> }</mat-select></mat-form-field>
              <mat-form-field><mat-label>Status</mat-label>
                <mat-select formControlName="status"><mat-option value="ACTIVE">Active</mat-option><mat-option value="INACTIVE">Inactive</mat-option></mat-select></mat-form-field>
              <mat-form-field><mat-label>Noted on</mat-label><input matInput [matDatepicker]="d1" formControlName="notedOn" [max]="today" /><mat-datepicker-toggle matSuffix [for]="d1" /><mat-datepicker #d1 /></mat-form-field>
            }
            @case ('problem') {
              <div class="full"><cc-icd10-picker label="ICD-10 code (optional)" (selected)="pickCode($event)" /></div>
              <mat-form-field class="full"><mat-label>Problem description</mat-label><input matInput formControlName="description" /><mat-error>Required</mat-error></mat-form-field>
              <mat-form-field><mat-label>Status</mat-label>
                <mat-select formControlName="status"><mat-option value="ACTIVE">Active</mat-option><mat-option value="RESOLVED">Resolved</mat-option></mat-select></mat-form-field>
              <mat-form-field><mat-label>Onset date</mat-label><input matInput [matDatepicker]="d2" formControlName="onsetDate" [max]="today" /><mat-datepicker-toggle matSuffix [for]="d2" /><mat-datepicker #d2 /></mat-form-field>
              <mat-form-field><mat-label>Resolved date</mat-label><input matInput [matDatepicker]="d3" formControlName="resolvedDate" [max]="today" /><mat-datepicker-toggle matSuffix [for]="d3" /><mat-datepicker #d3 /></mat-form-field>
            }
            @case ('immunization') {
              <mat-form-field><mat-label>Vaccine</mat-label><input matInput formControlName="vaccineName" /><mat-error>Required</mat-error></mat-form-field>
              <mat-form-field><mat-label>Dose number</mat-label><input matInput type="number" formControlName="doseNumber" /></mat-form-field>
              <mat-form-field><mat-label>Given on</mat-label><input matInput [matDatepicker]="d4" formControlName="administeredOn" [max]="today" /><mat-datepicker-toggle matSuffix [for]="d4" /><mat-datepicker #d4 /><mat-error>Required</mat-error></mat-form-field>
              <mat-form-field><mat-label>Given by</mat-label><input matInput formControlName="administeredBy" /></mat-form-field>
              <mat-form-field><mat-label>Lot number</mat-label><input matInput formControlName="lotNumber" /></mat-form-field>
              <mat-form-field class="full"><mat-label>Notes</mat-label><input matInput formControlName="notes" /></mat-form-field>
            }
            @case ('family') {
              <mat-form-field><mat-label>Relation</mat-label>
                <mat-select formControlName="relation">@for (r of relations; track r) { <mat-option [value]="r">{{ r }}</mat-option> }</mat-select><mat-error>Required</mat-error></mat-form-field>
              <mat-form-field><mat-label>Condition</mat-label><input matInput formControlName="conditionName" /><mat-error>Required</mat-error></mat-form-field>
              <mat-form-field class="full"><mat-label>Notes</mat-label><input matInput formControlName="notes" /></mat-form-field>
            }
          }
        </div>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancel</button>
        <button mat-flat-button type="submit" [disabled]="saving()">Save</button>
      </mat-dialog-actions>
    </form>
  `,
})
export class HistoryDialog {
  readonly data = inject<HistoryDialogData>(MAT_DIALOG_DATA);
  private readonly api = inject(Api);
  private readonly ui = inject(Ui);
  private readonly ref = inject(MatDialogRef<HistoryDialog, boolean>);
  private readonly fb = inject(FormBuilder);
  readonly title = TITLES[this.data.kind];
  readonly today = new Date();
  readonly saving = signal(false);
  readonly relations = ['Father', 'Mother', 'Brother', 'Sister', 'Son', 'Daughter', 'Grandfather', 'Grandmother', 'Uncle', 'Aunt'];

  readonly form = this.build();

  private build() {
    const r = this.data.record ?? {};
    const req = Validators.required;
    switch (this.data.kind) {
      case 'allergy':
        return this.fb.group({
          allergen: [r.allergen ?? '', req], allergyType: [r.allergyType ?? 'DRUG', req], reaction: [r.reaction ?? ''],
          severity: [r.severity ?? 'MODERATE', req], status: [r.status ?? 'ACTIVE'], notedOn: [parseDate(r.notedOn)],
        });
      case 'problem':
        return this.fb.group({
          icd10Code: [r.icd10Code ?? ''], description: [r.description ?? '', req], status: [r.status ?? 'ACTIVE'],
          onsetDate: [parseDate(r.onsetDate)], resolvedDate: [parseDate(r.resolvedDate)],
        });
      case 'immunization':
        return this.fb.group({
          vaccineName: [r.vaccineName ?? '', req], doseNumber: [r.doseNumber ?? 1], administeredOn: [parseDate(r.administeredOn) ?? new Date(), req],
          administeredBy: [r.administeredBy ?? ''], lotNumber: [r.lotNumber ?? ''], notes: [r.notes ?? ''],
        });
      default:
        return this.fb.group({ relation: [r.relation ?? '', req], conditionName: [r.conditionName ?? '', req], notes: [r.notes ?? ''] });
    }
  }

  pickCode(c: Icd10): void {
    this.form.patchValue({ icd10Code: c.code, description: this.form.value['description' as never] || c.description } as never);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const body = clean(this.form.getRawValue() as object);
    const { patientId, record } = this.data;
    const id = record?.id as number | undefined;
    const calls: Record<HistoryKind, () => Observable<unknown>> = {
      allergy: () => this.api.saveAllergy(patientId, body, id),
      problem: () => this.api.saveProblem(patientId, body, id),
      immunization: () => this.api.saveImmunization(patientId, body, id),
      family: () => this.api.saveFamilyHistory(patientId, body, id),
    };
    this.saving.set(true);
    calls[this.data.kind]().subscribe({
      next: () => {
        this.ui.success(`${this.title.charAt(0).toUpperCase() + this.title.slice(1)} saved.`);
        this.ref.close(true);
      },
      error: err => {
        this.saving.set(false);
        this.ui.error(err);
      },
    });
  }
}
