import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { Subject, debounceTime } from 'rxjs';
import { Api } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Department, PatientSummary, Role, ROLES, User } from '../../core/models';
import { PatientPicker } from '../../shared/patient-picker';
import { LabelPipe, StatusChip } from '../../shared/status-chip';
import { Ui } from '../../shared/ui.service';
import { PASSWORD_PATTERN } from '../../layout/change-password';

@Component({
  selector: 'cc-user-dialog',
  imports: [ReactiveFormsModule, LabelPipe, PatientPicker, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  template: `
    <h2 mat-dialog-title>{{ user ? 'Edit ' + user.username : 'New user account' }}</h2>
    <form [formGroup]="form" (ngSubmit)="save()">
      <mat-dialog-content>
        <div class="form-grid">
          <mat-form-field><mat-label>Username</mat-label><input matInput formControlName="username" autocomplete="off" /><mat-error>3-50 letters, digits, . _ -</mat-error></mat-form-field>
          <mat-form-field><mat-label>Full name</mat-label><input matInput formControlName="fullName" /><mat-error>Required</mat-error></mat-form-field>
          <mat-form-field><mat-label>Role</mat-label>
            <mat-select formControlName="role">@for (r of roles; track r) { <mat-option [value]="r">{{ r | label }}</mat-option> }</mat-select></mat-form-field>
          <mat-form-field><mat-label>Email</mat-label><input matInput formControlName="email" type="email" /><mat-error>Invalid email</mat-error></mat-form-field>
          <mat-form-field><mat-label>Phone</mat-label><input matInput formControlName="phone" /></mat-form-field>
          @if (role() !== 'PATIENT') {
            <mat-form-field><mat-label>Department</mat-label>
              <mat-select formControlName="departmentId"><mat-option [value]="null">None</mat-option>
                @for (d of data.departments; track d.id) { <mat-option [value]="d.id">{{ d.name }}</mat-option> }</mat-select></mat-form-field>
          }
          @if (role() === 'DOCTOR') {
            <mat-form-field><mat-label>Specialty</mat-label><input matInput formControlName="specialty" /></mat-form-field>
          }
          @if (role() === 'DOCTOR' || role() === 'NURSE' || role() === 'PHARMACIST') {
            <mat-form-field><mat-label>License number</mat-label><input matInput formControlName="licenseNo" /></mat-form-field>
          }
          @if (role() === 'PATIENT') {
            <div class="full">
              @if (linked()) { <p class="alert-box info">Linked patient: <strong>&nbsp;{{ linked() }}</strong></p> }
              <cc-patient-picker label="Link to patient record" (selected)="link($event)" />
            </div>
          }
          @if (!user) {
            <mat-form-field class="full"><mat-label>Initial password</mat-label><input matInput formControlName="password" type="password" autocomplete="new-password" />
              <mat-hint>8+ characters with a letter and a digit</mat-hint><mat-error>8+ characters with a letter and a digit</mat-error></mat-form-field>
          }
        </div>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancel</button>
        <button mat-flat-button type="submit">Save</button>
      </mat-dialog-actions>
    </form>
  `,
})
export class UserDialog {
  readonly data = inject<{ user: User | null; departments: Department[] }>(MAT_DIALOG_DATA);
  readonly user = this.data.user;
  private readonly api = inject(Api);
  private readonly ui = inject(Ui);
  private readonly ref = inject(MatDialogRef<UserDialog, boolean>);
  readonly roles = ROLES;
  readonly linked = signal(this.user?.patientId ? `Patient #${this.user.patientId}` : '');

  readonly form = inject(FormBuilder).group({
    username: [{ value: this.user?.username ?? '', disabled: !!this.user }, [Validators.required, Validators.pattern(/^[a-zA-Z0-9._-]{3,50}$/)]],
    fullName: [this.user?.fullName ?? '', Validators.required],
    role: [(this.user?.role ?? 'DOCTOR') as Role, Validators.required],
    email: [this.user?.email ?? '', Validators.email],
    phone: [this.user?.phone ?? ''],
    departmentId: [this.user?.departmentId ?? (null as number | null)],
    specialty: [this.user?.specialty ?? ''],
    licenseNo: [this.user?.licenseNo ?? ''],
    patientId: [this.user?.patientId ?? (null as number | null)],
    password: ['', this.user ? [] : [Validators.required, Validators.pattern(PASSWORD_PATTERN)]],
  });
  readonly role = toSignal(this.form.controls.role.valueChanges, { initialValue: this.form.controls.role.value });

  link(p: PatientSummary): void {
    this.form.patchValue({ patientId: p.id, fullName: this.form.value.fullName || p.fullName });
    this.linked.set(`${p.fullName} (${p.mrn})`);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const isPatient = v.role === 'PATIENT';
    if (isPatient && !v.patientId) {
      this.ui.error(new Error('Link the patient account to a patient record.'));
      return;
    }
    const body = {
      ...v, email: v.email || null, phone: v.phone || null, specialty: v.specialty || null, licenseNo: v.licenseNo || null,
      departmentId: isPatient ? null : v.departmentId, patientId: isPatient ? v.patientId : null,
    };
    const req = this.user ? this.api.updateUser(this.user.id, body) : this.api.createUser(body);
    req.subscribe({
      next: () => {
        this.ui.success('User saved.');
        this.ref.close(true);
      },
      error: err => this.ui.error(err),
    });
  }
}

@Component({
  selector: 'cc-users',
  imports: [FormsModule, DatePipe, LabelPipe, StatusChip, MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatMenuModule, MatPaginatorModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div><h1>User accounts</h1><p>Create staff and patient portal accounts, change roles and reset passwords.</p></div>
        <button mat-flat-button (click)="edit()"><mat-icon>person_add</mat-icon>New user</button>
      </div>
      <div class="card filters">
        <mat-form-field subscriptSizing="dynamic"><mat-label>Search</mat-label><mat-icon matPrefix>search</mat-icon><input matInput [(ngModel)]="search" (ngModelChange)="search$.next()" /></mat-form-field>
        <mat-form-field subscriptSizing="dynamic"><mat-label>Role</mat-label>
          <mat-select [(ngModel)]="role" (selectionChange)="load(0)"><mat-option [value]="null">All roles</mat-option>
            @for (r of roles; track r) { <mat-option [value]="r">{{ r | label }}</mat-option> }</mat-select></mat-form-field>
      </div>
      <div class="table-wrap mt">
        <table class="simple">
          <tr><th>User</th><th>Role</th><th>Department</th><th>Last login</th><th>Status</th><th></th></tr>
          @for (u of users(); track u.id) {
            <tr>
              <td><strong>{{ u.fullName }}</strong><div class="muted small mono">{{ u.username }}{{ u.email ? ' · ' + u.email : '' }}</div></td>
              <td><cc-status [value]="u.role" /></td>
              <td>{{ u.departmentName }}{{ u.specialty ? ' · ' + u.specialty : '' }}</td>
              <td>{{ (u.lastLoginAt | date: 'dd MMM yyyy, HH:mm') || 'Never' }}</td>
              <td><span class="chip" [class.success]="u.active">{{ u.active ? 'Active' : 'Disabled' }}</span></td>
              <td class="right">
                <button mat-icon-button [matMenuTriggerFor]="m"><mat-icon>more_vert</mat-icon></button>
                <mat-menu #m="matMenu">
                  <button mat-menu-item (click)="edit(u)"><mat-icon>edit</mat-icon>Edit</button>
                  <button mat-menu-item (click)="reset(u)"><mat-icon>key</mat-icon>Reset password</button>
                  @if (u.id !== myId) {
                    <button mat-menu-item (click)="toggle(u)"><mat-icon>{{ u.active ? 'block' : 'check_circle' }}</mat-icon>{{ u.active ? 'Disable' : 'Enable' }}</button>
                  }
                </mat-menu>
              </td>
            </tr>
          }
        </table>
        <mat-paginator [length]="total()" [pageIndex]="page()" [pageSize]="20" (page)="pageChange($event)" hidePageSize />
      </div>
    </div>
  `,
})
export class Users {
  private readonly api = inject(Api);
  private readonly ui = inject(Ui);
  private readonly dialog = inject(MatDialog);
  readonly myId = inject(AuthService).user()?.id;
  readonly roles = ROLES;
  readonly users = signal<User[]>([]);
  readonly total = signal(0);
  readonly page = signal(0);
  readonly search$ = new Subject<void>();
  private departments: Department[] = [];
  search = '';
  role: Role | null = null;

  constructor() {
    this.load(0);
    this.api.departments().subscribe(d => (this.departments = d));
    this.search$.pipe(debounceTime(300)).subscribe(() => this.load(0));
  }

  load(page: number): void {
    this.page.set(page);
    this.api.users({ search: this.search.trim(), role: this.role, page, size: 20, sort: 'fullName,asc' }).subscribe({
      next: p => {
        this.users.set(p.content);
        this.total.set(p.totalElements);
      },
      error: err => this.ui.error(err),
    });
  }

  pageChange(e: PageEvent): void {
    this.load(e.pageIndex);
  }

  edit(user?: User): void {
    this.dialog.open(UserDialog, { data: { user: user ?? null, departments: this.departments }, width: '680px', maxWidth: '96vw' })
      .afterClosed().subscribe(ok => ok && this.load(this.page()));
  }

  async toggle(u: User): Promise<void> {
    const ok = await this.ui.confirm({
      title: `${u.active ? 'Disable' : 'Enable'} ${u.username}?`,
      message: u.active ? 'The user will be signed out and cannot log in until re-enabled.' : 'The user will be able to sign in again.',
      confirmText: u.active ? 'Disable' : 'Enable', danger: u.active,
    });
    if (!ok) return;
    this.api.setUserActive(u.id, !u.active).subscribe({ next: () => this.load(this.page()), error: err => this.ui.error(err) });
  }

  async reset(u: User): Promise<void> {
    const pwd = await this.ui.reason({ title: `Reset password for ${u.username}`, message: 'Enter a new password (8+ characters with a letter and a digit). The user\'s sessions will be revoked.', reasonLabel: 'New password', confirmText: 'Reset' });
    if (pwd === null) return;
    if (!PASSWORD_PATTERN.test(pwd)) {
      this.ui.error(new Error('Password must be 8-72 characters with a letter and a digit.'));
      return;
    }
    this.api.resetPassword(u.id, pwd).subscribe({ next: () => this.ui.success('Password reset.'), error: err => this.ui.error(err) });
  }
}
