import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, mensajeError } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Usuario } from '../../core/models';
@Component({ imports: [FormsModule], templateUrl: './usuarios.html' })
export class UsuariosPage implements OnInit {
  auth = inject(AuthService);
  private api = inject(ApiService);
  usuarios: Usuario[] = [];
  seleccion: Record<number, string> = {};
  error = '';
  mensaje = '';
  guardando?: number;
  ngOnInit() {
    this.api.usuarios().subscribe({
      next: (u) => {
        this.usuarios = u;
        u.forEach((item) => (this.seleccion[item.id!] = item.rol));
      },
      error: (e) => (this.error = mensajeError(e)),
    });
  }
  guardar(u: Usuario) {
    this.error = '';
    this.mensaje = '';
    this.guardando = u.id;
    this.api.rol(u.id!, this.seleccion[u.id!]).subscribe({
      next: (actualizado) => {
        u.rol = actualizado.rol;
        this.guardando = undefined;
        this.mensaje =
          'Rol actualizado. El usuario debe volver a iniciar sesión para renovar sus permisos.';
      },
      error: (e) => {
        this.error = mensajeError(e);
        this.guardando = undefined;
      },
    });
  }
}
