import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { catchError, debounceTime, distinctUntilChanged, map, of, switchMap } from 'rxjs';
import { Api } from '../../core/api.service';
import { CdsAlert, CdsResult, Drug, LabTest, OrderDetail, OrderType, ROUTES } from '../../core/models';
import { apiError, dosesPerDay, FREQUENCIES } from '../../core/util';
import { LabelPipe } from '../../shared/status-chip';
import { Ui } from '../../shared/ui.service';

export interface OrderDialogData {
  patientId: number;
  patientName: string;
  encounterId: number;
  orderType?: OrderType;
}

/**
 * Computerized provider order entry. Decision support runs as soon as a drug or test is chosen;
 * the server repeats the checks on submit and answers 409 CDS_ALERTS when an override reason is needed.
 */
@Component({
  selector: 'cc-order-dialog',
  imports: [
    ReactiveFormsModule, LabelPipe, MatDialogModule, MatButtonModule, MatButtonToggleModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatAutocompleteModule, MatIconModule, MatCheckboxModule, MatProgressBarModule,
  ],
  template: `
    <h2 mat-dialog-title>New order &middot; {{ data.patientName }}</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <mat-dialog-content class="dialog-wide">
        <mat-button-toggle-group formControlName="orderType" class="types" aria-label="Order type">
          <mat-button-toggle value="MEDICATION"><mat-icon>medication</mat-icon> Medication</mat-button-toggle>
          <mat-button-toggle value="LAB"><mat-icon>science</mat-icon> Lab</mat-button-toggle>
          <mat-button-toggle value="IMAGING"><mat-icon>radiology</mat-icon> Imaging</mat-button-toggle>
          <mat-button-toggle value="REFERRAL"><mat-icon>send</mat-icon> Referral</mat-button-toggle>
        </mat-button-toggle-group>

        <div class="form-grid mt">
          @switch (type()) {
            @case ('MEDICATION') {
              <mat-form-field class="full">
                <mat-label>Drug</mat-label>
                <mat-icon matPrefix>search</mat-icon>
                <input matInput [formControl]="drugQuery" [matAutocomplete]="drugAuto" placeholder="Brand, generic name or class" />
                <mat-autocomplete #drugAuto="matAutocomplete" [displayWith]="drugLabel" (optionSelected)="pickDrug($event.option.value)">
                  @for (d of drugResults(); track d.id) {
                    <mat-option [value]="d">
                      <strong>{{ d.name }}</strong>&nbsp;<span class="muted small">{{ d.genericName }} &middot; {{ d.strength }} {{ d.dosageForm }} &middot; stock {{ d.stockQty }}</span>
                    </mat-option>
                  }
                </mat-autocomplete>
              </mat-form-field>
              <ng-container formGroupName="medication">
                <mat-form-field><mat-label>Dose</mat-label><input matInput formControlName="dose" placeholder="e.g. 500 mg" /><mat-error>Required</mat-error></mat-form-field>
                <mat-form-field>
                  <mat-label>Route</mat-label>
                  <mat-select formControlName="route">@for (r of routes; track r) { <mat-option [value]="r">{{ r | label }}</mat-option> }</mat-select>
                </mat-form-field>
                <mat-form-field>
                  <mat-label>Frequency</mat-label>
                  <mat-select formControlName="frequency" (selectionChange)="suggestQuantity()">@for (f of frequencies; track f) { <mat-option [value]="f">{{ f }}</mat-option> }</mat-select>
                </mat-form-field>
                <mat-form-field><mat-label>Duration</mat-label><input matInput type="number" formControlName="durationDays" (input)="suggestQuantity()" /><span matTextSuffix>days</span><mat-error>1-365</mat-error></mat-form-field>
                <mat-form-field><mat-label>Quantity</mat-label><input matInput type="number" formControlName="quantity" /><mat-error>1-10000</mat-error></mat-form-field>
                <mat-form-field class="full"><mat-label>Instructions to patient</mat-label><input matInput formControlName="instructions" maxlength="500" placeholder="e.g. after food" /></mat-form-field>
              </ng-container>
            }
            @case ('LAB') {
              <mat-form-field class="full">
                <mat-label>Lab test</mat-label>
                <mat-select formControlName="labTestId" (selectionChange)="runCheck()">
                  @for (t of labTests(); track t.id) { <mat-option [value]="t.id">{{ t.name }} <span class="muted small">({{ t.specimen }}{{ t.unit ? ', ' + t.unit : '' }})</span></mat-option> }
                </mat-select>
                <mat-error>Choose a test</mat-error>
              </mat-form-field>
            }
            @case ('IMAGING') {
              <mat-form-field><mat-label>Study</mat-label><input matInput formControlName="imagingStudy" placeholder="e.g. X-ray chest PA" (blur)="runCheck()" /><mat-error>Required</mat-error></mat-form-field>
              <mat-form-field><mat-label>Body site</mat-label><input matInput formControlName="bodySite" /></mat-form-field>
            }
            @case ('REFERRAL') {
              <mat-form-field><mat-label>Specialty</mat-label><input matInput formControlName="referralSpecialty" placeholder="e.g. Cardiology" /><mat-error>Required</mat-error></mat-form-field>
              <mat-form-field><mat-label>Refer to (doctor / hospital)</mat-label><input matInput formControlName="referralTo" /></mat-form-field>
            }
          }
          <mat-form-field>
            <mat-label>Priority</mat-label>
            <mat-select formControlName="priority">
              <mat-option value="ROUTINE">Routine</mat-option>
              <mat-option value="URGENT">Urgent</mat-option>
              <mat-option value="STAT">STAT</mat-option>
            </mat-select>
          </mat-form-field>
          <mat-form-field class="wide"><mat-label>Clinical indication</mat-label><input matInput formControlName="clinicalIndication" maxlength="500" /></mat-form-field>
          @if (type() !== 'MEDICATION') {
            <mat-form-field class="full"><mat-label>Instructions</mat-label><input matInput formControlName="instructions" maxlength="1000" /></mat-form-field>
          }
        </div>

        @if (checking()) { <mat-progress-bar mode="indeterminate" /> }
        @if (alerts().length) {
          <div class="cds">
            <div class="cds-title"><mat-icon>gpp_maybe</mat-icon> Clinical decision support</div>
            @for (a of alerts(); track $index) {
              <div class="alert-box" [class]="'alert-box ' + a.severity">
                <mat-icon>{{ a.severity === 'CRITICAL' ? 'dangerous' : a.severity === 'WARNING' ? 'warning' : 'info' }}</mat-icon>
                <div><strong>{{ a.type | label }} &middot; {{ a.severity | label }}</strong><br />{{ a.message }}
                  @if (!a.overridable) { <br /><strong>Hard stop - this order cannot be placed.</strong> }
                </div>
              </div>
            }
            @if (needsOverride() && !blocked()) {
              <mat-form-field class="w100">
                <mat-label>Override reason (required to continue)</mat-label>
                <textarea matInput rows="2" [formControl]="overrideReason" maxlength="500"></textarea>
                <mat-error>Explain why the benefit outweighs the risk</mat-error>
              </mat-form-field>
            }
          </div>
        } @else if (checked() && !checking()) {
          <div class="alert-box info"><mat-icon>verified</mat-icon>No allergy, interaction or duplicate alerts.</div>
        }
        <mat-checkbox formControlName="saveAsDraft">Save as draft (place later)</mat-checkbox>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancel</button>
        <button mat-flat-button type="submit" [disabled]="saving() || blocked()">
          <mat-icon>{{ form.value.saveAsDraft ? 'save' : 'send' }}</mat-icon>
          {{ form.value.saveAsDraft ? 'Save draft' : needsOverride() ? 'Override & place order' : 'Place order' }}
        </button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    .types { flex-wrap: wrap; } .types mat-icon { margin-right: 4px; vertical-align: middle; }
    .wide { grid-column: span 2; } .w100 { width: 100%; }
    .cds { margin: 8px 0 12px; } .cds-title { display: flex; align-items: center; gap: 6px; font-weight: 700; margin-bottom: 8px; color: var(--cc-primary-dark); }
    @media (max-width: 600px) { .wide { grid-column: auto; } }
  `,
})
export class OrderDialog {
  readonly data = inject<OrderDialogData>(MAT_DIALOG_DATA);
  private readonly api = inject(Api);
  private readonly ui = inject(Ui);
  private readonly ref = inject(MatDialogRef<OrderDialog, OrderDetail>);

  readonly routes = ROUTES;
  readonly frequencies = FREQUENCIES;
  readonly labTests = signal<LabTest[]>([]);
  readonly alerts = signal<CdsAlert[]>([]);
  readonly checked = signal(false);
  readonly checking = signal(false);
  readonly saving = signal(false);
  readonly selectedDrug = signal<Drug | null>(null);
  readonly blocked = computed(() => this.alerts().some(a => !a.overridable));
  readonly needsOverride = computed(() => this.alerts().some(a => a.severity !== 'INFO'));

  readonly drugQuery = new FormControl<string | Drug>('');
  readonly overrideReason = new FormControl('');

  readonly form = inject(FormBuilder).group({
    orderType: [this.data.orderType ?? ('MEDICATION' as OrderType), Validators.required],
    priority: ['ROUTINE'],
    labTestId: [null as number | null],
    imagingStudy: [''],
    bodySite: [''],
    referralSpecialty: [''],
    referralTo: [''],
    clinicalIndication: [''],
    instructions: [''],
    saveAsDraft: [false],
    medication: inject(FormBuilder).group({
      drugId: [null as number | null],
      dose: ['', [Validators.required, Validators.maxLength(50)]],
      route: ['ORAL', Validators.required],
      frequency: ['OD', Validators.required],
      durationDays: [5 as number | null, [Validators.required, Validators.min(1), Validators.max(365)]],
      quantity: [5 as number | null, [Validators.required, Validators.min(1), Validators.max(10000)]],
      instructions: [''],
    }),
  });

  readonly type = toSignal(this.form.controls.orderType.valueChanges, { initialValue: this.form.controls.orderType.value });

  readonly drugResults = toSignal(this.drugQuery.valueChanges.pipe(
    map(v => (typeof v === 'string' ? v.trim() : '')),
    debounceTime(250),
    distinctUntilChanged(),
    switchMap(q => (q.length < 2 ? of([]) : this.api.drugs({ q, size: 12 }).pipe(map(p => p.content), catchError(() => of([])))))),
    { initialValue: [] as Drug[] });

  constructor() {
    this.api.labTests().subscribe(t => this.labTests.set(t));
    this.form.controls.orderType.valueChanges.subscribe(() => this.resetAlerts());
  }

  drugLabel = (d: Drug | string | null) => (d && typeof d === 'object' ? `${d.name} (${d.genericName})` : (d ?? ''));

  pickDrug(d: Drug): void {
    this.selectedDrug.set(d);
    const med = this.form.controls.medication;
    med.patchValue({ drugId: d.id, dose: d.strength ?? '', route: (d.defaultRoute as string) || 'ORAL' });
    this.runCheck();
  }

  suggestQuantity(): void {
    const med = this.form.controls.medication.getRawValue();
    if (med.durationDays) {
      this.form.controls.medication.controls.quantity.setValue(Math.max(1, Math.ceil(dosesPerDay(med.frequency ?? 'OD') * med.durationDays)));
    }
  }

  /** Pre-check with the decision support service so the doctor sees alerts before submitting. */
  runCheck(): void {
    const v = this.form.getRawValue();
    const type = v.orderType as OrderType;
    const body = {
      patientId: this.data.patientId, orderType: type,
      drugId: type === 'MEDICATION' ? v.medication.drugId : null,
      labTestId: type === 'LAB' ? v.labTestId : null,
      imagingStudy: type === 'IMAGING' ? v.imagingStudy : null,
    };
    if ((type === 'MEDICATION' && !body.drugId) || (type === 'LAB' && !body.labTestId) || (type === 'IMAGING' && !body.imagingStudy)) return;
    this.checking.set(true);
    this.api.checkOrder(body).subscribe({
      next: (res: CdsResult) => {
        this.alerts.set(res.alerts);
        this.checked.set(true);
        this.checking.set(false);
      },
      error: () => this.checking.set(false),
    });
  }

  private resetAlerts(): void {
    this.alerts.set([]);
    this.checked.set(false);
    this.overrideReason.setValue('');
  }

  submit(): void {
    const v = this.form.getRawValue();
    const type = v.orderType as OrderType;
    const problems: string[] = [];
    if (type === 'MEDICATION') {
      if (!v.medication.drugId) problems.push('Choose a drug from the list.');
      if (this.form.controls.medication.invalid) problems.push('Complete the dose, frequency, duration and quantity.');
    }
    if (type === 'LAB' && !v.labTestId) problems.push('Choose a lab test.');
    if (type === 'IMAGING' && !v.imagingStudy?.trim()) problems.push('Enter the imaging study.');
    if (type === 'REFERRAL' && !v.referralSpecialty?.trim()) problems.push('Enter the referral specialty.');
    if (problems.length) {
      this.form.markAllAsTouched();
      this.ui.error(new Error(problems.join(' ')));
      return;
    }
    const reason = this.overrideReason.value?.trim();
    if (!v.saveAsDraft && this.needsOverride() && !reason) {
      this.overrideReason.markAsTouched();
      this.overrideReason.setErrors({ required: true });
      return;
    }

    const body = {
      patientId: this.data.patientId,
      encounterId: this.data.encounterId,
      orderType: type,
      priority: v.priority,
      labTestId: type === 'LAB' ? v.labTestId : null,
      imagingStudy: type === 'IMAGING' ? v.imagingStudy : null,
      bodySite: type === 'IMAGING' ? v.bodySite || null : null,
      referralSpecialty: type === 'REFERRAL' ? v.referralSpecialty : null,
      referralTo: type === 'REFERRAL' ? v.referralTo || null : null,
      clinicalIndication: v.clinicalIndication || null,
      instructions: type === 'MEDICATION' ? null : v.instructions || null,
      medication: type === 'MEDICATION' ? { ...v.medication, instructions: v.medication.instructions || null } : null,
      saveAsDraft: !!v.saveAsDraft,
      overrideReason: reason || null,
    };
    this.saving.set(true);
    this.api.createOrder(body).subscribe({
      next: detail => {
        this.ui.success(v.saveAsDraft ? `Draft ${detail.order.orderNo} saved.` : `Order ${detail.order.orderNo} placed.`);
        this.ref.close(detail);
      },
      error: err => {
        this.saving.set(false);
        const e = apiError(err);
        if (e?.code === 'CDS_ALERTS' && e.details?.alerts) {
          // The server found alerts that need an override reason: show them and wait for the doctor.
          this.alerts.set(e.details.alerts);
          this.checked.set(true);
          return;
        }
        this.ui.error(err);
      },
    });
  }
}
