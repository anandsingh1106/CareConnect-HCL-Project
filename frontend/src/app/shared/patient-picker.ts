import { Component, inject, input, output, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { catchError, debounceTime, distinctUntilChanged, map, of, switchMap } from 'rxjs';
import { Api } from '../core/api.service';
import { PatientSummary } from '../core/models';

/** Type-ahead patient search (name, MRN or phone). Emits the chosen patient. */
@Component({
  selector: 'cc-patient-picker',
  imports: [ReactiveFormsModule, MatAutocompleteModule, MatFormFieldModule, MatInputModule, MatIconModule],
  template: `
    <mat-form-field appearance="outline" class="picker" subscriptSizing="dynamic">
      <mat-label>{{ label() }}</mat-label>
      <mat-icon matPrefix>person_search</mat-icon>
      <input matInput [formControl]="query" [matAutocomplete]="auto" placeholder="Name, MRN or phone" />
      <mat-autocomplete #auto="matAutocomplete" [displayWith]="display" (optionSelected)="pick($event.option.value)">
        @for (p of results(); track p.id) {
          <mat-option [value]="p">
            <strong>{{ p.fullName }}</strong>
            <span class="muted small"> &middot; {{ p.mrn }} &middot; {{ p.age }}y {{ p.gender.charAt(0) }}{{ p.phone ? ' · ' + p.phone : '' }}</span>
          </mat-option>
        }
        @if (searched() && !results().length) {
          <mat-option disabled>No matching patients</mat-option>
        }
      </mat-autocomplete>
    </mat-form-field>
  `,
  styles: `.picker { width: 100%; }`,
})
export class PatientPicker {
  private readonly api = inject(Api);
  readonly label = input('Find patient');
  readonly selected = output<PatientSummary>();
  readonly query = new FormControl<string | PatientSummary>('');
  readonly searched = signal(false);

  readonly results = toSignal(this.query.valueChanges.pipe(
    map(v => (typeof v === 'string' ? v.trim() : '')),
    debounceTime(250),
    distinctUntilChanged(),
    switchMap(q => {
      if (q.length < 2) {
        this.searched.set(false);
        return of([] as PatientSummary[]);
      }
      const isMrn = /^cc-/i.test(q);
      return this.api.searchPatients({ [isMrn ? 'mrn' : 'q']: q, size: 10 }).pipe(
        map(page => {
          this.searched.set(true);
          return page.content;
        }),
        catchError(() => of([] as PatientSummary[])));
    })), { initialValue: [] as PatientSummary[] });

  display = (p: PatientSummary | string | null) => (p && typeof p === 'object' ? `${p.fullName} (${p.mrn})` : (p ?? ''));

  pick(p: PatientSummary): void {
    this.selected.emit(p);
  }
}
