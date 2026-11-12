import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService, mensajeError } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Residuo, tipos, nombreTipo } from '../../core/models';
@Component({ imports: [FormsModule, RouterLink], templateUrl: './residuos.html' })
export class ResiduosPage implements OnInit {
  auth = inject(AuthService);
  private api = inject(ApiService);
  datos: Residuo[] = [];
  tipos = tipos;
  nombreTipo = nombreTipo;
  busqueda = '';
  tipo = '';
  cargando = false;
  error = '';
  ngOnInit() {
    this.cargar();
  }
  cargar() {
    this.error = '';
    this.cargando = true;
    this.api.listar().subscribe({
      next: (r) => {
        this.datos = r;
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
        (!q ||
          [r.descripcion, r.empresa, nombreTipo(r.tipoResiduo)]
            .join(' ')
            .toLocaleLowerCase()
            .includes(q)),
    );
  }
}
