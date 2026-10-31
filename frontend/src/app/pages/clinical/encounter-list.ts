import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { Router } from '@angular/router';
import { Api } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Encounter, EncounterStatus } from '../../core/models';
import { LabelPipe, StatusChip } from '../../shared/status-chip';
import { Ui } from '../../shared/ui.service';
import { StartEncounterDialog } from './start-encounter';

@Component({
  selector: 'cc-encounter-list',
  imports: [DatePipe, StatusChip, LabelPipe, MatTableModule, MatButtonModule, MatButtonToggleModule, MatIconModule, MatProgressBarModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>Encounters</h1>
          <p>{{ isDoctor ? 'Your visits. Sign every note before closing an encounter.' : 'Open visits across the clinic.' }}</p>
        </div>
        <div class="actions">
          <mat-button-toggle-group [value]="status()" (change)="setStatus($event.value)">
            <mat-button-toggle value="OPEN">Open</mat-button-toggle>
            <mat-button-toggle value="CLOSED">Closed</mat-button-toggle>
          </mat-button-toggle-group>
          <button mat-flat-button (click)="start()"><mat-icon>add</mat-icon>Start encounter</button>
        </div>
      </div>
      <div class="table-wrap">
        @if (loading()) { <mat-progress-bar mode="indeterminate" /> }
        <table mat-table [dataSource]="rows()">
          <ng-container matColumnDef="date">
            <th mat-header-cell *matHeaderCellDef>Date</th>
            <td mat-cell *matCellDef="let e" class="nowrap">{{ e.encounterDate | date: 'dd MMM yyyy' }}<div class="muted small">{{ e.encounterDate | date: 'HH:mm' }}</div></td>
          </ng-container>
          <ng-container matColumnDef="patient">
            <th mat-header-cell *matHeaderCellDef>Patient</th>
            <td mat-cell *matCellDef="let e"><strong>{{ e.patientName }}</strong><div class="muted small mono">{{ e.patientMrn }}</div></td>
          </ng-container>
          <ng-container matColumnDef="type">
            <th mat-header-cell *matHeaderCellDef>Type</th>
            <td mat-cell *matCellDef="let e">{{ e.encounterType | label }}</td>
          </ng-container>
          <ng-container matColumnDef="complaint">
            <th mat-header-cell *matHeaderCellDef>Chief complaint / diagnosis</th>
            <td mat-cell *matCellDef="let e">{{ e.chiefComplaint }}@if (e.primaryDiagnosis) { <div class="muted small">Dx: {{ e.primaryDiagnosis }}</div> }</td>
          </ng-container>
          <ng-container matColumnDef="provider">
            <th mat-header-cell *matHeaderCellDef>Doctor</th>
            <td mat-cell *matCellDef="let e">{{ e.providerName }}<div class="muted small">{{ e.departmentName }}</div></td>
          </ng-container>
          <ng-container matColumnDef="status">
            <th mat-header-cell *matHeaderCellDef>Status</th>
            <td mat-cell *matCellDef="let e"><cc-status [value]="e.status" /></td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columns"></tr>
          <tr mat-row *matRowDef="let e; columns: columns" class="clickable" (click)="open(e)"></tr>
        </table>
        @if (!loading() && !rows().length) { <div class="empty"><mat-icon>clinical_notes</mat-icon>No {{ status().toLowerCase() }} encounters.</div> }
      </div>
    </div>
  `,
})
export class EncounterList {
  private readonly api = inject(Api);
  private readonly ui = inject(Ui);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  readonly isDoctor = inject(AuthService).hasRole('DOCTOR');

  readonly columns = ['date', 'patient', 'type', 'complaint', 'provider', 'status'];
  readonly rows = signal<Encounter[]>([]);
  readonly status = signal<EncounterStatus>('OPEN');
  readonly loading = signal(false);

  constructor() {
    this.load();
  }

  setStatus(s: EncounterStatus): void {
    this.status.set(s);
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.api.encounterWorklist(this.status()).subscribe({
      next: list => {
        this.rows.set(list);
        this.loading.set(false);
      },
      error: err => {
        this.loading.set(false);
        this.ui.error(err);
      },
    });
  }

  open(e: Encounter): void {
    this.router.navigate(['/encounters', e.id]);
  }

  start(): void {
    this.dialog.open(StartEncounterDialog, { width: '560px', maxWidth: '96vw' }).afterClosed()
      .subscribe((e?: Encounter) => e && this.router.navigate(['/encounters', e.id]));
  }
}
