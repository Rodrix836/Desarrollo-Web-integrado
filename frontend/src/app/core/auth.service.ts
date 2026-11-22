import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs';
import { Residuo, Sesion, Usuario } from './models';

/** Margen antes del vencimiento en el que se pide un token nuevo. */
const MARGEN_RENOVACION_MS = 5 * 60 * 1000;

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private temporizador?: ReturnType<typeof setTimeout>;
  usuario = signal<Usuario | null>(null);

  constructor() {
    try {
      const raw = sessionStorage.getItem('reciclo.sesion');
      if (raw && this.token()) {
        this.usuario.set(JSON.parse(raw));
        this.programarRenovacion();
      }
    } catch {
      this.logout(false);
    }
  }

  token(): string | null {
    const token = sessionStorage.getItem('reciclo.token');
    if (!token) return null;
    const exp = this.expiracion(token);
    if (exp === null || exp <= Date.now()) {
      this.logout(false);
      return null;
    }
    return token;
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
  /** Relee los datos de la cuenta (nombre, rol, empresa). */
  refresh() {
    return this.http.get<Usuario>('/api/auth/me').pipe(
      tap((u) => {
        this.usuario.set(u);
        sessionStorage.setItem('reciclo.sesion', JSON.stringify(u));
      }),
    );
  }
  /** Pide un token nuevo antes de que expire el actual. */
  renovar() {
    return this.http.post<Sesion>('/api/auth/refresh', {}).pipe(tap((s) => this.guardar(s)));
  }
  actualizarPerfil(nombre: string) {
    return this.http.put<Usuario>('/api/auth/me', { nombre }).pipe(
      tap((u) => {
        this.usuario.set(u);
        sessionStorage.setItem('reciclo.sesion', JSON.stringify(u));
      }),
    );
  }
  cambiarPassword(actual: string, nueva: string) {
    return this.http.put<void>('/api/auth/me/password', { actual, nueva });
  }

  rol(): string {
    return this.usuario()?.rol ?? '';
  }
  admin(): boolean {
    return this.rol() === 'ADMIN';
  }
  esEmpresa(): boolean {
    return this.rol() === 'EMPRESA';
  }
  /** ADMIN y EMPRESA pueden registrar materiales (EMPRESA sólo los suyos). */
  puedeGestionar(): boolean {
    return this.admin() || this.esEmpresa();
  }
  puedeConsultar(): boolean {
    return ['ADMIN', 'USER', 'EMPRESA'].includes(this.rol());
  }
  /** USER y EMPRESA envían solicitudes de intercambio. */
  puedeSolicitar(): boolean {
    return ['USER', 'EMPRESA'].includes(this.rol());
  }
  /** Igual que la regla de residuos-api: registró el material o es de su empresa. */
  esDueno(r: Residuo): boolean {
    const u = this.usuario();
    if (!u) return false;
    if (r.propietario && r.propietario === u.username) return true;
    return (
      u.rol === 'EMPRESA' &&
      !!u.empresa &&
      !!r.empresa &&
      r.empresa.trim().toLocaleLowerCase() === u.empresa.trim().toLocaleLowerCase()
    );
  }
  puedeEditar(r: Residuo): boolean {
    return this.admin() || (this.esEmpresa() && this.esDueno(r));
  }

  logout(navigate = true) {
    if (this.temporizador) clearTimeout(this.temporizador);
    sessionStorage.removeItem('reciclo.token');
    sessionStorage.removeItem('reciclo.sesion');
    this.usuario.set(null);
    if (navigate) void this.router.navigate(['/login']);
  }

  private guardar(sesion: Sesion) {
    const { token, expiraEnMs: _, ...usuario } = sesion;
    sessionStorage.setItem('reciclo.token', token);
    sessionStorage.setItem('reciclo.sesion', JSON.stringify(usuario));
    this.usuario.set(usuario);
    this.programarRenovacion();
  }

  private programarRenovacion() {
    if (this.temporizador) clearTimeout(this.temporizador);
    const token = sessionStorage.getItem('reciclo.token');
    const exp = token ? this.expiracion(token) : null;
    if (exp === null) return;
    const restante = exp - Date.now();
    const espera = Math.max(restante - MARGEN_RENOVACION_MS, restante / 2, 1000);
    this.temporizador = setTimeout(() => {
      // Si falla, el token vence y el interceptor cierra la sesión con el 401.
      this.renovar().subscribe({ error: () => undefined });
    }, espera);
  }

  private expiracion(token: string): number | null {
    try {
      const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
      return typeof payload.exp === 'number' ? payload.exp * 1000 : null;
    } catch {
      return null;
    }
  }
}
