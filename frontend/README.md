# ReCiclo Angular

Interfaz Angular 20 para Auth (8081), Residuos (8082) y Consultas (8083).
La aplicación se abre en http://localhost:4200.

La [guía completa de Windows](../docs/GUIA_FRONTEND_WINDOWS.txt) incluye preparación, comandos de inicio, cuentas, demo del CRUD y solución de los errores de PowerShell.

Desde la raíz, abre una terminal por API y ejecuta el comando correspondiente.

```powershell
powershell.exe -ExecutionPolicy Bypass -File .\scripts\iniciar-api.ps1 -Servicio auth
powershell.exe -ExecutionPolicy Bypass -File .\scripts\iniciar-api.ps1 -Servicio residuos
powershell.exe -ExecutionPolicy Bypass -File .\scripts\iniciar-api.ps1 -Servicio consultas
```

En otra terminal, instala y sirve Angular.

```powershell
cd frontend
npm.cmd ci
npm.cmd start
```

Cuenta ADMIN de desarrollo `admin / admin123`. Cuenta USER `user / user123`.
El registro público crea cuentas USER.

`proxy.conf.json` enruta las solicitudes a los servicios locales. Una publicación de `dist/frontend/browser` requiere configurar esas mismas rutas en el reverse proxy del servidor.

`npm.cmd run build` compila producción. Con las APIs y el frontend activos, `npm.cmd test` ejecuta cuatro pruebas de navegador usando Google Chrome instalado. Las pruebas crean cuentas y realizan CRUD en las bases de desarrollo.

El JWT se conserva en sessionStorage y vence en una hora. Los guards controlan la navegación, mientras el backend aplica los permisos. Las pantallas incluyen estados de carga, vacío, error y confirmación de borrado.

Consultas usa un servicio HTTP de lectura sobre Residuos. Los JPQL anteriores siguen en la aplicación raíz.
