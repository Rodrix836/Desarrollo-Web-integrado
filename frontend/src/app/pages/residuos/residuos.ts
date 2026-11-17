import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ApiService, mensajeError } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import {
  AvisoIntercambio,
  Residuo,
  tipos,
  estados,
  nombreTipo,
  nombreEstado,
  fechaLegible,
} from '../../core/models';
import { AvisosIntercambioComponent } from '../../shared/avisos-intercambio';

@Component({
  imports: [FormsModule, RouterLink, AvisosIntercambioComponent],
  templateUrl: './residuos.html',
})
export class ResiduosPage implements OnInit {
  auth = inject(AuthService);
  private api = inject(ApiService);
  datos: Residuo[] = [];
  avisos: AvisoIntercambio[] = [];
  tipos = tipos;
  estados = estados;
  nombreTipo = nombreTipo;
  nombreEstado = nombreEstado;
  fechaLegible = fechaLegible;
  busqueda = '';
  tipo = '';
  estado = '';
  preferencia = '';
  cargando = false;
  error = '';
  ngOnInit() {
    this.cargar();
  }
  cargar() {
    this.error = '';
    this.cargando = true;
    forkJoin({ datos: this.api.listar(), avisos: this.api.avisos() }).subscribe({
      next: (r) => {
        this.datos = r.datos;
        this.avisos = r.avisos;
        this.cargando = false;
      },
      error: (e) => {
        this.error = mensajeError(e);
        this.cargando = false;
      },
    });
  }
  get filtrados() {
    const q = this.busqueda.trim().toLocaleLowerCase();
    return this.datos.filter(
      (r) =>
        (!this.tipo || r.tipoResiduo === this.tipo) &&
        (!this.estado || r.estadoPublicacion === this.estado) &&
        (!this.preferencia || r.intercambioPor === this.preferencia) &&
        (!q ||
          [r.descripcion, r.empresa, nombreTipo(r.tipoResiduo)]
            .join(' ')
            .toLocaleLowerCase()
            .includes(q)),
    );
  }
}
