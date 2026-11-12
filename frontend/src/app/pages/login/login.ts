import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { mensajeError } from '../../core/api.service';
@Component({
  imports: [FormsModule],
  templateUrl: './login.html',
})
export class LoginPage {
  auth = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  registro = false;
  username = '';
  password = '';
  nombre = '';
  error = '';
  cargando = false;
  entrar() {
    if (this.cargando) return;
    this.error = '';
    this.cargando = true;
    const request = this.registro
      ? this.auth.register(this.username, this.password, this.nombre)
      : this.auth.login(this.username, this.password);
    request.pipe(finalize(() => (this.cargando = false))).subscribe({
      next: () => {
        const volver = this.route.snapshot.queryParamMap.get('volver') ?? '/';
        void this.router.navigateByUrl(
          volver.startsWith('/') && !volver.startsWith('//') ? volver : '/',
        );
      },
      error: (error) =>
        (this.error =
          error.status === 401 ? 'Usuario o contraseña incorrectos.' : mensajeError(error)),
    });
  }
}
