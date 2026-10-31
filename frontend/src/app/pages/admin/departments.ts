import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Api } from '../../core/api.service';
import { Department } from '../../core/models';
import { Ui } from '../../shared/ui.service';

@Component({
  selector: 'cc-departments',
  imports: [FormsModule, MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule, MatCheckboxModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div><h1>Departments</h1><p>Clinical departments that doctors and encounters belong to.</p></div>
        <button mat-flat-button (click)="edit(null)"><mat-icon>add</mat-icon>New department</button>
      </div>
      @if (form) {
        <div class="card mb">
          <div class="form-grid">
            <mat-form-field><mat-label>Code</mat-label><input matInput [(ngModel)]="form.code" placeholder="CARDIO" /></mat-form-field>
            <mat-form-field><mat-label>Name</mat-label><input matInput [(ngModel)]="form.name" /></mat-form-field>
            <mat-form-field class="full"><mat-label>Description</mat-label><input matInput [(ngModel)]="form.description" /></mat-form-field>
            <mat-checkbox [(ngModel)]="form.active">Active</mat-checkbox>
          </div>
          <div class="actions"><button mat-button (click)="form = null">Cancel</button><button mat-flat-button (click)="save()">Save</button></div>
        </div>
      }
      <div class="grid grid-3">
        @for (d of departments(); track d.id) {
          <div class="card dept" [class.off]="!d.active">
            <div class="card-title"><span>{{ d.name }}</span><button mat-icon-button (click)="edit(d)"><mat-icon>edit</mat-icon></button></div>
            <span class="chip primary mono">{{ d.code }}</span> @if (!d.active) { <span class="chip">Inactive</span> }
            <p class="muted">{{ d.description }}</p>
          </div>
        }
      </div>
    </div>
  `,
  styles: `.dept.off { opacity: .6; }`,
})
export class Departments {
  private readonly api = inject(Api);
  private readonly ui = inject(Ui);
  readonly departments = signal<Department[]>([]);
  form: { id?: number; code: string; name: string; description: string; active: boolean } | null = null;

  constructor() {
    this.load();
  }

  load(): void {
    this.api.departments().subscribe(d => this.departments.set(d));
  }

  edit(d: Department | null): void {
    this.form = d ? { id: d.id, code: d.code, name: d.name, description: d.description ?? '', active: d.active } : { code: '', name: '', description: '', active: true };
  }

  save(): void {
    const f = this.form!;
    this.api.saveDepartment({ code: f.code.trim().toUpperCase(), name: f.name.trim(), description: f.description || null, active: f.active }, f.id).subscribe({
      next: () => {
        this.ui.success('Department saved.');
        this.form = null;
        this.load();
      },
      error: err => this.ui.error(err),
    });
  }
}
