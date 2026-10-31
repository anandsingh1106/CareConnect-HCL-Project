import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatTabsModule } from '@angular/material/tabs';
import { RouterLink } from '@angular/router';
import { Subject, debounceTime } from 'rxjs';
import { Api } from '../../core/api.service';
import { Icd10, LabTest } from '../../core/models';
import { Ui } from '../../shared/ui.service';

type LabForm = Omit<LabTest, 'id'> & { id?: number };

/** Code tables used across the system: ICD-10 diagnoses and the lab test catalogue. */
@Component({
  selector: 'cc-master-data',
  imports: [FormsModule, RouterLink, MatTabsModule, MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule, MatCheckboxModule, MatPaginatorModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div><h1>Master data</h1><p>ICD-10 codes and the lab test catalogue with normal and critical ranges.</p></div>
        <a mat-stroked-button routerLink="/pharmacy/inventory"><mat-icon>medication</mat-icon>Drugs &amp; interactions</a>
      </div>
      <mat-tab-group mat-stretch-tabs="false">
        <mat-tab label="ICD-10 codes">
          <div class="card filters mt">
            <mat-form-field subscriptSizing="dynamic"><mat-label>Search codes</mat-label><mat-icon matPrefix>search</mat-icon><input matInput [(ngModel)]="q" (ngModelChange)="q$.next()" /></mat-form-field>
            <button mat-flat-button (click)="editIcd(null)"><mat-icon>add</mat-icon>Add code</button>
          </div>
          @if (icdForm) {
            <div class="card mt">
              <div class="form-grid">
                <mat-form-field><mat-label>Code</mat-label><input matInput [(ngModel)]="icdForm.code" [disabled]="!icdNew" placeholder="E11.9" /></mat-form-field>
                <mat-form-field class="wide"><mat-label>Description</mat-label><input matInput [(ngModel)]="icdForm.description" /></mat-form-field>
                <mat-form-field><mat-label>Category</mat-label><input matInput [(ngModel)]="icdForm.category" /></mat-form-field>
                <mat-checkbox [(ngModel)]="icdForm.active">Active</mat-checkbox>
              </div>
              <div class="actions"><button mat-button (click)="icdForm = null">Cancel</button><button mat-flat-button (click)="saveIcd()">Save</button></div>
            </div>
          }
          <div class="table-wrap mt">
            <table class="simple">
              <tr><th>Code</th><th>Description</th><th>Category</th><th>Active</th><th></th></tr>
              @for (c of codes(); track c.code) {
                <tr><td class="mono"><strong>{{ c.code }}</strong></td><td>{{ c.description }}</td><td>{{ c.category }}</td>
                  <td>{{ c.active ? 'Yes' : 'No' }}</td><td class="right"><button mat-icon-button (click)="editIcd(c)"><mat-icon>edit</mat-icon></button></td></tr>
              }
            </table>
            <mat-paginator [length]="icdTotal()" [pageIndex]="icdPage()" [pageSize]="20" (page)="loadIcd($event.pageIndex)" hidePageSize />
          </div>
        </mat-tab>

        <mat-tab label="Lab tests">
          <div class="actions mt mb"><button mat-flat-button (click)="editLab(null)"><mat-icon>add</mat-icon>Add lab test</button></div>
          @if (labForm) {
            <div class="card mb">
              <div class="form-grid">
                <mat-form-field><mat-label>Code</mat-label><input matInput [(ngModel)]="labForm.code" /></mat-form-field>
                <mat-form-field><mat-label>Name</mat-label><input matInput [(ngModel)]="labForm.name" /></mat-form-field>
                <mat-form-field><mat-label>Category</mat-label><input matInput [(ngModel)]="labForm.category" /></mat-form-field>
                <mat-form-field><mat-label>Specimen</mat-label><input matInput [(ngModel)]="labForm.specimen" /></mat-form-field>
                <mat-form-field><mat-label>Unit</mat-label><input matInput [(ngModel)]="labForm.unit" /></mat-form-field>
                <mat-form-field><mat-label>Normal low</mat-label><input matInput type="number" [(ngModel)]="labForm.normalLow" /></mat-form-field>
                <mat-form-field><mat-label>Normal high</mat-label><input matInput type="number" [(ngModel)]="labForm.normalHigh" /></mat-form-field>
                <mat-form-field><mat-label>Critical low</mat-label><input matInput type="number" [(ngModel)]="labForm.criticalLow" /></mat-form-field>
                <mat-form-field><mat-label>Critical high</mat-label><input matInput type="number" [(ngModel)]="labForm.criticalHigh" /></mat-form-field>
                <mat-form-field><mat-label>Turnaround (hours)</mat-label><input matInput type="number" [(ngModel)]="labForm.turnaroundHours" /></mat-form-field>
                <mat-checkbox [(ngModel)]="labForm.active">Active</mat-checkbox>
              </div>
              <div class="actions"><button mat-button (click)="labForm = null">Cancel</button><button mat-flat-button (click)="saveLab()">Save</button></div>
            </div>
          }
          <div class="table-wrap">
            <table class="simple">
              <tr><th>Code</th><th>Test</th><th>Specimen</th><th>Normal range</th><th>Critical</th><th>TAT</th><th></th></tr>
              @for (t of labs(); track t.id) {
                <tr [style.opacity]="t.active ? 1 : .55"><td class="mono">{{ t.code }}</td><td><strong>{{ t.name }}</strong><div class="muted small">{{ t.category }}</div></td><td>{{ t.specimen }}</td>
                  <td>{{ t.normalLow ?? '' }} - {{ t.normalHigh ?? '' }} {{ t.unit }}</td>
                  <td>&lt;{{ t.criticalLow ?? '-' }} / &gt;{{ t.criticalHigh ?? '-' }}</td><td>{{ t.turnaroundHours }}h</td>
                  <td class="right"><button mat-icon-button (click)="editLab(t)"><mat-icon>edit</mat-icon></button></td></tr>
              }
            </table>
          </div>
        </mat-tab>
      </mat-tab-group>
    </div>
  `,
  styles: `.wide { grid-column: span 2; }`,
})
export class MasterData {
  private readonly api = inject(Api);
  private readonly ui = inject(Ui);
  readonly codes = signal<Icd10[]>([]);
  readonly icdTotal = signal(0);
  readonly icdPage = signal(0);
  readonly labs = signal<LabTest[]>([]);
  readonly q$ = new Subject<void>();
  q = '';
  icdForm: Icd10 | null = null;
  icdNew = false;
  labForm: LabForm | null = null;

  constructor() {
    this.loadIcd(0);
    this.loadLabs();
    this.q$.pipe(debounceTime(300)).subscribe(() => this.loadIcd(0));
  }

  loadIcd(page: number): void {
    this.icdPage.set(page);
    this.api.icd10(this.q.trim(), false, page, 20).subscribe(p => {
      this.codes.set(p.content);
      this.icdTotal.set(p.totalElements);
    });
  }

  loadLabs(): void {
    this.api.labTests(false).subscribe(l => this.labs.set(l));
  }

  editIcd(c: Icd10 | null): void {
    this.icdNew = !c;
    this.icdForm = c ? { ...c } : { code: '', description: '', category: '', active: true };
  }

  saveIcd(): void {
    const f = this.icdForm!;
    this.api.saveIcd10({ ...f, code: f.code.trim().toUpperCase() }, this.icdNew).subscribe({
      next: () => {
        this.ui.success('ICD-10 code saved.');
        this.icdForm = null;
        this.loadIcd(this.icdPage());
      },
      error: err => this.ui.error(err),
    });
  }

  editLab(t: LabTest | null): void {
    this.labForm = t ? { ...t } : { code: '', name: '', category: '', specimen: 'Blood', unit: '', turnaroundHours: 24, active: true };
  }

  saveLab(): void {
    const { id, ...body } = this.labForm!;
    this.api.saveLabTest(body, id).subscribe({
      next: () => {
        this.ui.success('Lab test saved.');
        this.labForm = null;
        this.loadLabs();
      },
      error: err => this.ui.error(err),
    });
  }
}

