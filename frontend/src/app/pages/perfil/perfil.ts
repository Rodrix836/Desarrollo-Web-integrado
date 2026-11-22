import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-perfil',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './perfil.html',
})
export class PerfilComponent implements OnInit {
  auth = inject(AuthService);
  error = '';
  cargando = true;

  descripcionRol(rol: string): string {
    switch (rol) {
      case 'ADMIN':
        return 'Gestiona materiales, programa intercambios y administra usuarios.';
      case 'USER':
        return 'Consulta el catálogo disponible y recibe avisos de intercambio.';
      default:
        return 'Cuenta de empresa. Requiere un rol USER o ADMIN para ver el catálogo.';
    }
  }

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
