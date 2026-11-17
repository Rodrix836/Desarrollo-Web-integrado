# ReCiclo · API de Residuos

Servicio REST en **8082**, con Java 21, Spring Boot 3.5, JPA/Hibernate y H2. Auth firma los JWT; Residuos valida el token y persiste el CRUD.

La [guía Windows de intercambios programados](../docs/GUIA_INTERCAMBIOS_PROGRAMADOS.txt) incluye apertura en VS Code, cuatro terminales, cuentas, PowerShell, Swagger y solución de los errores que tuvimos.

## Arranque y pruebas

Desde la raíz del repositorio, en una terminal por API:

~~~powershell
powershell.exe -ExecutionPolicy Bypass -File .\scripts\iniciar-api.ps1 -Servicio auth
powershell.exe -ExecutionPolicy Bypass -File .\scripts\iniciar-api.ps1 -Servicio residuos
~~~

El script configura Java 21, la clave compartida y la base de archivo en residuos-api/data. El perfil demo habilita [Swagger](http://localhost:8082/swagger-ui/index.html). Auth usa username/password; cuentas iniciales de una base vacía: admin/admin123 y user/user123.

Con Residuos detenido para evitar bloquear el JAR en Windows:

~~~powershell
mvn.cmd -f residuos-api/pom.xml clean verify
~~~

La suite de Residuos contiene 53 pruebas. Usa H2, MockMvc y Mockito para verificar CRUD, JWT, roles, validaciones, decimal exacto y programación. Un Clock de prueba permite cruzar medianoche en Lima sin cambiar el reloj del equipo. Reportes en target/surefire-reports.

## Contrato HTTP

| Método y ruta | Rol | Respuesta |
| --- | --- | --- |
| GET /api/residuos | USER o ADMIN | 200 y lista autorizada. Filtros estado y preferencia. |
| GET /api/residuos/avisos | USER o ADMIN | 200 y resúmenes de publicaciones de hoy o futuras. |
| GET /api/residuos/{id} | USER o ADMIN | 200 o 404 según existencia y visibilidad. |
| POST /api/residuos | ADMIN | 201 y cabecera Location. |
| PUT /api/residuos/{id} | ADMIN | 200, o 404 si no existe. |
| DELETE /api/residuos/{id} | ADMIN | 204 sin cuerpo, o 404. |

El ID es positivo y lo asigna H2. POST y PUT reciben JSON:

| Campo | Regla |
| --- | --- |
| tipoResiduo | Obligatorio: PLASTICO, PAPEL_CARTON, VIDRIO, METAL, ORGANICO u OTROS. |
| cantidad | BigDecimal obligatorio mayor que cero. |
| unidadMedida | kg para altas nuevas; una edición puede conservar la unidad histórica original. |
| fecha | Dato histórico opcional en la solicitud. Al crear se asigna hoy en Lima si se omite; al editar se conserva la anterior si se omite. |
| descripcion | Opcional, hasta 500 caracteres. |
| disponible | Booleano de habilitación de intercambio; false si se omite. |
| empresa | Obligatoria al habilitar, hasta 100 caracteres. |
| intercambioPor | Obligatoria al habilitar: código del TipoResiduo aceptado, por ejemplo PAPEL_CARTON. Hasta 300 caracteres para compatibilidad histórica. |
| fechaPublicacion | Fecha yyyy-MM-dd obligatoria al habilitar. Hoy o futura en America/Lima. |

Una edición de un registro habilitado puede mantener su fecha de publicación pasada; cambiarla exige hoy o una fecha futura. Puede mantener una preferencia histórica de texto libre sin modificarla, o cambiarla por un valor del catálogo.

Con disponible=false el servidor limpia intercambioPor y fechaPublicacion. Al crear también deja empresa vacía; al editar conserva la empresa anterior. Esta normalización se aplica aunque el cliente envíe campos contradictorios.

Los archivos ejemplos/registro.json y ejemplos/actualizacion.json incluyen fechas ilustrativas de noviembre de 2026: cámbialas por hoy o una fecha futura antes de usarlos. La guía genera fechas válidas dinámicamente en PowerShell. peticiones.http utiliza esos archivos mediante REST Client.

## Publicación, avisos y autorización

El estado se deriva en el servidor con un Clock de America/Lima:

- NO_PUBLICADO: intercambio deshabilitado.
- PROGRAMADO: habilitado, fechaPublicacion posterior a hoy.
- DISPONIBLE: habilitado y fecha alcanzada.

ADMIN lista todo y puede filtrar por estado. USER solo lista y abre el detalle de DISPONIBLE. Un detalle privado o futuro devuelve 404 a USER. Los filtros se aplican después de restringir la visibilidad.

Los avisos contienen únicamente id, tipoResiduo, empresa, fechaPublicacion, estadoPublicacion y mensaje. Usan el ID del material, se ordenan por fecha e ID y se derivan de los registros actuales: reprogramar actualiza el aviso; cancelar o borrar lo elimina. Incluyen futuras publicaciones y las de hoy. Un aviso de hoy permite acceder al detalle. No son push ni tienen un historial independiente.

disponible=true expresa la intención de intercambiar. Para saber si se puede intercambiar ahora usa estadoPublicacion=DISPONIBLE. No hay un booleano persistido que quede desactualizado al pasar el día.

## Compatibilidad y persistencia

La nueva columna fecha_publicacion es nullable. Hibernate con ddl-auto=update permite abrir la base H2 anterior y añadirla. Los registros antiguos habilitados sin fechaPublicacion continúan DISPONIBLE; al editarlos habilitados se exige seleccionar una fecha. fecha histórica permanece separada.

Las cantidades y unidades anteriores se conservan, sin convertirlas ni reinterpretarlas. H2 usa DECFLOAT para conservar los decimales. JPA mantiene @Version y devuelve 409 ante un conflicto de escritura detectado. Detener y reiniciar desde la misma copia conserva los materiales en residuos-api/data/residuosdb.mv.db.

Los errores incluyen mensaje, errores, timestamp y status. Entradas inválidas: 400; Bearer ausente, firma o vencimiento inválidos: 401; rol sin permiso: 403; registro no visible: 404.

Spring Security verifica HS256 y exige sub, exp y rol. También valida nbf cuando está presente. Auth no emite iss ni aud en el contrato actual. Cada petición lleva Authorization: Bearer TOKEN y se autoriza sin sesión de servidor.

Angular en 4200 usa su proxy para las tres APIs. Consultas reenvía el mismo Bearer y filtra exclusivamente la respuesta autorizada de Residuos.
