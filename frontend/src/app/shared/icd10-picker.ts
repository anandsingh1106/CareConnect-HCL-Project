import { Component, inject, input, output } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { catchError, debounceTime, distinctUntilChanged, map, of, switchMap } from 'rxjs';
import { Api } from '../core/api.service';
import { Icd10 } from '../core/models';

/** ICD-10 type-ahead: search by code prefix ("E11") or words ("diabetes"). */
@Component({
  selector: 'cc-icd10-picker',
  imports: [ReactiveFormsModule, MatAutocompleteModule, MatFormFieldModule, MatInputModule],
  template: `
    <mat-form-field class="w" [subscriptSizing]="dense() ? 'dynamic' : 'fixed'">
      <mat-label>{{ label() }}</mat-label>
      <input matInput [formControl]="query" [matAutocomplete]="auto" placeholder="Code or description" />
      <mat-autocomplete #auto="matAutocomplete" [displayWith]="display" (optionSelected)="selected.emit($event.option.value)">
        @for (c of results(); track c.code) {
          <mat-option [value]="c"><span class="mono">{{ c.code }}</span> &nbsp;{{ c.description }}</mat-option>
        }
      </mat-autocomplete>
    </mat-form-field>
  `,
  styles: `.w { width: 100%; }`,
})
export class Icd10Picker {
  private readonly api = inject(Api);
  readonly label = input('ICD-10 diagnosis');
  readonly dense = input(false);
  readonly selected = output<Icd10>();
  readonly query = new FormControl<string | Icd10>('');

  readonly results = toSignal(this.query.valueChanges.pipe(
    map(v => (typeof v === 'string' ? v.trim() : '')),
    debounceTime(250),
    distinctUntilChanged(),
    switchMap(q => (q.length < 2 ? of([]) : this.api.icd10(q).pipe(map(p => p.content), catchError(() => of([])))))),
    { initialValue: [] as Icd10[] });

  display = (c: Icd10 | string | null) => (c && typeof c === 'object' ? `${c.code} - ${c.description}` : (c ?? ''));

  clear(): void {
    this.query.setValue('');
  }
}
