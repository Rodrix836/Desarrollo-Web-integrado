import { Component, Input } from '@angular/core';
import { GrupoEstadistica } from '../core/models';

@Component({
  selector: 'app-grafico-barras',
  template: `<section class="panel grafico" [attr.aria-label]="titulo">
    <h2>{{ titulo }}</h2>
    <p class="muted">Kilogramos registrados</p>
    @if (cargando) { <p role="status">Cargando datos…</p> }
    @else if (!grupos.length) { <p class="muted">Aún no hay materiales en kilos.</p> }
    @else {
      <ul class="barras">
        @for (g of grupos; track g.clave) {
          <li><div class="barra-etiqueta"><span>{{ g.etiqueta }}</span><strong>{{ g.kg }} kg</strong></div>
            <div class="barra-fondo" aria-hidden="true"><span [style.width.%]="maximo ? g.kg / maximo * 100 : 0"></span></div></li>
        }
      </ul>
    }
  </section>`,
})
export class GraficoBarrasComponent {
  @Input() titulo = '';
  @Input() grupos: GrupoEstadistica[] = [];
  @Input() cargando = false;
  get maximo() { return Math.max(0, ...this.grupos.map(g => g.kg)); }
}
