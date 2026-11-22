import { Component, HostListener, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AuthService } from './core/auth.service';
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
})
export class App {
  auth = inject(AuthService);
  private router = inject(Router);
  menuAbierto = signal(false);
  @HostListener('document:keydown.escape') cerrarMenu() {
    if (!this.menuAbierto()) return;
    this.menuAbierto.set(false);
    document.getElementById('boton-menu')?.focus();
  }
  hoy = new Intl.DateTimeFormat('es-PE', {
    timeZone: 'America/Lima',
    weekday: 'long',
    day: 'numeric',
    month: 'long',
  }).format(new Date());
  constructor() {
    this.router.events.pipe(takeUntilDestroyed()).subscribe(evento => {
      if (evento instanceof NavigationEnd) this.menuAbierto.set(false);
    });
    if (this.auth.token()) this.auth.refresh().subscribe({ error: () => this.auth.logout() });
  }
}
