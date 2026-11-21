import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = (_, state) => {
  const auth = inject(AuthService);
  return auth.token()
    ? true
    : inject(Router).createUrlTree(['/login'], { queryParams: { volver: state.url } });
};

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.token()) {
    return router.createUrlTree(['/login']);
  }

  if (auth.admin()) {
    return true;
  }

  alert('No tienes acceso a este apartado. Se requiere rol de administrador.');
  return router.createUrlTree(['/']);
};
