export interface Usuario {
  id?: number;
  username: string;
  nombre: string;
  rol: string;
  empresa?: string | null;
  activo?: boolean;
  creadoEn?: string;
}
export interface Sesion extends Usuario {
  token: string;
  expiraEnMs?: number;
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
  propietario?: string | null;
  creadoPor?: string | null;
  creadoEn?: string | null;
  actualizadoPor?: string | null;
  actualizadoEn?: string | null;
}
export interface ResiduoInput {
  tipoResiduo: string;
  cantidad: number;
  unidadMedida: string;
  fecha?: string;
  descripcion: string;
  empresa: string;
  intercambioPor: string;
  disponible: boolean;
  fechaPublicacion: string | null;
}
export function aResiduoInput(r: Residuo): ResiduoInput {
  return {
    tipoResiduo: r.tipoResiduo,
    cantidad: r.cantidad,
    unidadMedida: r.unidadMedida,
    fecha: r.fecha,
    descripcion: r.descripcion,
    empresa: r.empresa,
    intercambioPor: r.intercambioPor,
    disponible: r.disponible,
    fechaPublicacion: r.fechaPublicacion,
  };
}
export type EstadoSolicitud = 'PENDIENTE' | 'ACEPTADA' | 'RECHAZADA' | 'CANCELADA';
export interface Solicitud {
  id: number;
  residuoId: number;
  residuoTipo: string;
  empresaDestino: string;
  solicitante: string;
  empresaSolicitante: string | null;
  ofrece: string | null;
  cantidadOfrecida: number | null;
  mensaje: string;
  estado: EstadoSolicitud;
  respuesta: string;
  creadoEn: string;
  actualizadoEn: string;
}
export interface SolicitudInput {
  residuoId: number;
  ofrece: string | null;
  cantidadOfrecida: number | null;
  mensaje: string;
}
export interface GrupoEstadistica {
  clave: string;
  etiqueta: string;
  registros: number;
  kg: number;
}
export interface Estadisticas {
  registros: number;
  disponibles: number;
  totalKg: number;
  porTipo: GrupoEstadistica[];
  porEmpresa: GrupoEstadistica[];
  porMes: GrupoEstadistica[];
  solicitudesPendientes: number;
}
export const estadosSolicitud: Record<EstadoSolicitud, string> = {
  PENDIENTE: 'Pendiente',
  ACEPTADA: 'Aceptada',
  RECHAZADA: 'Rechazada',
  CANCELADA: 'Cancelada',
};
export function fechaHora(instante: string | null | undefined): string {
  if (!instante) return '—';
  return new Intl.DateTimeFormat('es-PE', {
    timeZone: 'America/Lima',
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(instante));
}
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
