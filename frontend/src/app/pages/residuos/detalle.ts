import { Component, inject, OnInit } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService, mensajeError } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Residuo, nombreTipo, nombreEstado, fechaLegible, fechaHora, tipos } from '../../core/models';
@Component({ imports: [RouterLink, FormsModule], templateUrl: './detalle.html' })
export class DetallePage implements OnInit {
  auth = inject(AuthService);
  private api = inject(ApiService);
  private router = inject(Router);
  private id = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));
  residuo?: Residuo;
  error = '';
  mensaje = '';
  enviando = false;
  solicitada = false;
  tipos = tipos;
  fechaHora = fechaHora;
  ofrece = '';
  cantidadOfrecida: number | null = null;
  propuesta = '';
  solicitar(form: NgForm) {
    if (form.invalid || this.enviando || this.solicitada) return;
    if (this.cantidadOfrecida !== null && this.cantidadOfrecida <= 0) {
      this.error = 'La cantidad ofrecida debe ser mayor que cero.';
      return;
    }
    this.enviando = true;
    this.error = '';
    this.api.solicitar({ residuoId: this.id, ofrece: this.ofrece || null,
      cantidadOfrecida: this.cantidadOfrecida, mensaje: this.propuesta }).subscribe({
      next: () => {
        this.enviando = false;
        this.solicitada = true;
        this.mensaje = 'Solicitud enviada. Puedes seguirla en Mis solicitudes.';
      },
      error: e => { this.error = mensajeError(e); this.enviando = false; },
    });
  }

  eliminando = false;
  nombreTipo = nombreTipo;
  nombreEstado = nombreEstado;
  fechaLegible = fechaLegible;
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
