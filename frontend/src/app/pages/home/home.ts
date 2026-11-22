import { Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ApiService, mensajeError } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { AvisoIntercambio, Estadisticas, Residuo, nombreTipo, nombreEstado } from '../../core/models';
import { GraficoBarrasComponent } from '../../shared/grafico-barras';
import { AvisosIntercambioComponent } from '../../shared/avisos-intercambio';

@Component({ imports: [RouterLink, AvisosIntercambioComponent, GraficoBarrasComponent], templateUrl: './home.html' })
export class HomePage implements OnInit {
  auth = inject(AuthService);
  private api = inject(ApiService);
  datos: Residuo[] = [];
  resumen: Estadisticas = { registros: 0, disponibles: 0, totalKg: 0, porTipo: [], porEmpresa: [], porMes: [], solicitudesPendientes: 0 };
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
    forkJoin({ datos: this.api.listar(), avisos: this.api.avisos(), resumen: this.api.estadisticas() }).subscribe({
      next: (r) => {
        this.datos = r.datos;
        this.resumen = r.resumen;
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
    return this.resumen.disponibles;
  }
}
