import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { Api } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Order } from '../../core/models';
import { LabelPipe, StatusChip } from '../../shared/status-chip';
import { Ui } from '../../shared/ui.service';
import { OrderActions } from './order-actions';

@Component({
  selector: 'cc-order-list',
  imports: [
    FormsModule, RouterLink, DatePipe, StatusChip, LabelPipe, MatTableModule, MatPaginatorModule, MatSelectModule, MatFormFieldModule,
    MatButtonModule, MatButtonToggleModule, MatIconModule, MatMenuModule, MatProgressBarModule, MatSlideToggleModule,
  ],
  providers: [OrderActions],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>Orders &amp; results</h1>
          <p>{{ isDoctor() ? 'Orders you and your colleagues placed. Open an encounter to place new orders.' : 'Work through pending lab and imaging orders and enter results.' }}</p>
        </div>
      </div>

      <div class="card filters">
        <mat-form-field subscriptSizing="dynamic">
          <mat-label>Status</mat-label>
          <mat-select [(ngModel)]="status" (selectionChange)="load(0)">
            <mat-option [value]="null">All</mat-option>
            @for (s of statuses; track s) { <mat-option [value]="s">{{ s | label }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field subscriptSizing="dynamic">
          <mat-label>Type</mat-label>
          <mat-select [(ngModel)]="type" (selectionChange)="load(0)">
            <mat-option [value]="null">All</mat-option>
            @for (t of types; track t) { <mat-option [value]="t">{{ t | label }}</mat-option> }
          </mat-select>
        </mat-form-field>
        @if (isDoctor()) {
          <mat-slide-toggle [(ngModel)]="mine" (change)="load(0)">Only my orders</mat-slide-toggle>
        }
        <span class="spacer"></span>
        <button mat-stroked-button (click)="load(page())"><mat-icon>refresh</mat-icon>Refresh</button>
      </div>

      <div class="table-wrap mt">
        @if (loading()) { <mat-progress-bar mode="indeterminate" /> }
        <table mat-table [dataSource]="rows()">
          <ng-container matColumnDef="orderNo">
            <th mat-header-cell *matHeaderCellDef>Order</th>
            <td mat-cell *matCellDef="let o"><span class="mono">{{ o.orderNo }}</span></td>
          </ng-container>
          <ng-container matColumnDef="patient">
            <th mat-header-cell *matHeaderCellDef>Patient</th>
            <td mat-cell *matCellDef="let o"><a [routerLink]="['/patients', o.patientId]" (click)="$event.stopPropagation()">{{ o.patientName }}</a><div class="muted small mono">{{ o.patientMrn }}</div></td>
          </ng-container>
          <ng-container matColumnDef="summary">
            <th mat-header-cell *matHeaderCellDef>Details</th>
            <td mat-cell *matCellDef="let o"><strong>{{ o.summary }}</strong><div class="muted small">{{ o.orderType | label }}{{ o.clinicalIndication ? ' · ' + o.clinicalIndication : '' }}</div></td>
          </ng-container>
          <ng-container matColumnDef="priority">
            <th mat-header-cell *matHeaderCellDef>Priority</th>
            <td mat-cell *matCellDef="let o"><cc-status [value]="o.priority" /></td>
          </ng-container>
          <ng-container matColumnDef="status">
            <th mat-header-cell *matHeaderCellDef>Status</th>
            <td mat-cell *matCellDef="let o"><cc-status [value]="o.status" /></td>
          </ng-container>
          <ng-container matColumnDef="orderedBy">
            <th mat-header-cell *matHeaderCellDef>Ordered</th>
            <td mat-cell *matCellDef="let o">{{ o.orderedByName }}<div class="muted small">{{ (o.placedAt ?? o.createdAt) | date: 'dd MMM, HH:mm' }}</div></td>
          </ng-container>
          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef></th>
            <td mat-cell *matCellDef="let o" class="right nowrap" (click)="$event.stopPropagation()">
              @if (actions.canResult(o)) {
                <button mat-stroked-button (click)="actions.enterResult(o, reload)"><mat-icon>edit_note</mat-icon>Result</button>
              }
              <button mat-icon-button [matMenuTriggerFor]="menu" aria-label="Order actions"><mat-icon>more_vert</mat-icon></button>
              <mat-menu #menu="matMenu">
                <button mat-menu-item (click)="actions.view(o)"><mat-icon>visibility</mat-icon>View details</button>
                @if (actions.canStart(o)) { <button mat-menu-item (click)="actions.start(o, reload)"><mat-icon>play_arrow</mat-icon>Mark in progress</button> }
                @if (actions.canPlace(o)) { <button mat-menu-item (click)="actions.place(o, reload)"><mat-icon>send</mat-icon>Place draft order</button> }
                @if (actions.canCancel(o)) { <button mat-menu-item (click)="actions.cancel(o, reload)"><mat-icon>cancel</mat-icon>Cancel order</button> }
              </mat-menu>
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columns"></tr>
          <tr mat-row *matRowDef="let o; columns: columns" class="clickable" (click)="actions.view(o)"></tr>
        </table>
        @if (!loading() && !rows().length) { <div class="empty"><mat-icon>assignment</mat-icon>No orders match these filters.</div> }
        <mat-paginator [length]="total()" [pageIndex]="page()" [pageSize]="20" (page)="pageChange($event)" hidePageSize />
      </div>
    </div>
  `,
})
export class OrderList {
  private readonly api = inject(Api);
  private readonly ui = inject(Ui);
  private readonly auth = inject(AuthService);
  readonly actions = inject(OrderActions);

  readonly columns = ['orderNo', 'patient', 'summary', 'priority', 'status', 'orderedBy', 'actions'];
  readonly statuses = ['DRAFT', 'PLACED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'];
  readonly types = ['LAB', 'IMAGING', 'MEDICATION', 'REFERRAL'];
  readonly rows = signal<Order[]>([]);
  readonly total = signal(0);
  readonly page = signal(0);
  readonly loading = signal(false);
  readonly isDoctor = computed(() => this.auth.hasRole('DOCTOR'));

  status: string | null = this.auth.hasRole('NURSE') ? 'PLACED' : null;
  type: string | null = null;
  mine = false;
  readonly reload = () => this.load(this.page());

  constructor() {
    this.load(0);
  }

  load(page: number): void {
    this.loading.set(true);
    this.page.set(page);
    this.api.orders({ status: this.status, type: this.type, mine: this.mine, page, size: 20, sort: 'createdAt,desc' }).subscribe({
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

  pageChange(e: PageEvent): void {
    this.load(e.pageIndex);
  }
}
