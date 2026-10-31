import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { AuthService } from '../core/auth.service';
import { Ui } from '../shared/ui.service';

export const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d).{8,72}$/;

@Component({
  selector: 'cc-change-password',
  imports: [ReactiveFormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>Change password</h2>
    <form [formGroup]="form" (ngSubmit)="save()">
      <mat-dialog-content>
        <mat-form-field>
          <mat-label>Current password</mat-label>
          <input matInput type="password" formControlName="currentPassword" autocomplete="current-password" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>New password</mat-label>
          <input matInput type="password" formControlName="newPassword" autocomplete="new-password" />
          <mat-hint>At least 8 characters with a letter and a digit</mat-hint>
          @if (form.controls.newPassword.hasError('pattern')) { <mat-error>Needs 8+ characters, a letter and a digit</mat-error> }
        </mat-form-field>
        <mat-form-field>
          <mat-label>Confirm new password</mat-label>
          <input matInput type="password" formControlName="confirm" autocomplete="new-password" />
        </mat-form-field>
        @if (mismatch()) { <p class="alert-box danger">The new passwords do not match.</p> }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancel</button>
        <button mat-flat-button type="submit" [disabled]="saving()">Update password</button>
      </mat-dialog-actions>
    </form>
  `,
})
export class ChangePasswordDialog {
  private readonly auth = inject(AuthService);
  private readonly ui = inject(Ui);
  private readonly ref = inject(MatDialogRef<ChangePasswordDialog>);
  readonly saving = signal(false);
  readonly mismatch = signal(false);

  readonly form = inject(FormBuilder).nonNullable.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.pattern(PASSWORD_PATTERN)]],
    confirm: ['', Validators.required],
  });

  save(): void {
    const v = this.form.getRawValue();
    this.mismatch.set(v.newPassword !== v.confirm);
    if (this.form.invalid || this.mismatch()) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.auth.changePassword(v.currentPassword, v.newPassword).subscribe({
      next: () => {
        this.ui.success('Password changed. Other sessions have been signed out.');
        this.ref.close();
      },
      error: err => {
        this.saving.set(false);
        this.ui.error(err);
      },
    });
  }
}
