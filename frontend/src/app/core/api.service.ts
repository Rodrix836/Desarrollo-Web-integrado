import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Residuo, ResiduoInput, Usuario } from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);
  listar() {
    return this.http.get<Residuo[]>('/api/residuos');
  }
  consultar(id: number) {
    return this.http.get<Residuo>('/api/residuos/' + id);
  }
  crear(datos: ResiduoInput) {
    return this.http.post<Residuo>('/api/residuos', datos);
  }
  actualizar(id: number, datos: ResiduoInput) {
    return this.http.put<Residuo>('/api/residuos/' + id, datos);
  }
  eliminar(id: number) {
    return this.http.delete<void>('/api/residuos/' + id);
  }
  consultas(filtros: Record<string, string>) {
    const params = new HttpParams({ fromObject: filtros });
    return this.http.get<Residuo[]>('/api/consultas/residuos', { params });
  }
  empresas() {
    return this.http.get<string[]>('/api/consultas/empresas');
  }
  usuarios() {
    return this.http.get<Usuario[]>('/api/usuarios');
  }
  rol(id: number, rol: string) {
    return this.http.put<Usuario>('/api/usuarios/' + id + '/rol', { rol });
  }
}
export function mensajeError(error: HttpErrorResponse): string {
  if (error.status === 0 || error.status === 502 || error.status === 504)
    return 'No se pudo conectar con el servicio. Comprueba que las APIs estén iniciadas y vuelve a intentar.';
  if (error.status === 403) return 'Tu rol no tiene permiso para realizar esta operación.';
  return (
    error.error?.mensaje ??
    'No se pudo completar la solicitud. Revisa los datos e inténtalo nuevamente.'
  );
}
