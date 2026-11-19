export interface Usuario {
  id?: number;
  username: string;
  nombre: string;
  rol: string;
  empresa?: string;
}
export interface Sesion extends Usuario {
  token: string;
}
export type EstadoPublicacion = 'NO_PUBLICADO' | 'PROGRAMADO' | 'DISPONIBLE';
export interface Residuo {
  id: number;
  tipoResiduo: string;
  cantidad: number;
  unidadMedida: string;
  fecha: string;
  descripcion: string;
  empresa: string;
  intercambioPor: string;
  disponible: boolean;
  fechaPublicacion: string | null;
  estadoPublicacion: EstadoPublicacion;
}
export type ResiduoInput = Omit<Residuo, 'id' | 'estadoPublicacion' | 'fecha'> & { fecha?: string };
export interface AvisoIntercambio {
  id: number;
  tipoResiduo: string;
  empresa: string;
  fechaPublicacion: string;
  estadoPublicacion: EstadoPublicacion;
  mensaje: string;
}
export const tipos = [
  { value: 'PLASTICO', label: 'Plástico' },
  { value: 'PAPEL_CARTON', label: 'Papel y cartón' },
  { value: 'VIDRIO', label: 'Vidrio' },
  { value: 'METAL', label: 'Metal' },
  { value: 'ORGANICO', label: 'Orgánico' },
  { value: 'OTROS', label: 'Otros' },
];
export const estados = [
  { value: 'NO_PUBLICADO', label: 'No publicado' },
  { value: 'PROGRAMADO', label: 'Programado' },
  { value: 'DISPONIBLE', label: 'Disponible' },
];
export function nombreTipo(value: string): string {
  return tipos.find((t) => t.value === value)?.label ?? value;
}
export function nombreEstado(value: EstadoPublicacion): string {
  return estados.find((e) => e.value === value)?.label ?? value;
}
// Un LocalDate no se convierte a un instante UTC para presentarlo.
export function fechaLegible(fecha: string | null): string {
  if (!fecha) return 'Sin programar';
  const [anio, mes, dia] = fecha.split('-');
  return `${dia}/${mes}/${anio}`;
}
export function hoyLima(): string {
  const partes = new Intl.DateTimeFormat('en', {
    timeZone: 'America/Lima',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(new Date());
  const parte = (tipo: string) => partes.find((p) => p.type === tipo)!.value;
  return `${parte('year')}-${parte('month')}-${parte('day')}`;
}
