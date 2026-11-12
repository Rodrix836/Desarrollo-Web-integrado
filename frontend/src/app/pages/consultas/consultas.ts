import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService, mensajeError } from '../../core/api.service';
import { Residuo, tipos, nombreTipo } from '../../core/models';
@Component({ imports: [FormsModule, RouterLink], templateUrl: './consultas.html' })
export class ConsultasPage implements OnInit {
  private api = inject(ApiService);
  tipos = tipos;
  nombreTipo = nombreTipo;
  datos: Residuo[] = [];
  empresas: string[] = [];
  empresa = '';
  tipo = '';
  disponible = '';
  cantidadMinima = '';
  error = '';
  cargando = false;
  consultado = false;
  ngOnInit() {
    this.api
      .empresas()
      .subscribe({
        next: (r) => (this.empresas = r),
        error: (e) => (this.error = mensajeError(e)),
      });
    this.buscar();
  }
  buscar() {
    this.cargando = true;
    this.error = '';
    const filtros = Object.fromEntries(
      Object.entries({
        empresa: this.empresa,
        tipo: this.tipo,
        disponible: this.disponible,
        cantidadMinima: this.cantidadMinima,
      }).filter(([, v]) => v !== ''),
    );
    this.api.consultas(filtros).subscribe({
      next: (r) => {
        this.datos = r;
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
