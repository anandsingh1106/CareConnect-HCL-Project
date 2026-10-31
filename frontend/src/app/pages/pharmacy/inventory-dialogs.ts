import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { catchError, debounceTime, map, of, switchMap } from 'rxjs';
import { Api } from '../../core/api.service';
import { Drug, InventoryTransaction, ROUTES } from '../../core/models';
import { LabelPipe, StatusChip } from '../../shared/status-chip';
import { Ui } from '../../shared/ui.service';

/** Add or edit a drug in the formulary. */
@Component({
  selector: 'cc-drug-dialog',
  imports: [ReactiveFormsModule, LabelPipe, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatCheckboxModule],
  template: `
    <h2 mat-dialog-title>{{ drug ? 'Edit ' + drug.name : 'Add drug' }}</h2>
    <form [formGroup]="form" (ngSubmit)="save()">
      <mat-dialog-content>
        <div class="form-grid">
          <mat-form-field><mat-label>Code</mat-label><input matInput formControlName="code" placeholder="e.g. PARA500" /><mat-error>Required</mat-error></mat-form-field>
          <mat-form-field><mat-label>Brand / display name</mat-label><input matInput formControlName="name" /><mat-error>Required</mat-error></mat-form-field>
          <mat-form-field><mat-label>Generic name</mat-label><input matInput formControlName="genericName" /><mat-error>Required</mat-error></mat-form-field>
          <mat-form-field><mat-label>Drug class</mat-label><input matInput formControlName="drugClass" placeholder="e.g. Penicillin" /></mat-form-field>
          <mat-form-field><mat-label>Dosage form</mat-label><input matInput formControlName="dosageForm" placeholder="TABLET, SYRUP..." /><mat-error>Required</mat-error></mat-form-field>
          <mat-form-field><mat-label>Strength</mat-label><input matInput formControlName="strength" placeholder="500 mg" /></mat-form-field>
          <mat-form-field><mat-label>Default route</mat-label>
            <mat-select formControlName="defaultRoute">@for (r of routes; track r) { <mat-option [value]="r">{{ r | label }}</mat-option> }</mat-select></mat-form-field>
          <mat-form-field><mat-label>Unit price</mat-label><input matInput type="number" step="0.01" formControlName="unitPrice" /><span matTextPrefix>&#8377;&nbsp;</span></mat-form-field>
          <mat-form-field><mat-label>Reorder level</mat-label><input matInput type="number" formControlName="reorderLevel" /><mat-error>Required</mat-error></mat-form-field>
          <mat-checkbox formControlName="active">Active (can be prescribed)</mat-checkbox>
        </div>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancel</button>
        <button mat-flat-button type="submit" [disabled]="saving()">Save</button>
      </mat-dialog-actions>
    </form>
  `,
})
export class DrugDialog {
  readonly drug = inject<Drug | null>(MAT_DIALOG_DATA);
  private readonly api = inject(Api);
  private readonly ui = inject(Ui);
  private readonly ref = inject(MatDialogRef<DrugDialog, boolean>);
  readonly routes = ROUTES;
  readonly saving = signal(false);
  readonly form = inject(FormBuilder).group({
    code: [this.drug?.code ?? '', Validators.required],
    name: [this.drug?.name ?? '', Validators.required],
    genericName: [this.drug?.genericName ?? '', Validators.required],
    drugClass: [this.drug?.drugClass ?? ''],
    dosageForm: [this.drug?.dosageForm ?? 'TABLET', Validators.required],
    strength: [this.drug?.strength ?? ''],
    defaultRoute: [this.drug?.defaultRoute ?? 'ORAL'],
    unitPrice: [this.drug?.unitPrice ?? (null as number | null)],
    reorderLevel: [this.drug?.reorderLevel ?? 20, [Validators.required, Validators.min(0)]],
    active: [this.drug?.active ?? true],
  });

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.saving.set(true);
    this.api.saveDrug({ ...v, drugClass: v.drugClass || null, strength: v.strength || null }, this.drug?.id).subscribe({
      next: () => {
        this.ui.success('Drug saved.');
        this.ref.close(true);
      },
      error: err => {
        this.saving.set(false);
        this.ui.error(err);
      },
    });
  }
}

/** Receive stock or correct it, and show the last movements. */
@Component({
  selector: 'cc-stock-dialog',
  imports: [ReactiveFormsModule, DatePipe, StatusChip, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  template: `
    <h2 mat-dialog-title>Stock &middot; {{ drug.name }}</h2>
    <mat-dialog-content class="dialog-wide">
      <p>Current stock: <strong>{{ drug.stockQty }}</strong> &middot; reorder level {{ drug.reorderLevel }}</p>
      <form class="form-grid" [formGroup]="form" (ngSubmit)="save()">
        <mat-form-field><mat-label>Movement</mat-label>
          <mat-select formControlName="txnType"><mat-option value="RECEIPT">Receipt (add stock)</mat-option><mat-option value="ADJUSTMENT">Adjustment (+/-)</mat-option></mat-select></mat-form-field>
        <mat-form-field><mat-label>Quantity</mat-label><input matInput type="number" formControlName="changeQty" /><mat-error>Required</mat-error></mat-form-field>
        <mat-form-field><mat-label>Reference</mat-label><input matInput formControlName="reference" placeholder="Invoice / batch / reason" /></mat-form-field>
        <div><button mat-flat-button type="submit" [disabled]="saving()">Save movement</button></div>
      </form>
      <h4>Recent movements</h4>
      <table class="simple">
        <tr><th>Date</th><th>Type</th><th>Change</th><th>Balance</th><th>Reference</th><th>By</th></tr>
        @for (t of txns(); track t.id) {
          <tr><td class="nowrap">{{ t.createdAt | date: 'dd MMM yy, HH:mm' }}</td><td><cc-status [value]="t.txnType" /></td>
            <td [style.color]="t.changeQty < 0 ? 'var(--cc-danger)' : 'var(--cc-success)'">{{ t.changeQty > 0 ? '+' : '' }}{{ t.changeQty }}</td>
            <td>{{ t.balanceAfter }}</td><td>{{ t.reference }}</td><td>{{ t.performedByName }}</td></tr>
        } @empty { <tr><td colspan="6" class="muted">No movements yet.</td></tr> }
      </table>
    </mat-dialog-content>
    <mat-dialog-actions align="end"><button mat-button [mat-dialog-close]="changed">Close</button></mat-dialog-actions>
  `,
})
export class StockDialog {
  drug = inject<Drug>(MAT_DIALOG_DATA);
  private readonly api = inject(Api);
  private readonly ui = inject(Ui);
  readonly txns = signal<InventoryTransaction[]>([]);
  readonly saving = signal(false);
  changed = false;
  readonly form = inject(FormBuilder).group({
    txnType: ['RECEIPT'],
    changeQty: [null as number | null, Validators.required],
    reference: [''],
  });

  constructor() {
    this.loadTxns();
  }

  loadTxns(): void {
    this.api.drugTransactions(this.drug.id).subscribe(t => this.txns.set(t));
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.saving.set(true);
    this.api.adjustStock(this.drug.id, { ...v, reference: v.reference || null }).subscribe({
      next: d => {
        this.saving.set(false);
        this.drug = d;
        this.changed = true;
        this.form.reset({ txnType: 'RECEIPT', changeQty: null, reference: '' });
        this.ui.success(`Stock updated: ${d.stockQty} units.`);
        this.loadTxns();
      },
      error: err => {
        this.saving.set(false);
        this.ui.error(err);
      },
    });
  }
}

/** Record a new drug-drug interaction for decision support. */
@Component({
  selector: 'cc-interaction-dialog',
  imports: [ReactiveFormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatAutocompleteModule],
  template: `
    <h2 mat-dialog-title>Add drug interaction</h2>
    <mat-dialog-content>
      <div class="form-grid">
        <mat-form-field><mat-label>Drug A</mat-label><input matInput [formControl]="qa" [matAutocomplete]="a" />
          <mat-autocomplete #a="matAutocomplete" [displayWith]="name" (optionSelected)="drugA = $event.option.value">
            @for (d of resultsA(); track d.id) { <mat-option [value]="d">{{ d.name }} ({{ d.genericName }})</mat-option> }</mat-autocomplete></mat-form-field>
        <mat-form-field><mat-label>Drug B</mat-label><input matInput [formControl]="qb" [matAutocomplete]="b" />
          <mat-autocomplete #b="matAutocomplete" [displayWith]="name" (optionSelected)="drugB = $event.option.value">
            @for (d of resultsB(); track d.id) { <mat-option [value]="d">{{ d.name }} ({{ d.genericName }})</mat-option> }</mat-autocomplete></mat-form-field>
        <mat-form-field><mat-label>Severity</mat-label>
          <mat-select [formControl]="severity">@for (s of ['MINOR','MODERATE','MAJOR','CONTRAINDICATED']; track s) { <mat-option [value]="s">{{ s }}</mat-option> }</mat-select></mat-form-field>
        <mat-form-field class="full"><mat-label>Clinical effect</mat-label><textarea matInput rows="2" [formControl]="description" maxlength="500"></textarea></mat-form-field>
      </div>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-flat-button (click)="save()">Save</button>
    </mat-dialog-actions>
  `,
})
export class InteractionDialog {
  private readonly api = inject(Api);
  private readonly ui = inject(Ui);
  private readonly ref = inject(MatDialogRef<InteractionDialog, boolean>);
  readonly qa = new FormControl<string | Drug>('');
  readonly qb = new FormControl<string | Drug>('');
  readonly severity = new FormControl('MODERATE');
  readonly description = new FormControl('');
  drugA: Drug | null = null;
  drugB: Drug | null = null;
  readonly resultsA = this.search(this.qa);
  readonly resultsB = this.search(this.qb);

  name = (d: Drug | string | null) => (d && typeof d === 'object' ? d.name : (d ?? ''));

  private search(ctrl: FormControl<string | Drug | null>) {
    return toSignal(ctrl.valueChanges.pipe(
      map(v => (typeof v === 'string' ? v.trim() : '')), debounceTime(250),
      switchMap(q => (q.length < 2 ? of([]) : this.api.drugs({ q, size: 10 }).pipe(map(p => p.content), catchError(() => of([])))))),
      { initialValue: [] as Drug[] });
  }

  save(): void {
    if (!this.drugA || !this.drugB || !this.description.value?.trim()) {
      this.ui.error(new Error('Choose both drugs and describe the interaction.'));
      return;
    }
    this.api.addInteraction({ drugAId: this.drugA.id, drugBId: this.drugB.id, severity: this.severity.value, description: this.description.value }).subscribe({
      next: () => {
        this.ui.success('Interaction added.');
        this.ref.close(true);
      },
      error: err => this.ui.error(err),
    });
  }
}
