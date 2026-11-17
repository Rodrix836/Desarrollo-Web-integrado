import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ApiService, mensajeError } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Residuo, tipos, estados, nombreTipo, nombreEstado, fechaLegible } from '../../core/models';

@Component({ imports: [FormsModule, RouterLink], templateUrl: './consultas.html' })
export class ConsultasPage implements OnInit {
  private api = inject(ApiService);
  auth = inject(AuthService);
  tipos = tipos;
  estados = estados;
  nombreTipo = nombreTipo;
  nombreEstado = nombreEstado;
  fechaLegible = fechaLegible;
  datos: Residuo[] = [];
  empresas: string[] = [];
  empresa = '';
  tipo = '';
  estado = '';
  preferencia = '';
  cantidadMinima = '';
  error = '';
  cargando = false;
  consultado = false;
  ngOnInit() {
    this.buscar();
  }
  buscar() {
    this.cargando = true;
    this.error = '';
    const filtros = Object.fromEntries(
      Object.entries({
        empresa: this.empresa,
        tipo: this.tipo,
        estado: this.estado,
        preferencia: this.preferencia,
        cantidadMinima: this.cantidadMinima,
      }).filter(([, v]) => v !== ''),
    );
    forkJoin({ datos: this.api.consultas(filtros), empresas: this.api.empresas() }).subscribe({
      next: (r) => {
        this.datos = r.datos;
        this.empresas = r.empresas;
        this.cargando = false;
        this.consultado = true;
      },
      error: (e) => {
        this.error = mensajeError(e);
        this.cargando = false;
      },
    });
  }
}
