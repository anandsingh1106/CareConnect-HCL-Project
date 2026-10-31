import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { Api } from '../../core/api.service';
import { AuditLog } from '../../core/models';
import { isoDate } from '../../core/util';
import { LabelPipe, StatusChip } from '../../shared/status-chip';
import { Ui } from '../../shared/ui.service';

/** Read-only audit trail: who accessed which patient record, logins and failed logins. */
@Component({
  selector: 'cc-audit',
  imports: [FormsModule, DatePipe, LabelPipe, StatusChip, MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatDatepickerModule, MatPaginatorModule, MatProgressBarModule],
  template: `
    <div class="page">
      <div class="page-header"><div><h1>Audit log</h1><p>Every access to patient data and every sign-in attempt.</p></div></div>
      <div class="card filters">
        <mat-form-field subscriptSizing="dynamic"><mat-label>Username</mat-label><input matInput [(ngModel)]="f.username" /></mat-form-field>
        <mat-form-field subscriptSizing="dynamic"><mat-label>Patient ID</mat-label><input matInput type="number" [(ngModel)]="f.patientId" /></mat-form-field>
        <mat-form-field subscriptSizing="dynamic"><mat-label>Action</mat-label>
          <mat-select [(ngModel)]="f.action"><mat-option [value]="null">Any</mat-option>
            @for (a of actions; track a) { <mat-option [value]="a">{{ a | label }}</mat-option> }</mat-select></mat-form-field>
        <mat-form-field subscriptSizing="dynamic"><mat-label>From</mat-label><input matInput [matDatepicker]="d1" [(ngModel)]="f.from" /><mat-datepicker-toggle matSuffix [for]="d1" /><mat-datepicker #d1 /></mat-form-field>
        <mat-form-field subscriptSizing="dynamic"><mat-label>To</mat-label><input matInput [matDatepicker]="d2" [(ngModel)]="f.to" /><mat-datepicker-toggle matSuffix [for]="d2" /><mat-datepicker #d2 /></mat-form-field>
        <button mat-flat-button (click)="load(0)"><mat-icon>filter_alt</mat-icon>Apply</button>
      </div>
      <div class="table-wrap mt">
        @if (loading()) { <mat-progress-bar mode="indeterminate" /> }
        <table class="simple">
          <tr><th>Time</th><th>User</th><th>Action</th><th>Resource</th><th>Request</th><th>Status</th><th>IP</th></tr>
          @for (l of logs(); track l.id) {
            <tr>
              <td class="nowrap">{{ l.createdAt | date: 'dd MMM yy, HH:mm:ss' }}</td>
              <td>{{ l.username || '-' }}<div class="muted small">{{ l.userRole | label }}</div></td>
              <td><cc-status [value]="l.action" /></td>
              <td>{{ l.resourceType }} {{ l.resourceId }}@if (l.patientId) { <div class="muted small">patient #{{ l.patientId }}</div> }</td>
              <td class="mono small">{{ l.httpMethod }} {{ l.requestPath }}@if (l.details) { <div class="muted">{{ l.details }}</div> }</td>
              <td><span class="chip" [class.success]="(l.statusCode ?? 0) < 400" [class.danger]="(l.statusCode ?? 0) >= 400">{{ l.statusCode }}</span></td>
              <td class="mono small">{{ l.ipAddress }}</td>
            </tr>
          } @empty { <tr><td colspan="7" class="muted">No audit entries.</td></tr> }
        </table>
        <mat-paginator [length]="total()" [pageIndex]="page()" [pageSize]="25" (page)="load($event.pageIndex)" hidePageSize showFirstLastButtons />
      </div>
    </div>
  `,
})
export class Audit {
  private readonly api = inject(Api);
  private readonly ui = inject(Ui);
  readonly actions = ['CREATE', 'READ', 'UPDATE', 'DELETE', 'LOGIN', 'LOGIN_FAILED', 'LOGOUT'];
  readonly logs = signal<AuditLog[]>([]);
  readonly total = signal(0);
  readonly page = signal(0);
  readonly loading = signal(false);
  f: { username: string; patientId: number | null; action: string | null; from: Date | null; to: Date | null } =
    { username: '', patientId: null, action: null, from: null, to: null };

  constructor() {
    this.load(0);
  }

  load(page: number): void {
    this.loading.set(true);
    this.page.set(page);
    this.api.auditLogs({
      username: this.f.username.trim(), patientId: this.f.patientId, action: this.f.action,
      from: isoDate(this.f.from), to: isoDate(this.f.to), page, size: 25,
    }).subscribe({
      next: p => {
        this.logs.set(p.content);
        this.total.set(p.totalElements);
        this.loading.set(false);
      },
      error: err => {
        this.loading.set(false);
        this.ui.error(err);
      },
    });
  }

  pageChange(e: PageEvent): void {
    this.load(e.pageIndex);
  }
}
