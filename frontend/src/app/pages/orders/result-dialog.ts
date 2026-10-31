import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Api } from '../../core/api.service';
import { LabResult, LabTest, Order } from '../../core/models';
import { Ui } from '../../shared/ui.service';

/** Lab/imaging result entry. Numeric lab values are flagged against the test's normal and critical ranges. */
@Component({
  selector: 'cc-result-dialog',
  imports: [ReactiveFormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatCheckboxModule, MatIconModule],
  template: `
    <h2 mat-dialog-title>Enter result &middot; {{ data.order.orderNo }}</h2>
    <form [formGroup]="form" (ngSubmit)="save()">
      <mat-dialog-content>
        <p><strong>{{ data.order.summary }}</strong> for {{ data.order.patientName }} ({{ data.order.patientMrn }})</p>
        @if (test(); as t) {
          <p class="muted small">Normal range: {{ t.normalLow ?? '-' }} - {{ t.normalHigh ?? '-' }} {{ t.unit }}
            @if (t.criticalLow !== null && t.criticalLow !== undefined) { &middot; critical below {{ t.criticalLow }} }
            @if (t.criticalHigh !== null && t.criticalHigh !== undefined) { &middot; critical above {{ t.criticalHigh }} }
          </p>
        }
        <div class="form-grid">
          @if (numeric()) {
            <mat-form-field>
              <mat-label>Value</mat-label>
              <input matInput type="number" step="any" formControlName="valueNumeric" />
              @if (test()?.unit) { <span matTextSuffix>{{ test()?.unit }}</span> }
            </mat-form-field>
            @if (preview(); as p) {
              <div class="preview"><span class="chip" [class]="'chip ' + p.tone">{{ p.text }}</span></div>
            }
          }
          <mat-form-field class="full">
            <mat-label>{{ numeric() ? 'Text result (optional)' : 'Findings / report' }}</mat-label>
            <textarea matInput rows="4" formControlName="valueText" maxlength="4000"></textarea>
          </mat-form-field>
          @if (!numeric()) {
            <mat-checkbox formControlName="abnormal" class="full">Abnormal finding</mat-checkbox>
          }
          <mat-form-field>
            <mat-label>Result status</mat-label>
            <mat-select formControlName="resultStatus">
              <mat-option value="FINAL">Final</mat-option>
              <mat-option value="PRELIMINARY">Preliminary</mat-option>
            </mat-select>
          </mat-form-field>
          <mat-form-field class="full">
            <mat-label>Comments</mat-label>
            <input matInput formControlName="comments" maxlength="1000" />
          </mat-form-field>
        </div>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancel</button>
        <button mat-flat-button type="submit" [disabled]="saving()"><mat-icon>task_alt</mat-icon>Save result</button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `.preview { display: flex; align-items: center; padding-bottom: 20px; }`,
})
export class ResultDialog {
  readonly data = inject<{ order: Order }>(MAT_DIALOG_DATA);
  private readonly api = inject(Api);
  private readonly ui = inject(Ui);
  private readonly ref = inject(MatDialogRef<ResultDialog, LabResult>);
  readonly test = signal<LabTest | null>(null);
  readonly saving = signal(false);
  readonly numeric = computed(() => this.data.order.orderType === 'LAB' && !!this.test()?.unit);

  readonly form = inject(FormBuilder).group({
    valueNumeric: [null as number | null],
    valueText: [''],
    abnormal: [false],
    resultStatus: ['FINAL'],
    comments: [''],
  });
  private readonly value = toSignal(this.form.controls.valueNumeric.valueChanges, { initialValue: null });

  /** Client-side preview of the flag the server will assign. */
  readonly preview = computed(() => {
    const t = this.test();
    const v = this.value();
    if (!t || v === null || v === undefined || (v as unknown) === '') return null;
    if (t.criticalLow != null && v < t.criticalLow) return { text: 'Critical low', tone: 'danger' };
    if (t.criticalHigh != null && v > t.criticalHigh) return { text: 'Critical high', tone: 'danger' };
    if (t.normalLow != null && v < t.normalLow) return { text: 'Low', tone: 'warning' };
    if (t.normalHigh != null && v > t.normalHigh) return { text: 'High', tone: 'warning' };
    return { text: 'Normal', tone: 'success' };
  });

  constructor() {
    const testId = this.data.order.labTestId;
    if (testId) {
      this.api.labTests(false).subscribe(list => this.test.set(list.find(t => t.id === testId) ?? null));
    }
  }

  save(): void {
    const v = this.form.getRawValue();
    if ((v.valueNumeric === null || (v.valueNumeric as unknown) === '') && !v.valueText?.trim()) {
      this.ui.error(new Error('Enter a value or a text result.'));
      return;
    }
    this.saving.set(true);
    this.api.enterResult(this.data.order.id, {
      valueNumeric: v.valueNumeric === null || (v.valueNumeric as unknown) === '' ? null : v.valueNumeric,
      valueText: v.valueText || null,
      abnormal: this.numeric() ? null : v.abnormal,
      resultStatus: v.resultStatus,
      comments: v.comments || null,
    }).subscribe({
      next: r => {
        this.ui.success(`Result saved (${r.flag.replace('_', ' ').toLowerCase()}).`);
        this.ref.close(r);
      },
      error: err => {
        this.saving.set(false);
        this.ui.error(err);
      },
    });
  }
}
