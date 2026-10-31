import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

const PUBLIC_ENDPOINTS = ['/api/auth/login', '/api/auth/refresh'];

/**
 * Adds "Authorization: Bearer <access token>" to API calls.
 * On a 401 it renews the token pair once and retries; if renewal fails the user is sent to the login page.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  if (!req.url.startsWith('/api') || PUBLIC_ENDPOINTS.some(p => req.url.startsWith(p))) {
    return next(req);
  }

  return next(withToken(req, auth.token())).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401 || !auth.hasRefreshToken()) {
        if (error.status === 401 && auth.loggedIn()) {
          auth.sessionExpired();
        }
        return throwError(() => error);
      }
      return auth.refresh().pipe(
        catchError(refreshError => {
          auth.sessionExpired();
          return throwError(() => refreshError);
        }),
        switchMap(token => next(withToken(req, token))));
    }));
};

function withToken(req: HttpRequest<unknown>, token: string | null): HttpRequest<unknown> {
  return token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;
}
