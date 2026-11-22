import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, mensajeError } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { EstadoSolicitud, Solicitud, estadosSolicitud, fechaHora, nombreTipo } from '../../core/models';

@Component({ imports: [FormsModule], templateUrl: './solicitudes.html' })
export class SolicitudesPage implements OnInit {
  auth = inject(AuthService);
  private api = inject(ApiService);
  datos: Solicitud[] = [];
  estados = Object.entries(estadosSolicitud);
  estadosSolicitud = estadosSolicitud;
  nombreTipo = nombreTipo;
  fechaHora = fechaHora;
  estado = '';
  respuestas: Record<number, string> = {};
  cargando = false;
  guardando = false;
  error = '';
  mensaje = '';
  ngOnInit() { this.cargar(); }
  get filtradas() { return this.datos.filter(s => !this.estado || s.estado === this.estado); }
  cargar() {
    this.cargando = true;
    this.error = '';
    const peticion = this.auth.admin() ? this.api.solicitudes() : this.api.misSolicitudes();
    peticion.subscribe({
      next: datos => { this.datos = datos; this.cargando = false; },
      error: e => { this.error = mensajeError(e); this.cargando = false; },
    });
  }
  decidir(s: Solicitud, estado: Extract<EstadoSolicitud, 'ACEPTADA' | 'RECHAZADA'>) {
    if (this.guardando) return;
    this.guardar(this.api.decidirSolicitud(s.id, estado, this.respuestas[s.id] ?? ''));
  }
  cancelar(s: Solicitud) {
    if (this.guardando) return;
    this.guardar(this.api.cancelarSolicitud(s.id));
  }
  private guardar(peticion: ReturnType<ApiService['cancelarSolicitud']>) {
    this.guardando = true;
    this.error = '';
    this.mensaje = '';
    peticion.subscribe({
      next: s => {
        this.guardando = false;
        this.mensaje = 'Solicitud actualizada: ' + estadosSolicitud[s.estado] + '.';
        this.cargar();
      },
      error: e => { this.guardando = false; this.error = mensajeError(e); },
    });
  }
}
