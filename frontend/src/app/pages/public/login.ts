import { Component, inject, input, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Router, RouterLink } from '@angular/router';
import { AuthService, HOME_ROUTE } from '../../core/auth.service';
import { errorMessage } from '../../core/util';

export const DEMO_PASSWORD = 'Password@123';
export const DEMO_ACCOUNTS = [
  { username: 'dr.mehta', role: 'Doctor', icon: 'stethoscope' },
  { username: 'nurse.priya', role: 'Nurse', icon: 'vaccines' },
  { username: 'pharm.arjun', role: 'Pharmacist', icon: 'local_pharmacy' },
  { username: 'recep.neha', role: 'Receptionist', icon: 'support_agent' },
  { username: 'admin', role: 'Administrator', icon: 'admin_panel_settings' },
  { username: 'patient.ravi', role: 'Patient', icon: 'person' },
];

@Component({
  selector: 'cc-login',
  imports: [ReactiveFormsModule, RouterLink, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule, MatProgressBarModule],
  template: `
    <div class="split">
      <aside class="art">
        <a class="brand" routerLink="/"><span class="logo"><mat-icon>ecg_heart</mat-icon></span> Care<b>Connect</b></a>
        <div class="art-text">
          <h1>Welcome back.</h1>
          <p>Sign in to access patient charts, orders, prescriptions and your daily worklist.</p>
          <ul>
            <li><mat-icon>lock</mat-icon> Encrypted sessions with auto-renewing tokens</li>
            <li><mat-icon>policy</mat-icon> Every chart access is audited</li>
            <li><mat-icon>shield_with_heart</mat-icon> Built-in clinical safety checks</li>
          </ul>
        </div>
        <small>Demo environment &middot; no real patient data</small>
      </aside>

      <main class="panel">
        <form class="card form" [formGroup]="form" (ngSubmit)="submit()">
          @if (loading()) { <mat-progress-bar mode="indeterminate" class="bar" /> }
          <h2>Sign in</h2>
          <p class="muted">Use your CareConnect username and password.</p>

          @if (expired()) {
            <div class="alert-box warning"><mat-icon>schedule</mat-icon> Your session has expired. Please sign in again.</div>
          }
          @if (error()) {
            <div class="alert-box danger"><mat-icon>error</mat-icon> {{ error() }}</div>
          }

          <mat-form-field>
            <mat-label>Username</mat-label>
            <mat-icon matPrefix>person</mat-icon>
            <input matInput formControlName="username" autocomplete="username" />
            @if (form.controls.username.hasError('required')) { <mat-error>Username is required</mat-error> }
          </mat-form-field>

          <mat-form-field>
            <mat-label>Password</mat-label>
            <mat-icon matPrefix>key</mat-icon>
            <input matInput [type]="showPassword() ? 'text' : 'password'" formControlName="password" autocomplete="current-password" />
            <button mat-icon-button matSuffix type="button" (click)="showPassword.set(!showPassword())"
                    [attr.aria-label]="showPassword() ? 'Hide password' : 'Show password'">
              <mat-icon>{{ showPassword() ? 'visibility_off' : 'visibility' }}</mat-icon>
            </button>
            @if (form.controls.password.hasError('required')) { <mat-error>Password is required</mat-error> }
          </mat-form-field>

          <button mat-flat-button class="submit" type="submit" [disabled]="loading()">
            <mat-icon>login</mat-icon> Sign in
          </button>

          <div class="demo">
            <div class="demo-title">Demo accounts <span class="muted">(password {{ demoPassword }})</span></div>
            <div class="demo-list">
              @for (a of accounts; track a.username) {
                <button type="button" class="demo-btn" [class.sel]="form.value.username === a.username" (click)="fill(a.username)">
                  <mat-icon>{{ a.icon }}</mat-icon> {{ a.role }}
                </button>
              }
            </div>
          </div>
        </form>
      </main>
    </div>
  `,
  styles: `
    .split { display: grid; grid-template-columns: 1fr 1.1fr; min-height: 100vh; }
    .art { background: radial-gradient(700px 400px at 100% 0, rgba(14,159,142,.4), transparent 60%), linear-gradient(160deg, #081b33, #0b3c73);
      color: #fff; padding: 32px 48px; display: flex; flex-direction: column; justify-content: space-between; }
    .brand { display: flex; align-items: center; gap: 10px; color: #fff; text-decoration: none; font-size: 1.35rem; b { color: #5ec8ff; } }
    .logo { width: 38px; height: 38px; border-radius: 11px; display: grid; place-items: center; background: linear-gradient(135deg, #1a8cff, #0e9f8e); }
    .art-text h1 { font-size: 2.6rem; margin: 0 0 12px; letter-spacing: -.02em; }
    .art-text p { color: #c3d3e6; font-size: 1.05rem; line-height: 1.6; max-width: 420px; }
    .art-text ul { list-style: none; padding: 0; display: grid; gap: 12px; margin-top: 28px; }
    .art-text li { display: flex; gap: 10px; align-items: center; color: #dbe7f4; mat-icon { color: #3ee0c5; } }
    .art small { color: #7f93ab; }
    .panel { display: grid; place-items: center; padding: 32px 20px; }
    .form { width: min(440px, 100%); padding: 32px; position: relative; overflow: hidden; }
    .bar { position: absolute; top: 0; left: 0; right: 0; }
    h2 { margin: 0; font-size: 1.7rem; }
    .muted { margin: 4px 0 20px; }
    .submit { width: 100%; height: 48px; font-size: 1rem; margin-top: 4px; }
    .demo { margin-top: 24px; padding-top: 18px; border-top: 1px dashed var(--cc-border); }
    .demo-title { font-weight: 600; font-size: .88rem; margin-bottom: 10px; .muted { font-weight: 400; } }
    .demo-list { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; }
    .demo-btn { display: flex; align-items: center; gap: 8px; padding: 9px 12px; border-radius: 10px; border: 1px solid var(--cc-border);
      background: #fff; cursor: pointer; font: inherit; font-size: .88rem; color: var(--cc-text); text-align: left;
      mat-icon { color: var(--cc-primary); font-size: 20px; width: 20px; height: 20px; }
      &:hover { border-color: var(--cc-primary); } &.sel { border-color: var(--cc-primary); background: #eef5fd; } }
    @media (max-width: 860px) { .split { grid-template-columns: 1fr; } .art { display: none; } }
  `,
})
export class Login implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** Query parameters (bound via withComponentInputBinding). */
  readonly returnUrl = input<string>();
  readonly as = input<string>();
  readonly expired = input<string>();

  readonly accounts = DEMO_ACCOUNTS;
  readonly demoPassword = DEMO_PASSWORD;
  readonly loading = signal(false);
  readonly error = signal('');
  readonly showPassword = signal(false);

  readonly form = inject(FormBuilder).nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });

  ngOnInit(): void {
    const preset = this.as();
    if (preset) this.fill(preset);
  }

  fill(username: string): void {
    this.form.setValue({ username, password: DEMO_PASSWORD });
    this.error.set('');
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set('');
    const { username, password } = this.form.getRawValue();
    this.auth.login(username.trim(), password).subscribe({
      next: () => {
        const target = this.returnUrl();
        this.router.navigateByUrl(target && target.startsWith('/') && !target.startsWith('/login') ? target : HOME_ROUTE);
      },
      error: err => {
        this.loading.set(false);
        this.error.set(errorMessage(err));
      },
    });
  }
}
