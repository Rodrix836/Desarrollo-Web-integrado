import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { ApiService, mensajeError } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Usuario } from '../../core/models';
@Component({ imports: [FormsModule], templateUrl: './usuarios.html' })
export class UsuariosPage implements OnInit {
  auth = inject(AuthService);
  private api = inject(ApiService);
  usuarios: Usuario[] = [];
  seleccion: Record<number, string> = {};
  nombres: Record<number, string> = {};
  error = '';
  mensaje = '';
  cargando = true;
  guardando?: number;
  propia(u: Usuario) { return u.username === this.auth.usuario()?.username; }
  ngOnInit() {
    this.api.usuarios().subscribe({
      next: usuarios => {
        this.usuarios = usuarios;
        usuarios.forEach(u => { this.seleccion[u.id!] = u.rol; this.nombres[u.id!] = u.nombre; });
        this.cargando = false;
      },
      error: e => { this.error = mensajeError(e); this.cargando = false; },
    });
  }
  guardar(u: Usuario) {
    this.actualizar(u, this.api.rol(u.id!, this.seleccion[u.id!]),
      'Rol actualizado. El usuario debe volver a iniciar sesión para renovar sus permisos.');
  }
  guardarNombre(u: Usuario) {
    if (!this.nombres[u.id!]?.trim()) return;
    this.actualizar(u, this.api.editarUsuario(u.id!, this.nombres[u.id!].trim()), 'Nombre actualizado.');
  }
  cambiarEstado(u: Usuario) {
    this.actualizar(u, this.api.estadoUsuario(u.id!, !u.activo), 'Estado de la cuenta actualizado.');
  }
  eliminar(u: Usuario) {
    if (this.propia(u) || this.guardando !== undefined || !confirm('¿Eliminar la cuenta de ' + u.username + ' de forma permanente?')) return;
    this.error = ''; this.mensaje = ''; this.guardando = u.id;
    this.api.eliminarUsuario(u.id!).subscribe({
      next: () => { this.usuarios = this.usuarios.filter(item => item.id !== u.id); this.guardando = undefined; this.mensaje = 'Cuenta eliminada.'; },
      error: e => { this.error = mensajeError(e); this.guardando = undefined; },
    });
  }
  private actualizar(u: Usuario, peticion: Observable<Usuario>, mensaje: string) {
    if (this.propia(u) || this.guardando !== undefined) return;
    this.error = ''; this.mensaje = ''; this.guardando = u.id;
    peticion.subscribe({
      next: actualizado => {
        Object.assign(u, actualizado);
        this.seleccion[u.id!] = u.rol;
        this.nombres[u.id!] = u.nombre;
        this.guardando = undefined; this.mensaje = mensaje;
      },
      error: e => { this.error = mensajeError(e); this.guardando = undefined; },
    });
  }
}
