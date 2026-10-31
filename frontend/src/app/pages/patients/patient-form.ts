import { Component, inject, input, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { Router, RouterLink } from '@angular/router';
import { Api } from '../../core/api.service';
import { Patient, Provider } from '../../core/models';
import { apiError, clean, parseDate } from '../../core/util';
import { Ui } from '../../shared/ui.service';

const PHONE = /^[0-9+][0-9 -]{6,19}$/;

/** Register a new patient or edit demographics (optimistic locking via "version"). */
@Component({
  selector: 'cc-patient-form',
  imports: [
    ReactiveFormsModule, RouterLink, MatFormFieldModule, MatInputModule, MatSelectModule, MatDatepickerModule,
    MatButtonModule, MatIconModule, MatProgressBarModule,
  ],
  template: `
    <div class="page narrow">
      <div class="page-header">
        <div>
          <h1>{{ id() ? 'Edit patient' : 'Register new patient' }}</h1>
          <p>{{ id() ? existing()?.fullName + ' · ' + existing()?.mrn : 'An MRN is generated automatically when you save.' }}</p>
        </div>
        <a mat-button [routerLink]="id() ? ['/patients', id()] : ['/patients']"><mat-icon>arrow_back</mat-icon>Back</a>
      </div>

      <form class="card" [formGroup]="form" (ngSubmit)="save()">
        @if (saving()) { <mat-progress-bar mode="indeterminate" /> }
        <div class="form-grid">
          <div class="form-section-title">Identity</div>
          <mat-form-field>
            <mat-label>First name</mat-label>
            <input matInput formControlName="firstName" maxlength="60" />
            <mat-error>First name is required</mat-error>
          </mat-form-field>
          <mat-form-field>
            <mat-label>Last name</mat-label>
            <input matInput formControlName="lastName" maxlength="60" />
            <mat-error>Last name is required</mat-error>
          </mat-form-field>
          <mat-form-field>
            <mat-label>Date of birth</mat-label>
            <input matInput [matDatepicker]="dob" formControlName="dateOfBirth" [max]="today" />
            <mat-datepicker-toggle matSuffix [for]="dob" />
            <mat-datepicker #dob startView="multi-year" />
            <mat-error>Enter a past date</mat-error>
          </mat-form-field>
          <mat-form-field>
            <mat-label>Gender</mat-label>
            <mat-select formControlName="gender">
              <mat-option value="MALE">Male</mat-option>
              <mat-option value="FEMALE">Female</mat-option>
              <mat-option value="OTHER">Other</mat-option>
            </mat-select>
            <mat-error>Gender is required</mat-error>
          </mat-form-field>
          <mat-form-field>
            <mat-label>Blood group</mat-label>
            <mat-select formControlName="bloodGroup">
              <mat-option [value]="''">Unknown</mat-option>
              @for (b of bloodGroups; track b) { <mat-option [value]="b">{{ b }}</mat-option> }
            </mat-select>
          </mat-form-field>
          <mat-form-field>
            <mat-label>Primary doctor</mat-label>
            <mat-select formControlName="primaryProviderId">
              <mat-option [value]="null">None</mat-option>
              @for (p of providers(); track p.id) { <mat-option [value]="p.id">{{ p.fullName }} - {{ p.specialty }}</mat-option> }
            </mat-select>
          </mat-form-field>

          <div class="form-section-title">Contact</div>
          <mat-form-field>
            <mat-label>Phone</mat-label>
            <input matInput formControlName="phone" inputmode="tel" />
            <mat-error>Enter a valid phone number</mat-error>
          </mat-form-field>
          <mat-form-field>
            <mat-label>Email</mat-label>
            <input matInput formControlName="email" type="email" />
            <mat-error>Enter a valid email</mat-error>
          </mat-form-field>
          <mat-form-field class="full">
            <mat-label>Address</mat-label>
            <input matInput formControlName="addressLine" maxlength="200" />
          </mat-form-field>
          <mat-form-field>
            <mat-label>City</mat-label>
            <input matInput formControlName="city" maxlength="60" />
          </mat-form-field>
          <mat-form-field>
            <mat-label>State</mat-label>
            <input matInput formControlName="stateName" maxlength="60" />
          </mat-form-field>
          <mat-form-field>
            <mat-label>Postal code</mat-label>
            <input matInput formControlName="postalCode" maxlength="12" />
          </mat-form-field>

          <div class="form-section-title">Emergency contact</div>
          <mat-form-field>
            <mat-label>Name</mat-label>
            <input matInput formControlName="emergencyName" maxlength="120" />
          </mat-form-field>
          <mat-form-field>
            <mat-label>Phone</mat-label>
            <input matInput formControlName="emergencyPhone" />
            <mat-error>Enter a valid phone number</mat-error>
          </mat-form-field>
          <mat-form-field>
            <mat-label>Relation</mat-label>
            <input matInput formControlName="emergencyRelation" maxlength="40" />
          </mat-form-field>

          <div class="form-section-title">Insurance</div>
          <mat-form-field>
            <mat-label>Provider</mat-label>
            <input matInput formControlName="insuranceProvider" maxlength="100" />
          </mat-form-field>
          <mat-form-field>
            <mat-label>Policy number</mat-label>
            <input matInput formControlName="insurancePolicyNo" maxlength="50" />
          </mat-form-field>
          <mat-form-field>
            <mat-label>Valid till</mat-label>
            <input matInput [matDatepicker]="ins" formControlName="insuranceValidTill" />
            <mat-datepicker-toggle matSuffix [for]="ins" />
            <mat-datepicker #ins />
          </mat-form-field>

          <mat-form-field class="full">
            <mat-label>Past medical history (free text)</mat-label>
            <textarea matInput rows="3" formControlName="medicalHistory" maxlength="4000"></textarea>
          </mat-form-field>
        </div>
        <div class="actions end">
          <a mat-button [routerLink]="id() ? ['/patients', id()] : ['/patients']">Cancel</a>
          <button mat-flat-button type="submit" [disabled]="saving()"><mat-icon>save</mat-icon>{{ id() ? 'Save changes' : 'Register patient' }}</button>
        </div>
      </form>
    </div>
  `,
  styles: `.narrow { max-width: 1000px; } .end { justify-content: flex-end; margin-top: 8px; }`,
})
export class PatientForm implements OnInit {
  private readonly api = inject(Api);
  private readonly ui = inject(Ui);
  private readonly router = inject(Router);

  /** Route parameter (absent when registering). */
  readonly id = input<string>();
  readonly existing = signal<Patient | null>(null);
  readonly providers = signal<Provider[]>([]);
  readonly saving = signal(false);
  readonly today = new Date();
  readonly bloodGroups = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];

  readonly form = inject(FormBuilder).group({
    firstName: ['', [Validators.required, Validators.maxLength(60)]],
    lastName: ['', [Validators.required, Validators.maxLength(60)]],
    dateOfBirth: [null as Date | null, Validators.required],
    gender: ['', Validators.required],
    bloodGroup: [''],
    phone: ['', [Validators.required, Validators.pattern(PHONE)]],
    email: ['', Validators.email],
    addressLine: [''],
    city: [''],
    stateName: [''],
    postalCode: [''],
    emergencyName: [''],
    emergencyPhone: ['', Validators.pattern(PHONE)],
    emergencyRelation: [''],
    insuranceProvider: [''],
    insurancePolicyNo: [''],
    insuranceValidTill: [null as Date | null],
    medicalHistory: [''],
    primaryProviderId: [null as number | null],
  });

  ngOnInit(): void {
    this.api.providers().subscribe(p => this.providers.set(p));
    const id = this.id();
    if (id) {
      this.api.patient(+id).subscribe({
        next: p => {
          this.existing.set(p);
          this.form.patchValue({
            ...p,
            bloodGroup: p.bloodGroup ?? '',
            dateOfBirth: parseDate(p.dateOfBirth),
            insuranceValidTill: parseDate(p.insuranceValidTill),
            primaryProviderId: p.primaryProviderId ?? null,
          } as never);
        },
        error: err => this.ui.error(err),
      });
    }
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.ui.error(new Error('Please correct the highlighted fields.'));
      return;
    }
    const body = clean({ ...this.form.getRawValue(), version: this.existing()?.version });
    this.saving.set(true);
    const id = this.id();
    const request = id ? this.api.updatePatient(+id, body) : this.api.registerPatient(body);
    request.subscribe({
      next: p => {
        this.ui.success(id ? 'Patient details updated.' : `Patient registered with MRN ${p.mrn}.`);
        this.router.navigate(['/patients', p.id]);
      },
      error: err => {
        this.saving.set(false);
        this.ui.error(err);
        // Show server-side field errors next to the fields.
        apiError(err)?.fieldErrors?.forEach(f => this.form.get(f.field)?.setErrors({ server: f.message }));
      },
    });
  }
}
