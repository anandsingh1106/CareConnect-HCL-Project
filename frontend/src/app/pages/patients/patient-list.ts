import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { Router, RouterLink } from '@angular/router';
import { Api } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { PatientSummary } from '../../core/models';
import { isoDate } from '../../core/util';
import { Ui } from '../../shared/ui.service';

@Component({
  selector: 'cc-patient-list',
  imports: [
    ReactiveFormsModule, RouterLink, DatePipe, MatTableModule, MatPaginatorModule, MatSortModule, MatFormFieldModule,
    MatInputModule, MatButtonModule, MatIconModule, MatDatepickerModule, MatProgressBarModule, MatMenuModule,
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>Patients</h1>
          <p>Search by name, MRN, date of birth or phone number.</p>
        </div>
        @if (canRegister()) {
          <a mat-flat-button routerLink="/patients/new"><mat-icon>person_add</mat-icon>Register patient</a>
        }
      </div>

      <form class="card filters" [formGroup]="filters" (ngSubmit)="search(0)">
        <mat-form-field subscriptSizing="dynamic">
          <mat-label>Name</mat-label>
          <mat-icon matPrefix>search</mat-icon>
          <input matInput formControlName="q" placeholder="e.g. Kumar" />
        </mat-form-field>
        <mat-form-field subscriptSizing="dynamic">
          <mat-label>MRN</mat-label>
          <input matInput formControlName="mrn" placeholder="CC-2026-000001" />
        </mat-form-field>
        <mat-form-field subscriptSizing="dynamic">
          <mat-label>Date of birth</mat-label>
          <input matInput [matDatepicker]="dob" formControlName="dob" />
          <mat-datepicker-toggle matSuffix [for]="dob" />
          <mat-datepicker #dob />
        </mat-form-field>
        <mat-form-field subscriptSizing="dynamic">
          <mat-label>Phone</mat-label>
          <input matInput formControlName="phone" />
        </mat-form-field>
        <div class="actions">
          <button mat-flat-button type="submit"><mat-icon>search</mat-icon>Search</button>
          <button mat-button type="button" (click)="reset()">Clear</button>
        </div>
      </form>

      <div class="table-wrap mt">
        @if (loading()) { <mat-progress-bar mode="indeterminate" /> }
        <table mat-table [dataSource]="rows()" matSort (matSortChange)="sortChange($event)" matSortActive="lastName" matSortDirection="asc">
          <ng-container matColumnDef="mrn">
            <th mat-header-cell *matHeaderCellDef mat-sort-header="mrn">MRN</th>
            <td mat-cell *matCellDef="let p" class="mono">{{ p.mrn }}</td>
          </ng-container>
          <ng-container matColumnDef="lastName">
            <th mat-header-cell *matHeaderCellDef mat-sort-header="lastName">Name</th>
            <td mat-cell *matCellDef="let p"><strong>{{ p.fullName }}</strong></td>
          </ng-container>
          <ng-container matColumnDef="age">
            <th mat-header-cell *matHeaderCellDef mat-sort-header="dateOfBirth">Age / sex</th>
            <td mat-cell *matCellDef="let p">{{ p.age ?? '-' }} / {{ p.gender.charAt(0) }}</td>
          </ng-container>
          <ng-container matColumnDef="dob">
            <th mat-header-cell *matHeaderCellDef>DOB</th>
            <td mat-cell *matCellDef="let p">{{ p.dateOfBirth | date: 'dd MMM yyyy' }}</td>
          </ng-container>
          <ng-container matColumnDef="phone">
            <th mat-header-cell *matHeaderCellDef>Phone</th>
            <td mat-cell *matCellDef="let p">{{ p.phone }}</td>
          </ng-container>
          <ng-container matColumnDef="city">
            <th mat-header-cell *matHeaderCellDef mat-sort-header="city">City</th>
            <td mat-cell *matCellDef="let p">{{ p.city }}</td>
          </ng-container>
          <ng-container matColumnDef="provider">
            <th mat-header-cell *matHeaderCellDef>Primary doctor</th>
            <td mat-cell *matCellDef="let p">{{ p.primaryProviderName }}</td>
          </ng-container>
          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef></th>
            <td mat-cell *matCellDef="let p" class="right" (click)="$event.stopPropagation()">
              <button mat-icon-button [matMenuTriggerFor]="menu" aria-label="Actions"><mat-icon>more_vert</mat-icon></button>
              <mat-menu #menu="matMenu">
                @if (canOpen()) { <a mat-menu-item [routerLink]="['/patients', p.id]"><mat-icon>folder_open</mat-icon>Open chart</a> }
                @if (canRegister()) { <a mat-menu-item [routerLink]="['/patients', p.id, 'edit']"><mat-icon>edit</mat-icon>Edit demographics</a> }
                @if (isAdmin()) { <button mat-menu-item (click)="remove(p)"><mat-icon>delete</mat-icon>Delete record</button> }
              </mat-menu>
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columns"></tr>
          <tr mat-row *matRowDef="let p; columns: columns" [class.clickable]="canOpen()" (click)="open(p)"></tr>
        </table>
        @if (!loading() && !rows().length) {
          <div class="empty"><mat-icon>person_off</mat-icon>No patients found.</div>
        }
        <mat-paginator [length]="total()" [pageIndex]="page()" [pageSize]="size()" [pageSizeOptions]="[10, 20, 50]"
                       (page)="pageChange($event)" showFirstLastButtons />
      </div>
    </div>
  `,
})
export class PatientList {
  private readonly api = inject(Api);
  private readonly ui = inject(Ui);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  readonly columns = ['mrn', 'lastName', 'age', 'dob', 'phone', 'city', 'provider', 'actions'];
  readonly rows = signal<PatientSummary[]>([]);
  readonly total = signal(0);
  readonly page = signal(0);
  readonly size = signal(20);
  readonly loading = signal(false);
  private sort = 'lastName,asc';

  readonly canRegister = computed(() => this.auth.hasRole('RECEPTIONIST', 'DOCTOR'));
  readonly canOpen = computed(() => this.auth.hasRole('DOCTOR', 'NURSE', 'PHARMACIST', 'RECEPTIONIST'));
  readonly isAdmin = computed(() => this.auth.hasRole('ADMIN'));

  readonly filters = inject(FormBuilder).group({
    q: [''],
    mrn: [''],
    dob: [null as Date | null],
    phone: [''],
  });

  constructor() {
    this.search(0);
  }

  search(page: number): void {
    const f = this.filters.getRawValue();
    this.loading.set(true);
    this.page.set(page);
    this.api.searchPatients({ q: f.q?.trim(), mrn: f.mrn?.trim(), dob: isoDate(f.dob), phone: f.phone?.trim(), page, size: this.size(), sort: this.sort })
      .subscribe({
        next: res => {
          this.rows.set(res.content);
          this.total.set(res.totalElements);
          this.loading.set(false);
        },
        error: err => {
          this.loading.set(false);
          this.ui.error(err);
        },
      });
  }

  reset(): void {
    this.filters.reset({ q: '', mrn: '', dob: null, phone: '' });
    this.search(0);
  }

  pageChange(e: PageEvent): void {
    this.size.set(e.pageSize);
    this.search(e.pageIndex);
  }

  sortChange(s: Sort): void {
    this.sort = s.direction ? `${s.active},${s.direction}` : 'lastName,asc';
    this.search(0);
  }

  open(p: PatientSummary): void {
    if (this.canOpen()) this.router.navigate(['/patients', p.id]);
  }

  async remove(p: PatientSummary): Promise<void> {
    const ok = await this.ui.confirm({
      title: 'Delete patient record?',
      message: `${p.fullName} (${p.mrn}) will be hidden from all searches. The record is soft-deleted and kept for audit.`,
      confirmText: 'Delete', danger: true,
    });
    if (!ok) return;
    this.api.deletePatient(p.id).subscribe({
      next: () => {
        this.ui.success('Patient record deleted.');
        this.search(this.page());
      },
      error: err => this.ui.error(err),
    });
  }
}
