import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService, mensajeError } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Residuo, nombreTipo } from '../../core/models';
@Component({ imports: [RouterLink], templateUrl: './detalle.html' })
export class DetallePage implements OnInit {
  auth = inject(AuthService);
  private api = inject(ApiService);
  private router = inject(Router);
  private id = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));
  residuo?: Residuo;
  error = '';
  eliminando = false;
  nombreTipo = nombreTipo;
  ngOnInit() {
    this.api
      .consultar(this.id)
      .subscribe({ next: (r) => (this.residuo = r), error: (e) => (this.error = mensajeError(e)) });
  }
  eliminar() {
    if (!confirm('¿Eliminar este residuo de forma permanente?')) return;
    this.eliminando = true;
    this.api.eliminar(this.id).subscribe({
      next: () => void this.router.navigate(['/residuos']),
      error: (e) => {
        this.error = mensajeError(e);
        this.eliminando = false;
      },
    });
  }
}
