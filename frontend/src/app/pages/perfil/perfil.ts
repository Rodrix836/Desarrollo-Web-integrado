import { Component, OnInit, inject } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { mensajeError } from '../../core/api.service';

@Component({ selector: 'app-perfil', imports: [RouterLink, FormsModule], templateUrl: './perfil.html' })
export class PerfilComponent implements OnInit {
  auth = inject(AuthService);
  error = '';
  mensaje = '';
  cargando = true;
  guardando = false;
  nombre = '';
  actual = '';
  nueva = '';
  confirmacion = '';
  descripcionRol(rol: string): string {
    return rol === 'ADMIN' ? 'Gestiona materiales, responde solicitudes de intercambio y administra usuarios.'
      : 'Consulta el catálogo disponible, solicita intercambios y cancela sus propuestas pendientes.';
  }
  ngOnInit() {
    this.auth.refresh().subscribe({
      next: u => { this.nombre = u.nombre; this.cargando = false; },
      error: e => { this.error = mensajeError(e); this.cargando = false; },
    });
  }
  guardarNombre(form: NgForm) {
    if (form.invalid || !this.nombre.trim() || this.guardando) return;
    this.error = ''; this.mensaje = ''; this.guardando = true;
    this.auth.actualizarPerfil(this.nombre.trim()).subscribe({
      next: u => { this.nombre = u.nombre; this.guardando = false; this.mensaje = 'Nombre actualizado.'; },
      error: e => { this.guardando = false; this.error = mensajeError(e); },
    });
  }
  cambiarPassword(form: NgForm) {
    if (this.guardando) return;
    this.error = ''; this.mensaje = '';
    if (this.nueva.length < 6) { this.error = 'La nueva contraseña debe tener al menos 6 caracteres.'; return; }
    if (this.nueva !== this.confirmacion) { this.error = 'La confirmación no coincide con la nueva contraseña.'; return; }
    if (form.invalid) { this.error = 'Completa la contraseña actual.'; return; }
    this.guardando = true;
    this.auth.cambiarPassword(this.actual, this.nueva).subscribe({
      next: () => { this.guardando = false; this.actual = ''; this.nueva = ''; this.confirmacion = ''; form.resetForm(); this.mensaje = 'Contraseña actualizada.'; },
      error: e => { this.guardando = false; this.error = mensajeError(e); },
    });
  }
}
