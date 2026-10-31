import { Component, inject } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

export interface ConfirmOptions {
  title: string;
  message: string;
  confirmText?: string;
  danger?: boolean;
  /** When set, a text box is shown and its value is returned. */
  reasonLabel?: string;
  reasonRequired?: boolean;
  reasonValue?: string;
}

@Component({
  selector: 'cc-confirm-dialog',
  imports: [MatDialogModule, MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule, ReactiveFormsModule],
  template: `
    <h2 mat-dialog-title class="title">
      <mat-icon [class.danger]="data.danger">{{ data.danger ? 'warning' : 'help' }}</mat-icon>
      {{ data.title }}
    </h2>
    <mat-dialog-content>
      <p class="pre">{{ data.message }}</p>
      @if (data.reasonLabel) {
        <mat-form-field appearance="outline">
          <mat-label>{{ data.reasonLabel }}</mat-label>
          <textarea matInput rows="3" [formControl]="reason" maxlength="500" cdkFocusInitial></textarea>
          @if (reason.hasError('required')) { <mat-error>{{ data.reasonLabel }} is required</mat-error> }
        </mat-form-field>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-flat-button [class.danger-btn]="data.danger" (click)="ok()">{{ data.confirmText ?? 'Confirm' }}</button>
    </mat-dialog-actions>
  `,
  styles: `
    .title { display: flex; align-items: center; gap: 10px; }
    .title mat-icon { color: var(--cc-primary); }
    .title mat-icon.danger { color: var(--cc-danger); }
    .danger-btn { --mat-button-filled-container-color: var(--cc-danger); }
  `,
})
export class ConfirmDialog {
  readonly data = inject<ConfirmOptions>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<ConfirmDialog>);
  readonly reason = new FormControl(this.data.reasonValue ?? '', this.data.reasonRequired ? Validators.required : []);

  ok(): void {
    if (!this.data.reasonLabel) {
      this.ref.close(true);
      return;
    }
    const text = (this.reason.value ?? '').trim();
    if (this.data.reasonRequired && !text) {
      this.reason.setValue('');
      this.reason.markAsTouched();
      return;
    }
    this.ref.close(text);
  }
}
