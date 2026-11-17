import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AvisoIntercambio, fechaLegible, nombreTipo } from '../core/models';

@Component({
  selector: 'app-avisos-intercambio',
  imports: [RouterLink],
  template: ` <section class="notices-section" aria-label="Avisos de intercambio">
    <div class="section-title">
      <h2>Próximos intercambios</h2>
      <span>{{ avisos.length }} {{ avisos.length === 1 ? 'aviso' : 'avisos' }}</span>
    </div>
    <div class="panel">
      @if (cargando) {
        <p class="empty" role="status">Actualizando avisos…</p>
      } @else if (!avisos.length) {
        <p class="notice-empty">
          Los materiales programados y las publicaciones de hoy aparecerán aquí.
        </p>
      } @else {
        @for (aviso of avisos; track aviso.id) {
          <article class="notice-row">
            <div class="notice-date">
              <strong>{{ fechaLegible(aviso.fechaPublicacion) }}</strong
              ><span>Publicación</span>
            </div>
            <div class="notice-copy">
              <strong>{{ nombreTipo(aviso.tipoResiduo) }} · {{ aviso.empresa }}</strong>
              <p>{{ aviso.mensaje }}</p>
            </div>
            @if (aviso.estadoPublicacion === 'DISPONIBLE') {
              <a class="button secondary" [routerLink]="['/residuos', aviso.id]">Ver material ↗</a>
            } @else {
              <span class="status scheduled">Programado</span>
            }
          </article>
        }
      }
    </div>
  </section>`,
})
export class AvisosIntercambioComponent {
  @Input() avisos: AvisoIntercambio[] = [];
  @Input() cargando = false;
  fechaLegible = fechaLegible;
  nombreTipo = nombreTipo;
}
