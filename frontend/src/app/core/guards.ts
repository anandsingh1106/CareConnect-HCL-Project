import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService, HOME_ROUTE } from './auth.service';
import { Role } from './models';

/**
 * Requires a signed-in user; when the route has data.roles, the user's role must be one of them.
 * This only hides screens - the API enforces the same rules with @PreAuthorize.
 */
export const authGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.loggedIn()) {
    return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
  }
  const roles = route.data['roles'] as Role[] | undefined;
  if (roles && !auth.hasRole(...roles)) {
    return router.createUrlTree([HOME_ROUTE]);
  }
  return true;
};

/** Keeps signed-in users away from the login page. */
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.loggedIn() ? inject(Router).createUrlTree([HOME_ROUTE]) : true;
};
