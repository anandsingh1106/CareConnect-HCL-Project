import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { finalize, map, Observable, shareReplay, tap } from 'rxjs';
import { Role, TokenResponse, UserInfo } from './models';

const ACCESS_KEY = 'cc.accessToken';
const REFRESH_KEY = 'cc.refreshToken';
const USER_KEY = 'cc.user';

/** Where each role lands after signing in. */
export const HOME_ROUTE = '/dashboard';

/**
 * Holds the signed-in user and the JWT pair.
 * The access token (15 min) is sent on every call; the refresh token (7 days) is exchanged for a new pair
 * by the interceptor when the API answers 401. Tokens live in localStorage so a page reload keeps the session.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly userSignal = signal<UserInfo | null>(read<UserInfo>(USER_KEY));
  private accessToken: string | null = readRaw(ACCESS_KEY);
  private refreshToken: string | null = readRaw(REFRESH_KEY);
  private refreshInFlight$?: Observable<string>;

  readonly user = this.userSignal.asReadonly();
  readonly loggedIn = computed(() => this.userSignal() !== null);
  readonly role = computed(() => this.userSignal()?.role ?? null);

  token(): string | null {
    return this.accessToken;
  }

  hasRefreshToken(): boolean {
    return !!this.refreshToken;
  }

  hasRole(...roles: Role[]): boolean {
    const role = this.role();
    return role !== null && roles.includes(role);
  }

  login(username: string, password: string): Observable<UserInfo> {
    return this.http.post<TokenResponse>('/api/auth/login', { username, password }).pipe(
      tap(res => this.store(res)),
      map(res => res.user));
  }

  /** Exchanges the refresh token for a new pair. Concurrent callers share one request. */
  refresh(): Observable<string> {
    if (!this.refreshInFlight$) {
      this.refreshInFlight$ = this.http.post<TokenResponse>('/api/auth/refresh', { refreshToken: this.refreshToken }).pipe(
        tap(res => this.store(res)),
        map(res => res.accessToken),
        finalize(() => (this.refreshInFlight$ = undefined)),
        shareReplay(1));
    }
    return this.refreshInFlight$;
  }

  logout(): void {
    const refreshToken = this.refreshToken;
    if (refreshToken) {
      // Revoke on the server; the local session is cleared either way.
      this.http.post('/api/auth/logout', { refreshToken }).subscribe({ error: () => undefined });
    }
    this.clear();
    this.router.navigateByUrl('/login');
  }

  /** Called when the session cannot be renewed (refresh token expired or revoked). */
  sessionExpired(): void {
    this.clear();
    this.router.navigate(['/login'], { queryParams: { expired: 1, returnUrl: this.router.url } });
  }

  changePassword(currentPassword: string, newPassword: string): Observable<void> {
    return this.http.post<void>('/api/auth/change-password', { currentPassword, newPassword });
  }

  private store(res: TokenResponse): void {
    this.accessToken = res.accessToken;
    this.refreshToken = res.refreshToken;
    this.userSignal.set(res.user);
    write(ACCESS_KEY, res.accessToken);
    write(REFRESH_KEY, res.refreshToken);
    write(USER_KEY, JSON.stringify(res.user));
  }

  private clear(): void {
    this.accessToken = null;
    this.refreshToken = null;
    this.userSignal.set(null);
    [ACCESS_KEY, REFRESH_KEY, USER_KEY].forEach(k => write(k, null));
  }
}

function readRaw(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

function read<T>(key: string): T | null {
  const raw = readRaw(key);
  try {
    return raw ? (JSON.parse(raw) as T) : null;
  } catch {
    return null;
  }
}

function write(key: string, value: string | null): void {
  try {
    if (value === null) {
      localStorage.removeItem(key);
    } else {
      localStorage.setItem(key, value);
    }
  } catch {
    // storage unavailable (private mode): the session simply lasts for this tab
  }
}
