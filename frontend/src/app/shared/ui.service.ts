import { inject, Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { firstValueFrom } from 'rxjs';
import { errorMessage } from '../core/util';
import { ConfirmDialog, ConfirmOptions } from './confirm-dialog';

/** Toasts and confirmation dialogs used by every screen. */
@Injectable({ providedIn: 'root' })
export class Ui {
  private readonly snack = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  success(message: string): void {
    this.snack.open(message, 'OK', { duration: 3500, panelClass: 'snack-success' });
  }

  error(err: unknown): void {
    this.snack.open(errorMessage(err), 'Dismiss', { duration: 7000, panelClass: 'snack-error' });
  }

  /** Resolves true when the user confirms. */
  async confirm(options: ConfirmOptions): Promise<boolean> {
    const result = await firstValueFrom(this.dialog.open(ConfirmDialog, { data: options, width: '460px' }).afterClosed());
    return result !== undefined && result !== false;
  }

  /** Confirmation that also asks for a reason; resolves the text or null when cancelled. */
  async reason(options: ConfirmOptions): Promise<string | null> {
    const result = await firstValueFrom(this.dialog.open(ConfirmDialog, {
      data: { reasonLabel: 'Reason', reasonRequired: true, ...options }, width: '480px',
    }).afterClosed());
    return typeof result === 'string' ? result : null;
  }
}
