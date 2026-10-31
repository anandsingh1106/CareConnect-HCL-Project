import { inject, Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Api } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Order } from '../../core/models';
import { apiError } from '../../core/util';
import { Ui } from '../../shared/ui.service';
import { OrderDetailDialog } from './order-detail-dialog';
import { ResultDialog } from './result-dialog';

/** Order lifecycle actions shared by the worklist, the patient chart and the encounter screen. */
@Injectable()
export class OrderActions {
  private readonly api = inject(Api);
  private readonly ui = inject(Ui);
  private readonly dialog = inject(MatDialog);
  private readonly auth = inject(AuthService);

  private open(o: Order): boolean {
    return o.status === 'PLACED' || o.status === 'IN_PROGRESS';
  }
  canResult(o: Order): boolean {
    return this.auth.hasRole('DOCTOR', 'NURSE') && (o.orderType === 'LAB' || o.orderType === 'IMAGING') && this.open(o);
  }
  canStart(o: Order): boolean {
    return this.auth.hasRole('DOCTOR', 'NURSE') && o.status === 'PLACED' && o.orderType !== 'MEDICATION';
  }
  canCancel(o: Order): boolean {
    return this.auth.hasRole('DOCTOR') && (o.status === 'DRAFT' || this.open(o));
  }
  canPlace(o: Order): boolean {
    return this.auth.hasRole('DOCTOR') && o.status === 'DRAFT';
  }

  view(o: Order): void {
    this.dialog.open(OrderDetailDialog, { data: { orderId: o.id }, maxWidth: '96vw' });
  }

  enterResult(o: Order, done: () => void): void {
    this.dialog.open(ResultDialog, { data: { order: o }, width: '560px', maxWidth: '96vw' }).afterClosed().subscribe(r => r && done());
  }

  start(o: Order, done: () => void): void {
    this.api.orderStatus(o.id, 'IN_PROGRESS').subscribe({
      next: () => {
        this.ui.success(`${o.orderNo} marked in progress.`);
        done();
      },
      error: err => this.ui.error(err),
    });
  }

  async cancel(o: Order, done: () => void): Promise<void> {
    const reason = await this.ui.reason({
      title: `Cancel ${o.orderNo}?`, message: `${o.summary} for ${o.patientName} will be cancelled.`,
      confirmText: 'Cancel order', danger: true, reasonLabel: 'Cancellation reason',
    });
    if (reason === null) return;
    this.api.cancelOrder(o.id, reason).subscribe({
      next: () => {
        this.ui.success(`${o.orderNo} cancelled.`);
        done();
      },
      error: err => this.ui.error(err),
    });
  }

  /** Places a draft; if decision support raises alerts the doctor must give an override reason. */
  place(o: Order, done: () => void, overrideReason?: string): void {
    this.api.placeOrder(o.id, overrideReason).subscribe({
      next: () => {
        this.ui.success(`${o.orderNo} placed.`);
        done();
      },
      error: async err => {
        const e = apiError(err);
        if (e?.code === 'CDS_ALERTS' && e.details?.alerts) {
          const alerts = (e.details.alerts as { severity: string; message: string; overridable: boolean }[]);
          const text = alerts.map(a => `• [${a.severity}] ${a.message}`).join('\n');
          if (alerts.some(a => !a.overridable)) {
            await this.ui.confirm({ title: 'Order blocked', message: text, confirmText: 'OK', danger: true });
            return;
          }
          const reason = await this.ui.reason({
            title: 'Decision support alerts', message: text, confirmText: 'Override & place', danger: true, reasonLabel: 'Override reason',
          });
          if (reason) this.place(o, done, reason);
          return;
        }
        this.ui.error(err);
      },
    });
  }
}
