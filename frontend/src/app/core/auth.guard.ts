import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
export const authGuard: CanActivateFn = (_, state) => {
  const auth = inject(AuthService);
  return auth.token()
    ? true
    : inject(Router).createUrlTree(['/login'], { queryParams: { volver: state.url } });
};
export const adminGuard: CanActivateFn = () =>
  inject(AuthService).admin() || inject(Router).createUrlTree(['/']);
