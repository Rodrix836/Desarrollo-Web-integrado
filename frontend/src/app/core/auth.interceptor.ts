import { inject } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const propia = /^\/api\/(auth|usuarios|residuos|consultas)(\/|$)/.test(request.url);
  const token = propia ? auth.token() : null;
  const segura = token
    ? request.clone({ setHeaders: { Authorization: 'Bearer ' + token } })
    : request;
  return next(segura).pipe(
    catchError((error) => {
      if (propia && error.status === 401 && !/\/api\/auth\/(login|register)$/.test(request.url)) {
        auth.logout();
      }
      return throwError(() => error);
    }),
  );
};
