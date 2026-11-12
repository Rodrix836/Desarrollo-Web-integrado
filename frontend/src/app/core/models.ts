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
}
export type ResiduoInput = Omit<Residuo, 'id'>;
export const tipos = [
  { value: 'PLASTICO', label: 'Plástico' },
  { value: 'PAPEL_CARTON', label: 'Papel y cartón' },
  { value: 'VIDRIO', label: 'Vidrio' },
  { value: 'METAL', label: 'Metal' },
  { value: 'ORGANICO', label: 'Orgánico' },
  { value: 'OTROS', label: 'Otros' },
];
export function nombreTipo(value: string): string {
  return tipos.find((t) => t.value === value)?.label ?? value;
}
