# Consultas de ReCiclo

Servicio de lectura en 8083. `GET /api/consultas/residuos` acepta:

| Parámetro | Regla |
| --- | --- |
| empresa | Contiene el texto, sin distinguir mayúsculas. |
| tipo | Uno de los seis valores de TipoResiduo. |
| disponible | Decisión de habilitar intercambio, también para futuros. |
| cantidadMinima | Límite inclusivo, conserva BigDecimal. |
| estado | NO_PUBLICADO, PROGRAMADO o DISPONIBLE. |
| preferencia | TipoResiduo aceptado a cambio, por ejemplo PAPEL_CARTON. |

`GET /api/consultas/empresas` devuelve los nombres de empresas de los materiales autorizados.

Cada solicitud requiere Bearer y lo reenvía a `GET /api/residuos` de Residuos en 8082. Allí se valida la firma, el vencimiento y el rol USER/ADMIN. USER recibe únicamente el catálogo disponible; ningún filtro amplía esa visibilidad. ADMIN recibe todos los registros. Consultas aplica sus filtros a esa respuesta y transmite `fechaPublicacion` y `estadoPublicacion`. Para buscar materiales disponibles ahora usa `estado=DISPONIBLE`.

No almacena una copia de los residuos ni recalcula su estado. El JPQL de la versión anterior permanece en la aplicación raíz. Este adaptador usa HTTP y filtros Java.

Desde la raíz:

~~~powershell
powershell.exe -ExecutionPolicy Bypass -File .\scripts\iniciar-api.ps1 -Servicio consultas
mvn.cmd -f consultas-api/pom.xml clean verify
~~~

CORS permite localhost:4200 y 127.0.0.1:4200. Ante caída de Residuos se devuelve 502 con un mensaje controlado. Consulta la [guía de intercambios programados](../docs/GUIA_INTERCAMBIOS_PROGRAMADOS.txt) para iniciar el sistema y demostrar los filtros.
