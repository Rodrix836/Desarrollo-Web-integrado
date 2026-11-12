# Consultas de ReCiclo

Servicio de lectura en 8083. GET /api/consultas/residuos acepta empresa (contiene, sin distinguir mayúsculas), tipo, disponible y cantidadMinima (inclusive). GET /api/consultas/empresas devuelve los nombres registrados.

Cada solicitud requiere Bearer y lo reenvía a GET /api/residuos de Residuos en 8082. Allí se valida la firma, el vencimiento y el rol USER/ADMIN. El servicio aplica los filtros a la respuesta autorizada y conserva la cantidad como BigDecimal. No almacena una copia de los residuos.

El JPQL de la versión anterior permanece en la aplicación raíz. Este adaptador usa HTTP y filtros Java.

Desde la raíz:
```powershell
powershell.exe -ExecutionPolicy Bypass -File .\scripts\iniciar-api.ps1 -Servicio consultas
mvn.cmd -f consultas-api/pom.xml clean verify
```

CORS permite el origen localhost:4200. Ante caída de Residuos se devuelve 502 con un mensaje controlado.
