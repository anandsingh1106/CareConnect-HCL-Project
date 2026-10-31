import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';
import { ChartConfiguration } from 'chart.js';
import { Api } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { Dashboard } from '../core/models';
import { CHART_COLORS, ChartComponent } from '../shared/chart';
import { Ui } from '../shared/ui.service';

interface QuickAction { label: string; icon: string; link: string; }

const QUICK: Record<string, QuickAction[]> = {
  DOCTOR: [
    { label: 'Find patient', icon: 'person_search', link: '/patients' },
    { label: 'My encounters', icon: 'clinical_notes', link: '/encounters' },
    { label: 'Orders & results', icon: 'science', link: '/orders' },
    { label: 'Messages', icon: 'forum', link: '/messages' },
  ],
  NURSE: [
    { label: 'Medication round (MAR)', icon: 'medication', link: '/nursing' },
    { label: 'Open encounters', icon: 'clinical_notes', link: '/encounters' },
    { label: 'Enter lab results', icon: 'science', link: '/orders' },
    { label: 'Find patient', icon: 'person_search', link: '/patients' },
  ],
  PHARMACIST: [
    { label: 'Verification queue', icon: 'fact_check', link: '/pharmacy' },
    { label: 'Drug inventory', icon: 'inventory_2', link: '/pharmacy/inventory' },
    { label: 'Find patient', icon: 'person_search', link: '/patients' },
  ],
  RECEPTIONIST: [
    { label: 'Register patient', icon: 'person_add', link: '/patients/new' },
    { label: 'Book appointment', icon: 'calendar_add_on', link: '/appointments' },
    { label: 'Find patient', icon: 'person_search', link: '/patients' },
  ],
  ADMIN: [
    { label: 'Manage users', icon: 'manage_accounts', link: '/admin/users' },
    { label: 'Master data', icon: 'dataset', link: '/admin/master-data' },
    { label: 'Audit log', icon: 'policy', link: '/admin/audit' },
    { label: 'Drug inventory', icon: 'inventory_2', link: '/pharmacy/inventory' },
  ],
  PATIENT: [
    { label: 'Book an appointment', icon: 'calendar_add_on', link: '/portal/appointments' },
    { label: 'My medicines & results', icon: 'favorite', link: '/portal/health' },
    { label: 'Message my doctor', icon: 'forum', link: '/portal/messages' },
  ],
};

@Component({
  selector: 'cc-dashboard',
  imports: [RouterLink, MatIconModule, MatButtonModule, MatProgressSpinnerModule, ChartComponent],
  template: `
    <div class="page">
      <div class="hello">
        <div>
          <p class="muted">{{ greeting }}</p>
          <h1>{{ auth.user()?.fullName }}</h1>
          <p class="muted">{{ today }}{{ auth.user()?.departmentName ? ' · ' + auth.user()?.departmentName : '' }}</p>
        </div>
        <div class="quick">
          @for (q of quick(); track q.link) {
            <a mat-stroked-button [routerLink]="q.link"><mat-icon>{{ q.icon }}</mat-icon>{{ q.label }}</a>
          }
        </div>
      </div>

      @if (data(); as d) {
        <div class="grid grid-4 cards">
          @for (c of d.cards; track c.key) {
            <a class="stat card" [class]="'stat card ' + c.tone" [routerLink]="c.link || null">
              <span class="icon"><mat-icon>{{ c.icon }}</mat-icon></span>
              <div>
                <div class="value">{{ c.value }}</div>
                <div class="label">{{ c.label }}</div>
              </div>
            </a>
          }
        </div>

        <div class="grid grid-2 mt">
          @for (chart of charts(); track chart.key) {
            <section class="card">
              <h3 class="card-title">{{ chart.title }}</h3>
              @if (chart.empty) {
                <div class="empty"><mat-icon>insights</mat-icon>No data yet</div>
              } @else {
                <cc-chart [config]="chart.config" height="260px" />
              }
            </section>
          }
          @if (d.workList; as wl) {
            <section class="card">
              <h3 class="card-title">{{ wl.title }}</h3>
              @for (item of wl.items; track $index) {
                <a class="work" [routerLink]="item.link || null">
                  <span class="dot"></span>
                  <div class="w-main">
                    <div class="w-title">{{ item.title }}</div>
                    @if (item.subtitle) { <div class="muted small">{{ item.subtitle }}</div> }
                  </div>
                  @if (item.meta) { <span class="chip primary">{{ item.meta }}</span> }
                </a>
              } @empty {
                <div class="empty"><mat-icon>task_alt</mat-icon>Nothing waiting for you.</div>
              }
            </section>
          }
        </div>
      } @else if (loading()) {
        <div class="center-spinner"><mat-spinner diameter="42" /></div>
      }
    </div>
  `,
  styles: `
    .hello { display: flex; justify-content: space-between; align-items: flex-end; gap: 16px; flex-wrap: wrap; margin-bottom: 22px;
      h1 { margin: 2px 0; font-size: 1.8rem; letter-spacing: -.02em; } p { margin: 0; } }
    .quick { display: flex; gap: 8px; flex-wrap: wrap; }
    .stat { display: flex; gap: 16px; align-items: center; text-decoration: none; color: inherit; transition: transform .15s, box-shadow .15s;
      &:hover { transform: translateY(-2px); box-shadow: 0 10px 28px rgba(16,38,64,.1); } }
    .icon { width: 50px; height: 50px; border-radius: 14px; display: grid; place-items: center; background: #e3eefa; color: var(--cc-primary); flex: none; }
    .success .icon { background: #e3f4ea; color: var(--cc-success); }
    .warning .icon { background: #fff1dc; color: var(--cc-warning); }
    .danger .icon { background: #fde7e7; color: var(--cc-danger); }
    .info .icon { background: #e1f1fb; color: var(--cc-info); }
    .value { font-size: 1.8rem; font-weight: 800; line-height: 1; }
    .label { color: var(--cc-muted); font-size: .88rem; margin-top: 4px; }
    .work { display: flex; align-items: center; gap: 12px; padding: 10px 8px; border-radius: 10px; text-decoration: none; color: inherit;
      &:hover { background: #f2f7fd; } }
    .dot { width: 8px; height: 8px; border-radius: 50%; background: var(--cc-teal); flex: none; }
    .w-main { flex: 1; min-width: 0; } .w-title { font-weight: 600; }
  `,
})
export class DashboardPage {
  private readonly api = inject(Api);
  private readonly ui = inject(Ui);
  readonly auth = inject(AuthService);

  readonly data = signal<Dashboard | null>(null);
  readonly loading = signal(true);
  readonly quick = computed(() => QUICK[this.auth.role() ?? ''] ?? []);
  readonly today = new Date().toLocaleDateString('en-GB', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
  readonly greeting = (() => {
    const h = new Date().getHours();
    return h < 12 ? 'Good morning,' : h < 17 ? 'Good afternoon,' : 'Good evening,';
  })();

  readonly charts = computed(() => (this.data()?.charts ?? []).map(c => {
    const round = c.type === 'doughnut' || c.type === 'pie';
    const config: ChartConfiguration = {
      type: c.type,
      data: {
        labels: c.labels,
        datasets: c.series.map((s, i) => ({
          label: s.name,
          data: s.data,
          backgroundColor: round ? CHART_COLORS : CHART_COLORS[i % CHART_COLORS.length],
          borderRadius: round ? 0 : 6,
          borderWidth: round ? 2 : 0,
        })),
      },
      options: {
        plugins: { legend: { position: round ? 'right' : 'top', display: round || c.series.length > 1 } },
        scales: round ? {} : { y: { beginAtZero: true, ticks: { precision: 0 } } },
        ...(round ? { cutout: '62%' } : {}),
      } as ChartConfiguration['options'],
    };
    const empty = c.series.every(s => s.data.every(v => !v));
    return { key: c.key, title: c.title, config, empty };
  }));

  constructor() {
    this.api.dashboard().subscribe({
      next: d => {
        this.data.set(d);
        this.loading.set(false);
      },
      error: err => {
        this.loading.set(false);
        this.ui.error(err);
      },
    });
  }
}
