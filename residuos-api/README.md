# ReCiclo · API de Residuos

Servicio REST del integrante 2. Corre en el puerto **8082** con Java 21, Spring Boot 3.5, Hibernate y H2. Auth emite los JWT. Residuos verifica el token y persiste el CRUD en su propia base.

## Abrir en VS Code desde Windows

En PowerShell o CMD, ubícate en la carpeta donde quieras clonar el repositorio y ejecuta:

```powershell
git clone https://github.com/Rodrix836/Desarrollo-Web-integrado.git
cd Desarrollo-Web-integrado
git fetch origin
git switch Rodrigo_Frontend-Angular
git branch --show-current
code .
```

Si ya tienes el repositorio, entra en su carpeta y empieza por `git fetch origin`. Abre **Terminal → Nueva terminal** en VS Code y selecciona **PowerShell**. Los comandos restantes se ejecutan desde la raíz del repositorio salvo donde se indique otra carpeta.

Instala JDK 21 y Maven 3.6.3 o posterior. Comprueba que Maven usa Java 21:

```powershell
java -version
mvn -version
```

Si aparece otra versión, usa la ruta real de tu JDK 21 en esa terminal:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.3.9-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn -version
```

## Ejecutar las pruebas

```powershell
mvn -f residuos-api/pom.xml clean verify
```

La suite usa H2 en memoria. Las pruebas unitarias con Mockito verifican las interacciones del servicio con el repositorio. Las de integración envían peticiones MockMvc con JWT firmados y comprueban los estados, el contenido de las respuestas y los cambios en la base. Consulta los resultados detallados en `residuos-api/target/surefire-reports/`.

La verificación del módulo completó **41 pruebas** (10 unitarias y 31 de integración), sin fallos ni omisiones. La prueba de listado con un JWT válido devolvió 401 antes de implementar el endpoint y 200 después. La demostración con Auth en 8081 confirmó el CRUD en 8082 y la recuperación del registro actualizado al reiniciar el servicio.

## Demostración integrada con Auth y Angular

Auth ya está en auth-api dentro de este repositorio. Su login recibe username y password.
Inicia una API en cada terminal, desde la raíz:

```powershell
powershell.exe -ExecutionPolicy Bypass -File ./scripts/iniciar-api.ps1 -Servicio auth
powershell.exe -ExecutionPolicy Bypass -File ./scripts/iniciar-api.ps1 -Servicio residuos
```

Usa la [guía del frontend Angular](../docs/GUIA_FRONTEND_WINDOWS.txt) para iniciar Consultas y abrir la página en localhost:4200. El script configura la misma clave local para Auth y Residuos.

Swagger está en http://localhost:8082/swagger-ui/index.html. Puedes obtener un JWT con POST /api/auth/login y copiarlo en Authorize, o usar peticiones.http en VS Code con REST Client. Las cuentas demo de una base Auth vacía son admin/admin123 y user/user123.

## Demostración local sin Auth

En una terminal PowerShell, desde la raíz, crea una clave efímera y genera un JWT de 15 minutos. El script es un cliente auxiliar de la demo. La API aplica la misma verificación de firma y permisos.

```powershell
$bytes = New-Object byte[] 32
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
$env:RECICLO_JWT_SECRET = [Convert]::ToBase64String($bytes)
$tokenLocal = powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\residuos-api\ejemplos\token-local.ps1 -Rol ADMIN
Set-Clipboard -Value $tokenLocal
mvn -f residuos-api/pom.xml "-Dspring-boot.run.profiles=demo" spring-boot:run
```

Usa el token del portapapeles en **Authorize** de Swagger. La opción `-Rol USER` genera un token de lectura. En REST Client, puedes sustituir temporalmente el valor de `@token` durante la sesión de prueba. Revierte el cambio antes de guardar o publicar el archivo.

## Contrato HTTP

| Método y ruta | Rol | Respuesta |
| --- | --- | --- |
| `GET /api/residuos` | USER o ADMIN | 200 y lista, incluso si está vacía |
| `GET /api/residuos/{id}` | USER o ADMIN | 200 y registro, o 404 |
| `POST /api/residuos` | ADMIN | 201, cuerpo con ID y cabecera `Location` |
| `PUT /api/residuos/{id}` | ADMIN | 200 y registro reemplazado, o 404 |
| `DELETE /api/residuos/{id}` | ADMIN | 204 sin cuerpo, o 404 |

El ID es un entero positivo. H2 asigna el ID al crear y el PUT toma el ID de la ruta. Los tipos admitidos son `PLASTICO`, `PAPEL_CARTON`, `VIDRIO`, `METAL`, `ORGANICO` y `OTROS`. Envía la fecha en formato `yyyy-MM-dd`. Los campos obligatorios son tipo, cantidad positiva, unidad y fecha. Si `disponible` vale `true`, se exige una empresa. Las longitudes máximas son 500 para descripción, 100 para empresa y 300 para intercambio.

Los errores de validación son HTTP 400 y detallan los campos en `errores`. Las respuestas de error incluyen `mensaje`, `errores`, `timestamp` y `status`. Un Bearer ausente o inválido da 401. Un rol sin permiso da 403. Un conflicto de escritura detectado por JPA da 409.

## Persistencia, JWT y Angular

- **JPA.** `RegistroResiduo` representa la tabla y `RegistroResiduoRepository` extiende `JpaRepository`. El servicio abre las transacciones y Hibernate guarda las modificaciones del PUT al confirmar. H2 usa `DECFLOAT` para evitar redondear la cantidad a dos decimales. Para comprobar la persistencia, detén el servicio con `Ctrl+C` y arráncalo con el mismo comando y directorio. H2 abre `./data/residuosdb.mv.db` relativo al directorio del proceso. La URL puede ajustarse mediante `SPRING_DATASOURCE_URL`.
- **JWT.** Spring Security verifica HS256, la firma y el vencimiento. Exige `sub`, `exp` y un `rol` de tipo texto. También comprueba `nbf` si está presente. El emisor inspeccionado no envía `iss` ni `aud`. Si Auth incorpora esos claims al contrato, añade sus verificaciones a `SecurityConfig` antes de la integración. Cada petición incluye `Authorization: Bearer TOKEN` y se autoriza sin sesión de servidor.
- **Angular.** Usa `http://localhost:8082/api/residuos` como URL base. El origen permitido es `http://localhost:4200`. Un interceptor de Angular puede adjuntar el token de Auth a las peticiones dirigidas a Residuos.

Para exponer el proyecto, abre primero `RegistroResiduoController` y sigue la inyección por constructor hacia `RegistroResiduoService` y `RegistroResiduoRepository`. En `RegistroResiduo`, muestra `@Entity`, el ID generado y el campo decimal. Después muestra `SecurityConfig` y ejecuta `RegistroResiduoApiTest` para demostrar el CRUD, el rechazo de firmas inválidas y la diferencia entre 401 y 403.
