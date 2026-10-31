import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Api } from '../../core/api.service';
import { OrderDetail } from '../../core/models';
import { LabelPipe, StatusChip } from '../../shared/status-chip';

/** Read-only view of an order with its CDS alerts, result and prescription. */
@Component({
  selector: 'cc-order-detail-dialog',
  imports: [MatDialogModule, MatButtonModule, MatIconModule, MatProgressBarModule, DatePipe, StatusChip, LabelPipe],
  template: `
    <h2 mat-dialog-title>Order {{ detail()?.order?.orderNo }}</h2>
    <mat-dialog-content class="dialog-wide">
      @if (detail(); as d) {
        <dl class="kv">
          <dt>Order</dt><dd>{{ d.order.summary }}</dd>
          <dt>Patient</dt><dd>{{ d.order.patientName }} ({{ d.order.patientMrn }})</dd>
          <dt>Type / priority</dt><dd>{{ d.order.orderType | label }} &middot; <cc-status [value]="d.order.priority" /></dd>
          <dt>Status</dt><dd><cc-status [value]="d.order.status" /></dd>
          <dt>Ordered by</dt><dd>{{ d.order.orderedByName }} &middot; {{ (d.order.placedAt ?? d.order.createdAt) | date: 'dd MMM yyyy, HH:mm' }}</dd>
          @if (d.order.clinicalIndication) { <dt>Indication</dt><dd>{{ d.order.clinicalIndication }}</dd> }
          @if (d.order.instructions) { <dt>Instructions</dt><dd>{{ d.order.instructions }}</dd> }
          @if (d.order.alertOverrideReason) { <dt>Override reason</dt><dd>{{ d.order.alertOverrideReason }}</dd> }
          @if (d.order.cancelReason) { <dt>Cancel reason</dt><dd>{{ d.order.cancelReason }}</dd> }
        </dl>

        @if (d.alerts.length) {
          <h3 class="section">Decision-support alerts</h3>
          @for (a of d.alerts; track a.id) {
            <div class="alert-box" [class]="'alert-box ' + a.severity">
              <mat-icon>warning</mat-icon>
              <div><strong>{{ a.alertType | label }}</strong> &middot; {{ a.message }} @if (a.overridden) { <em>(overridden)</em> }</div>
            </div>
          }
        }

        @if (d.result; as r) {
          <h3 class="section">Result</h3>
          <dl class="kv">
            <dt>Value</dt><dd>{{ r.valueNumeric ?? '' }} {{ r.unit }} <cc-status [value]="r.flag" /></dd>
            @if (r.referenceRange) { <dt>Reference</dt><dd>{{ r.referenceRange }}</dd> }
            @if (r.valueText) { <dt>Report</dt><dd class="pre">{{ r.valueText }}</dd> }
            @if (r.comments) { <dt>Comments</dt><dd>{{ r.comments }}</dd> }
            <dt>Resulted</dt><dd>{{ r.resultedAt | date: 'dd MMM yyyy, HH:mm' }} by {{ r.resultedByName }} (<cc-status [value]="r.resultStatus" />)</dd>
          </dl>
        }

        @if (d.prescription; as p) {
          <h3 class="section">Prescription {{ p.rxNo }}</h3>
          <dl class="kv">
            <dt>Medicine</dt><dd>{{ p.drugName }} ({{ p.genericName }})</dd>
            <dt>Sig</dt><dd>{{ p.dose }} &middot; {{ p.route | label }} &middot; {{ p.frequency }} &times; {{ p.durationDays }} days &middot; qty {{ p.quantity }}</dd>
            <dt>Status</dt><dd><cc-status [value]="p.status" /></dd>
            @if (p.verifiedByName) { <dt>Verified</dt><dd>{{ p.verifiedByName }} &middot; {{ p.verifiedAt | date: 'dd MMM, HH:mm' }}</dd> }
            @if (p.dispensedByName) { <dt>Dispensed</dt><dd>{{ p.dispensedQty }} by {{ p.dispensedByName }} &middot; {{ p.dispensedAt | date: 'dd MMM, HH:mm' }}</dd> }
          </dl>
        }
      } @else {
        <mat-progress-bar mode="indeterminate" />
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end"><button mat-button mat-dialog-close>Close</button></mat-dialog-actions>
  `,
  styles: `.section { font-size: 1rem; margin: 18px 0 8px; color: var(--cc-primary-dark); }`,
})
export class OrderDetailDialog {
  readonly detail = signal<OrderDetail | null>(null);

  constructor() {
    const { orderId } = inject<{ orderId: number }>(MAT_DIALOG_DATA);
    inject(Api).order(orderId).subscribe(d => this.detail.set(d));
  }
}
