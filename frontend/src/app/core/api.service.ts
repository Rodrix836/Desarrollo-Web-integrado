import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { AvisoIntercambio, Estadisticas, EstadoSolicitud, Pagina, Residuo, ResiduoInput, Sesion, Solicitud, SolicitudInput, Usuario } from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);
  refresh() {
    return this.http.post<Sesion>('/api/auth/refresh', {});
  }
  perfil() {
    return this.http.get<Usuario>('/api/auth/me');
  }
  actualizarPerfil(nombre: string) {
    return this.http.put<Usuario>('/api/auth/me', { nombre });
  }
  cambiarPassword(actual: string, nueva: string) {
    return this.http.put<void>('/api/auth/me/password', { actual, nueva });
  }
  editarUsuario(id: number, nombre: string) {
    return this.http.put<Usuario>('/api/usuarios/' + id, { nombre });
  }
  estadoUsuario(id: number, activo: boolean) {
    return this.http.put<Usuario>('/api/usuarios/' + id + '/estado', { activo });
  }
  eliminarUsuario(id: number) {
    return this.http.delete<void>('/api/usuarios/' + id);
  }
  pagina(pagina = 0, tamanio = 10) {
    return this.http.get<Pagina<Residuo>>('/api/residuos/pagina', { params: { pagina, tamanio } });
  }
  estadisticas() {
    return this.http.get<Estadisticas>('/api/residuos/estadisticas');
  }
  solicitar(datos: SolicitudInput) {
    return this.http.post<Solicitud>('/api/solicitudes', datos);
  }
  solicitudes() {
    return this.http.get<Solicitud[]>('/api/solicitudes');
  }
  misSolicitudes() {
    return this.http.get<Solicitud[]>('/api/solicitudes/mias');
  }
  cancelarSolicitud(id: number) {
    return this.http.put<Solicitud>('/api/solicitudes/' + id + '/cancelar', {});
  }
  decidirSolicitud(id: number, estado: Extract<EstadoSolicitud, 'ACEPTADA' | 'RECHAZADA'>, respuesta: string) {
    return this.http.put<Solicitud>('/api/solicitudes/' + id + '/decision', { estado, respuesta });
  }
  listar() {
    return this.http.get<Residuo[]>('/api/residuos');
  }
  avisos() {
    return this.http.get<AvisoIntercambio[]>('/api/residuos/avisos');
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
  if (error.status === 403 && error.error === 'Invalid CORS request')
    return 'El servidor rechazó el origen de esta página. Abre http://localhost:4200 y vuelve a intentar.';
  if (error.status === 0 || error.status === 502 || error.status === 504)
    return 'No se pudo conectar con el servicio. Comprueba que las APIs estén iniciadas y vuelve a intentar.';
  if (error.error?.mensaje) return error.error.mensaje;
  if (error.status === 403) return 'Tu rol no tiene permiso para realizar esta operación.';
  return (
    error.error?.mensaje ??
    'No se pudo completar la solicitud. Revisa los datos e inténtalo nuevamente.'
  );
}
