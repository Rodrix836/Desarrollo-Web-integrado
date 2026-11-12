import { KeyValuePipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService, mensajeError } from '../../core/api.service';
import { ResiduoInput, tipos } from '../../core/models';
@Component({ imports: [FormsModule, RouterLink, KeyValuePipe], templateUrl: './formulario.html' })
export class FormularioPage implements OnInit {
  private api = inject(ApiService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  id = Number(this.route.snapshot.paramMap.get('id')) || null;
  tipos = tipos;
  cargando = false;
  guardando = false;
  error = '';
  errores: Record<string, string> = {};
  datos: ResiduoInput = {
    tipoResiduo: 'PLASTICO',
    cantidad: 1,
    unidadMedida: 'kg',
    fecha: new Date().toLocaleDateString('en-CA'),
    descripcion: '',
    empresa: '',
    intercambioPor: '',
    disponible: false,
  };
  ngOnInit() {
    if (this.id) {
      this.cargando = true;
      this.api.consultar(this.id).subscribe({
        next: (r) => {
          const { id, ...datos } = r;
          this.datos = datos;
          this.cargando = false;
        },
        error: (e) => {
          this.error = mensajeError(e);
          this.cargando = false;
        },
      });
    }
  }
  guardar(form: NgForm) {
    this.error = '';
    this.errores = {};
    if (form.invalid || this.datos.cantidad <= 0 || this.guardando) {
      this.error = 'Completa los campos obligatorios y revisa la cantidad.';
      return;
    }
    if (this.datos.disponible && !this.datos.empresa.trim()) {
      this.error = 'Indica la empresa para publicar un material disponible.';
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
