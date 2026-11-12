import { Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService, mensajeError } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Residuo, nombreTipo } from '../../core/models';
@Component({ imports: [RouterLink], templateUrl: './home.html' })
export class HomePage implements OnInit {
  auth = inject(AuthService);
  private api = inject(ApiService);
  datos: Residuo[] = [];
  error = '';
  cargando = true;
  nombreTipo = nombreTipo;
  ngOnInit() {
    if (this.auth.puedeConsultar())
      this.api.listar().subscribe({
        next: (datos) => {
          this.datos = datos;
          this.cargando = false;
        },
        error: (e) => {
          this.error = mensajeError(e);
          this.cargando = false;
        },
      });
    else this.cargando = false;
  }
  get disponibles() {
    return this.datos.filter((r) => r.disponible).length;
  }
  get empresas() {
    return new Set(this.datos.map((r) => r.empresa).filter(Boolean)).size;
  }
}
