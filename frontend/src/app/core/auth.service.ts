import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs';
import { Sesion, Usuario } from './models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  usuario = signal<Usuario | null>(null);

  constructor() {
    try {
      const raw = sessionStorage.getItem('reciclo.sesion');
      if (raw && this.token()) this.usuario.set(JSON.parse(raw));
    } catch {
      this.logout(false);
    }
  }

  token(): string | null {
    const token = sessionStorage.getItem('reciclo.token');
    if (!token) return null;
    try {
      const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
      if (typeof payload.exp !== 'number' || payload.exp * 1000 <= Date.now()) {
        this.logout(false);
        return null;
      }
      return token;
    } catch {
      this.logout(false);
      return null;
    }
  }

  login(username: string, password: string) {
    return this.http
      .post<Sesion>('/api/auth/login', { username, password })
      .pipe(tap((s) => this.guardar(s)));
  }
  register(username: string, password: string, nombre: string) {
    return this.http
      .post<Sesion>('/api/auth/register', { username, password, nombre })
      .pipe(tap((s) => this.guardar(s)));
  }
  refresh() {
    return this.http.get<Usuario>('/api/auth/me').pipe(
      tap((u) => {
        this.usuario.set(u);
        sessionStorage.setItem('reciclo.sesion', JSON.stringify(u));
      }),
    );
  }
  admin(): boolean {
    return this.usuario()?.rol === 'ADMIN';
  }
  puedeConsultar(): boolean {
    return ['ADMIN', 'USER'].includes(this.usuario()?.rol ?? '');
  }
  logout(navigate = true) {
    sessionStorage.removeItem('reciclo.token');
    sessionStorage.removeItem('reciclo.sesion');
    this.usuario.set(null);
    if (navigate) void this.router.navigate(['/login']);
  }
  private guardar(sesion: Sesion) {
    const { token, ...usuario } = sesion;
    sessionStorage.setItem('reciclo.token', token);
    sessionStorage.setItem('reciclo.sesion', JSON.stringify(usuario));
    this.usuario.set(usuario);
  }
}
