import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-perfil',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './perfil.html',
  styles: [
    `
      .perfil-page {
        max-width: 520px;
      }
      .perfil-card {
        display: flex;
        flex-direction: column;
        gap: 0;
        padding: 0.25rem 0;
      }
      .perfil-row {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 1rem;
        padding: 1rem 1.25rem;
        border-bottom: 1px solid rgba(0, 0, 0, 0.06);
      }
      .perfil-row:last-child {
        border-bottom: none;
      }
      .perfil-label {
        font-size: 0.85rem;
        color: #6b7280;
      }
      .rol-badge {
        display: inline-block;
        padding: 0.25rem 0.75rem;
        border-radius: 999px;
        font-size: 0.8rem;
        font-weight: 600;
        background: #e8f0ec;
        color: #1a4d3e;
      }
    `,
  ],
})
export class PerfilComponent implements OnInit {
  auth = inject(AuthService);
  error = '';
  cargando = true;

  ngOnInit() {
    if (!this.auth.token()) {
      this.error = 'Debes iniciar sesión.';
      this.cargando = false;
      return;
    }
    this.auth.refresh().subscribe({
      next: () => (this.cargando = false),
      error: () => {
        this.error = 'No se pudo cargar el perfil.';
        this.cargando = false;
      },
    });
  }
}
