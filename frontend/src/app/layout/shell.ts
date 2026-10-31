import { BreakpointObserver } from '@angular/cdk/layout';
import { DatePipe } from '@angular/common';
import { Component, computed, DestroyRef, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatBadgeModule } from '@angular/material/badge';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { catchError, filter, map, of, startWith, switchMap, timer } from 'rxjs';
import { Api } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { AppNotification, Role } from '../core/models';
import { LabelPipe } from '../shared/status-chip';
import { ChangePasswordDialog } from './change-password';

interface NavItem {
  label: string;
  icon: string;
  link: string;
  roles: Role[];
  exact?: boolean;
}

interface NavGroup {
  title?: string;
  items: NavItem[];
}

const STAFF: Role[] = ['ADMIN', 'DOCTOR', 'NURSE', 'PHARMACIST', 'RECEPTIONIST'];

const NAV: NavGroup[] = [
  { items: [{ label: 'Dashboard', icon: 'space_dashboard', link: '/dashboard', roles: [...STAFF, 'PATIENT'] }] },
  {
    title: 'Clinical',
    items: [
      { label: 'Patients', icon: 'groups', link: '/patients', roles: STAFF },
      { label: 'Encounters', icon: 'clinical_notes', link: '/encounters', roles: ['DOCTOR', 'NURSE'] },
      { label: 'Orders & results', icon: 'science', link: '/orders', roles: ['DOCTOR', 'NURSE', 'PHARMACIST'] },
      { label: 'Medication (MAR)', icon: 'medication', link: '/nursing', roles: ['NURSE', 'DOCTOR'] },
      { label: 'Appointments', icon: 'calendar_month', link: '/appointments', roles: ['RECEPTIONIST', 'DOCTOR', 'NURSE', 'ADMIN'] },
      { label: 'Messages', icon: 'forum', link: '/messages', roles: ['DOCTOR'] },
    ],
  },
  {
    title: 'Pharmacy',
    items: [
      { label: 'Verify & dispense', icon: 'local_pharmacy', link: '/pharmacy', roles: ['PHARMACIST'], exact: true },
      { label: 'Drug inventory', icon: 'inventory_2', link: '/pharmacy/inventory', roles: ['PHARMACIST', 'ADMIN'] },
    ],
  },
  {
    title: 'My health',
    items: [
      { label: 'Health record', icon: 'favorite', link: '/portal/health', roles: ['PATIENT'] },
      { label: 'Appointments', icon: 'event', link: '/portal/appointments', roles: ['PATIENT'] },
      { label: 'Messages', icon: 'forum', link: '/portal/messages', roles: ['PATIENT'] },
    ],
  },
  {
    title: 'Administration',
    items: [
      { label: 'Users', icon: 'manage_accounts', link: '/admin/users', roles: ['ADMIN'] },
      { label: 'Departments', icon: 'apartment', link: '/admin/departments', roles: ['ADMIN'] },
      { label: 'Master data', icon: 'dataset', link: '/admin/master-data', roles: ['ADMIN'] },
      { label: 'Audit log', icon: 'policy', link: '/admin/audit', roles: ['ADMIN'] },
    ],
  },
];

@Component({
  selector: 'cc-shell',
  imports: [
    RouterOutlet, RouterLink, RouterLinkActive, DatePipe, LabelPipe, MatSidenavModule, MatToolbarModule, MatIconModule,
    MatButtonModule, MatListModule, MatMenuModule, MatBadgeModule, MatDividerModule, MatTooltipModule,
  ],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  private readonly api = inject(Api);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  readonly auth = inject(AuthService);

  readonly isMobile = toSignal(inject(BreakpointObserver).observe('(max-width: 960px)').pipe(map(r => r.matches)), { initialValue: false });
  readonly navOpen = signal(true);
  readonly notifications = signal<AppNotification[]>([]);
  readonly unread = signal(0);
  readonly darkMode = signal(false);

  readonly nav = computed(() => {
    const role = this.auth.role();
    return NAV.map(g => ({ ...g, items: g.items.filter(i => role && i.roles.includes(role)) })).filter(g => g.items.length);
  });

  readonly initials = computed(() => (this.auth.user()?.fullName ?? '?').split(' ').map(w => w[0]).slice(0, 2).join('').toUpperCase());

  constructor() {
    this.darkMode.set(localStorage.getItem('careconnect-theme') === 'dark');
    this.applyTheme();
    const destroyRef = inject(DestroyRef);
    // Side-by-side menu on desktop, slide-over menu on phones.
    effect(() => this.navOpen.set(!this.isMobile()));
    // Poll the unread count every 30 s (and after each navigation).
    const navigations = this.router.events.pipe(filter(e => e instanceof NavigationEnd), startWith(null));
    navigations.pipe(
      switchMap(() => timer(0, 30_000)),
      switchMap(() => this.api.unreadNotifications().pipe(catchError(() => of(this.unread())))),
      takeUntilDestroyed(destroyRef),
    ).subscribe(count => this.unread.set(count));

    this.router.events.pipe(filter(e => e instanceof NavigationEnd), takeUntilDestroyed(destroyRef)).subscribe(() => {
      if (this.isMobile()) this.navOpen.set(false);
    });
  }

  toggleTheme(): void {
    this.darkMode.update(value => !value);
    this.applyTheme();
  }

  private applyTheme(): void {
    const theme = this.darkMode() ? 'dark' : 'light';
    document.documentElement.classList.toggle('dark-theme', this.darkMode());
    localStorage.setItem('careconnect-theme', theme);
  }

  loadNotifications(): void {
    this.api.notifications(false, 8).subscribe({ next: page => this.notifications.set(page.content), error: () => undefined });
  }

  openNotification(n: AppNotification): void {
    if (!n.read) {
      this.api.markNotificationRead(n.id).subscribe({ next: () => this.unread.update(c => Math.max(0, c - 1)), error: () => undefined });
    }
    if (n.link) this.router.navigateByUrl(n.link);
  }

  markAllRead(): void {
    this.api.markAllNotificationsRead().subscribe(() => {
      this.unread.set(0);
      this.notifications.update(list => list.map(n => ({ ...n, read: true })));
    });
  }

  notificationIcon(type: string): string {
    return ({ LAB_RESULT: 'science', PRESCRIPTION: 'medication', LOW_STOCK: 'inventory_2', MESSAGE: 'mail', APPOINTMENT: 'event', ORDER: 'assignment' } as Record<string, string>)[type] ?? 'notifications';
  }

  changePassword(): void {
    this.dialog.open(ChangePasswordDialog, { width: '420px' });
  }

  logout(): void {
    this.auth.logout();
  }
}


