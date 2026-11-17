import { KeyValuePipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService, mensajeError } from '../../core/api.service';
import { fechaLegible, hoyLima, ResiduoInput, tipos } from '../../core/models';

@Component({ imports: [FormsModule, RouterLink, KeyValuePipe], templateUrl: './formulario.html' })
export class FormularioPage implements OnInit {
  private api = inject(ApiService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  id = Number(this.route.snapshot.paramMap.get('id')) || null;
  tipos = tipos;
  fechaLegible = fechaLegible;
  cargando = false;
  cargaFallida = false;
  guardando = false;
  error = '';
  errores: Record<string, string> = {};
  fechaOriginal: string | null = null;
  preferenciaHistorica = '';
  datos: ResiduoInput = {
    tipoResiduo: 'PLASTICO',
    cantidad: 1,
    unidadMedida: 'kg',
    descripcion: '',
    empresa: '',
    intercambioPor: '',
    disponible: false,
    fechaPublicacion: null,
  };

  ngOnInit() {
    if (!this.id) return;
    this.cargando = true;
    this.api.consultar(this.id).subscribe({
      next: (r) => {
        const { id, estadoPublicacion, ...datos } = r;
        this.fechaOriginal = r.fechaPublicacion;
        this.preferenciaHistorica = tipos.some((t) => t.value === r.intercambioPor)
          ? ''
          : r.intercambioPor;
        this.datos = datos;
        if (r.disponible && !r.fechaPublicacion) this.datos.fechaPublicacion = hoyLima();
        this.cargando = false;
      },
      error: (e) => {
        this.error = mensajeError(e);
        this.cargaFallida = true;
        this.cargando = false;
      },
    });
  }

  cambiarIntercambio() {
    this.errores = {};
    if (this.datos.disponible) {
      this.datos.fechaPublicacion = hoyLima();
    } else {
      this.datos.intercambioPor = '';
      this.datos.fechaPublicacion = null;
    }
  }

  get fechaMinima(): string {
    if (
      this.fechaOriginal &&
      this.datos.fechaPublicacion === this.fechaOriginal &&
      this.fechaOriginal < hoyLima()
    ) {
      return this.fechaOriginal;
    }
    return hoyLima();
  }

  guardar(form: NgForm) {
    this.error = '';
    this.errores = {};
    if (form.invalid || this.datos.cantidad <= 0 || this.guardando) {
      this.error = 'Completa los campos obligatorios y revisa la cantidad.';
      return;
    }
    if (
      this.datos.disponible &&
      (!this.datos.empresa.trim() || !this.datos.intercambioPor || !this.datos.fechaPublicacion)
    ) {
      this.error =
        'Indica empresa, preferencia y fecha de publicación para habilitar el intercambio.';
      return;
    }
    this.guardando = true;
    const req = this.id ? this.api.actualizar(this.id, this.datos) : this.api.crear(this.datos);
    req.subscribe({
      next: (r) => void this.router.navigate(['/residuos', r.id]),
      error: (e) => {
        this.error = mensajeError(e);
        this.errores = e.error?.errores ?? {};
        this.guardando = false;
      },
    });
  }
}
