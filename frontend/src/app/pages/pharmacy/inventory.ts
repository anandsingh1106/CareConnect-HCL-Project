import { CurrencyPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject, debounceTime } from 'rxjs';
import { Api } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Drug, Interaction } from '../../core/models';
import { StatusChip } from '../../shared/status-chip';
import { Ui } from '../../shared/ui.service';
import { DrugDialog, InteractionDialog, StockDialog } from './inventory-dialogs';

@Component({
  selector: 'cc-inventory',
  imports: [
    FormsModule, CurrencyPipe, StatusChip, MatTabsModule, MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule,
    MatPaginatorModule, MatProgressBarModule, MatSlideToggleModule, MatTooltipModule,
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div><h1>Drug inventory</h1><p>Formulary, stock levels and the interaction table used by decision support.</p></div>
        <button mat-flat-button (click)="editDrug()"><mat-icon>add</mat-icon>Add drug</button>
      </div>

      @if (low().length) {
        <div class="alert-box warning"><mat-icon>inventory_2</mat-icon>
          <div><strong>{{ low().length }} drug(s) at or below reorder level:</strong>
            @for (d of low(); track d.id) { <span class="chip warning lowchip">{{ d.name }} ({{ d.stockQty }})</span> }</div></div>
      }

      <mat-tab-group mat-stretch-tabs="false" (selectedIndexChange)="$event === 1 && loadInteractions()">
        <mat-tab label="Stock">
          <div class="filters card mt">
            <mat-form-field subscriptSizing="dynamic"><mat-label>Search drugs</mat-label><mat-icon matPrefix>search</mat-icon>
              <input matInput [(ngModel)]="q" (ngModelChange)="search$.next()" /></mat-form-field>
            <mat-slide-toggle [(ngModel)]="includeInactive" (change)="load(0)">Show inactive</mat-slide-toggle>
          </div>
          <div class="table-wrap mt">
            @if (loading()) { <mat-progress-bar mode="indeterminate" /> }
            <table class="simple">
              <tr><th>Code</th><th>Drug</th><th>Class</th><th>Form</th><th class="right">Price</th><th class="right">Stock</th><th>Status</th><th></th></tr>
              @for (d of drugs(); track d.id) {
                <tr [class.inactive]="!d.active">
                  <td class="mono">{{ d.code }}</td>
                  <td><strong>{{ d.name }}</strong><div class="muted small">{{ d.genericName }}</div></td>
                  <td>{{ d.drugClass }}</td><td>{{ d.strength }} {{ d.dosageForm }}</td>
                  <td class="right">{{ d.unitPrice | currency: 'INR' }}</td>
                  <td class="right"><strong [class.lowtxt]="d.lowStock">{{ d.stockQty }}</strong><div class="muted small">min {{ d.reorderLevel }}</div></td>
                  <td>@if (d.lowStock) { <span class="chip warning">Low stock</span> } @else if (!d.active) { <cc-status value="INACTIVE" /> } @else { <span class="chip success">OK</span> }</td>
                  <td class="right nowrap">
                    <button mat-icon-button matTooltip="Stock in / adjust" (click)="stock(d)"><mat-icon>move_to_inbox</mat-icon></button>
                    <button mat-icon-button matTooltip="Edit" (click)="editDrug(d)"><mat-icon>edit</mat-icon></button>
                  </td>
                </tr>
              }
            </table>
            <mat-paginator [length]="total()" [pageIndex]="page()" [pageSize]="20" (page)="pageChange($event)" hidePageSize />
          </div>
        </mat-tab>
        <mat-tab label="Drug interactions">
          <div class="actions mt mb"><button mat-stroked-button (click)="addInteraction()"><mat-icon>add</mat-icon>Add interaction</button></div>
          <div class="table-wrap">
            <table class="simple">
              <tr><th>Drug A</th><th>Drug B</th><th>Severity</th><th>Effect</th><th></th></tr>
              @for (i of interactions(); track i.id) {
                <tr><td><strong>{{ i.drugAName }}</strong></td><td><strong>{{ i.drugBName }}</strong></td><td><cc-status [value]="i.severity" /></td><td>{{ i.description }}</td>
                  <td class="right">@if (isAdmin) { <button mat-icon-button matTooltip="Delete" (click)="deleteInteraction(i)"><mat-icon>delete</mat-icon></button> }</td></tr>
              }
            </table>
          </div>
        </mat-tab>
      </mat-tab-group>
    </div>
  `,
  styles: `.lowchip { margin: 2px 4px; } .lowtxt { color: var(--cc-danger); } tr.inactive td { opacity: .55; }`,
})
export class Inventory {
  private readonly api = inject(Api);
  private readonly ui = inject(Ui);
  private readonly dialog = inject(MatDialog);
  readonly isAdmin = inject(AuthService).hasRole('ADMIN');
  readonly drugs = signal<Drug[]>([]);
  readonly low = signal<Drug[]>([]);
  readonly interactions = signal<Interaction[]>([]);
  readonly total = signal(0);
  readonly page = signal(0);
  readonly loading = signal(false);
  readonly search$ = new Subject<void>();
  q = '';
  includeInactive = false;

  constructor() {
    this.load(0);
    this.search$.pipe(debounceTime(300)).subscribe(() => this.load(0));
  }

  load(page: number): void {
    this.loading.set(true);
    this.page.set(page);
    this.api.drugs({ q: this.q.trim(), activeOnly: !this.includeInactive, page, size: 20 }).subscribe({
      next: p => {
        this.drugs.set(p.content);
        this.total.set(p.totalElements);
        this.loading.set(false);
      },
      error: err => {
        this.loading.set(false);
        this.ui.error(err);
      },
    });
    this.api.lowStock().subscribe(l => this.low.set(l));
  }

  pageChange(e: PageEvent): void {
    this.load(e.pageIndex);
  }

  loadInteractions(): void {
    this.api.interactions().subscribe(i => this.interactions.set(i));
  }

  editDrug(d?: Drug): void {
    this.dialog.open(DrugDialog, { data: d ?? null, width: '720px', maxWidth: '96vw' }).afterClosed().subscribe(ok => ok && this.load(this.page()));
  }

  stock(d: Drug): void {
    this.dialog.open(StockDialog, { data: d, maxWidth: '96vw' }).afterClosed().subscribe(changed => changed && this.load(this.page()));
  }

  addInteraction(): void {
    this.dialog.open(InteractionDialog, { width: '640px', maxWidth: '96vw' }).afterClosed().subscribe(ok => ok && this.loadInteractions());
  }

  async deleteInteraction(i: Interaction): Promise<void> {
    if (!(await this.ui.confirm({ title: 'Delete interaction?', message: `${i.drugAName} + ${i.drugBName} will no longer trigger an alert.`, confirmText: 'Delete', danger: true }))) return;
    this.api.deleteInteraction(i.id).subscribe({ next: () => this.loadInteractions(), error: err => this.ui.error(err) });
  }
}
