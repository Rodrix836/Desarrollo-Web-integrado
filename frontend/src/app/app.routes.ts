import { Routes } from '@angular/router';
import { authGuard, adminGuard } from './core/auth.guard';
export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./pages/login/login').then((m) => m.LoginPage) },
  {
    path: '',
    canActivate: [authGuard],
    children: [
      { path: '', loadComponent: () => import('./pages/home/home').then((m) => m.HomePage) },
      {
        path: 'residuos',
        loadComponent: () => import('./pages/residuos/residuos').then((m) => m.ResiduosPage),
      },
      {
        path: 'residuos/nuevo',
        canActivate: [adminGuard],
        loadComponent: () => import('./pages/residuos/formulario').then((m) => m.FormularioPage),
      },
      {
        path: 'residuos/:id/editar',
        canActivate: [adminGuard],
        loadComponent: () => import('./pages/residuos/formulario').then((m) => m.FormularioPage),
      },
      {
        path: 'residuos/:id',
        loadComponent: () => import('./pages/residuos/detalle').then((m) => m.DetallePage),
      },
      {
        path: 'consultas',
        loadComponent: () => import('./pages/consultas/consultas').then((m) => m.ConsultasPage),
      },
      {
        path: 'usuarios',
        canActivate: [adminGuard],
        loadComponent: () => import('./pages/usuarios/usuarios').then((m) => m.UsuariosPage),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
