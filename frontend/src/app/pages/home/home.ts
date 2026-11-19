import { Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ApiService, mensajeError } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { AvisoIntercambio, Residuo, nombreTipo, nombreEstado } from '../../core/models';
import { AvisosIntercambioComponent } from '../../shared/avisos-intercambio';

@Component({ imports: [RouterLink, AvisosIntercambioComponent], templateUrl: './home.html' })
export class HomePage implements OnInit {
  auth = inject(AuthService);
  private api = inject(ApiService);
  datos: Residuo[] = [];
  avisos: AvisoIntercambio[] = [];
  error = '';
  cargando = false;
  nombreTipo = nombreTipo;
  nombreEstado = nombreEstado;
  ngOnInit() {
    if (this.auth.puedeConsultar()) this.cargar();
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
  get disponibles() {
    return this.datos.filter((r) => r.estadoPublicacion === 'DISPONIBLE').length;
  }
  get empresas() {
    return new Set(this.datos.map((r) => r.empresa).filter(Boolean)).size;
  }
}
