# ReCiclo — primer avance

**Plataforma web para promover y gestionar el reciclaje de residuos.** Esta primera versión permite registrar, consultar, editar y eliminar residuos mediante una API REST y páginas sencillas de Thymeleaf, HTML y CSS, sin JavaScript.


El proyecto está preparado para una exposición inicial de Desarrollo Web Integrado y para que otros integrantes continúen trabajando. Conserva el modelo y las capas de la base anterior. La interfaz y la persistencia en un archivo JSON se incorporan por solicitud explícita para esta presentación; no se presentan como exigencias del sílabo de la unidad 1.

ReCiclo no es una tienda: no incluye compras, ventas, pagos ni marketplace. Tampoco implementa usuarios, autenticación, bases de datos, estadísticas ni otros módulos avanzados.

## Requisitos y ejecución

- **JDK 21**.
- **Maven 3.6.3 o posterior**, disponible como `mvn`; no se incluye Maven Wrapper.
- Acceso a Internet en la primera compilación para descargar dependencias.

Desde la carpeta que contiene `pom.xml`, comprueba el entorno y ejecuta:

```text
java -version
mvn -version
mvn clean verify
mvn spring-boot:run
```

Tanto `java -version` como `mvn -version` deben indicar Java 21. En Windows, si difieren, selecciona tu JDK 21 mediante `JAVA_HOME` y antepone su carpeta `bin` al `PATH` de esa sesión de PowerShell. No es necesario modificar las variables globales del equipo.

Ejemplo para la instalación usada en esta verificación (ajusta la ruta si tu JDK está en otra carpeta):

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.3.9-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn clean verify
mvn spring-boot:run
```

Abre [http://localhost:8080](http://localhost:8080). Detén la aplicación con `Ctrl+C`. Como alternativa, después de compilar puedes arrancar el JAR:

```text
java -jar target/reciclo-0.0.1-SNAPSHOT.jar
```

Utiliza una forma de arranque a la vez. Detén el servidor antes de volver a ejecutar `mvn clean verify`: en Windows, el proceso mantiene abierto el JAR y puede impedir que Maven lo reemplace. Para ejecutar sólo las pruebas:

```text
mvn test
```

## Estructura y responsabilidades

```text
pom.xml
docs/                              # Ejemplos JSON para peticiones HTTP
src/main/java/pe/reciclo/
  ReCicloApplication.java
  controller/                      # API REST y páginas/formularios
  service/                         # Operaciones sobre residuos
  repository/                      # Lectura y escritura del archivo JSON
  model/                           # RegistroResiduo y TipoResiduo
src/main/resources/
  application.properties
  residuos.json                    # Tres registros de semilla
  templates/                       # Vistas Thymeleaf
  static/css/                      # Estilos sencillos
src/test/java/pe/reciclo/
  controller/                      # API, páginas y formularios
  service/                         # Reglas y operaciones
  repository/                      # Persistencia y recarga del archivo
data/residuos.json                 # Se genera al ejecutar; ignorado por Git
```

El flujo es `Cliente HTTP → Controller → Service → Repository → archivo JSON`. Spring conecta las capas mediante inyección por constructor. La API y la interfaz usan el mismo servicio y los mismos datos. `RegistroResiduo` conserva el nombre y el paquete de la entrega anterior; no se crea una entidad adicional de usuario.

## Páginas para la exposición

| Página | Ruta |
| --- | --- |
| Inicio, explicación y botones principales | `/` |
| Tabla de residuos | `/residuos` |
| Formulario de registro | `/residuos/nuevo` |
| Detalle | `/residuos/{id}` |
| Formulario de edición | `/residuos/{id}/editar` |
| Confirmación de eliminación | `/residuos/{id}/eliminar` |

Los formularios envían POST para registrar, editar y confirmar la eliminación. Abrir la página de confirmación no elimina el registro. Las entradas inválidas muestran errores junto a los campos. La interfaz usa verde, blanco y gris claro, sin scripts, gráficos ni animaciones.

## Datos iniciales y persistencia

`src/main/resources/residuos.json` contiene tres registros demostrativos: **1: Plástico, 2 kg; 2: Papel / Cartón, 5 kg; 3: Vidrio, 3 kg**. Al iniciar por primera vez, si el archivo externo no existe, se copian allí los datos de semilla.

Los cambios se guardan en **`data/residuos.json`**, relativo al directorio desde el que se arranca la aplicación. Al reiniciar se carga ese archivo; su contenido prevalece sobre la semilla. Una lista `[]` es válida y permanece vacía: eliminar todos los registros no vuelve a instalar los ejemplos. Un JSON corrupto, con campos inválidos o ids ausentes/duplicados impide el arranque y se conserva sin sobrescribir.

Antes de aceptar los registros del disco se aplican las mismas validaciones del modelo. Al guardar se escribe primero un archivo temporal y luego se reemplaza el archivo de datos; la memoria se actualiza sólo cuando el guardado termina correctamente. Si falla la escritura, la operación devuelve un error comprensible y conserva los registros anteriores.

La propiedad `reciclo.datos.archivo` permite elegir otro archivo. Por ejemplo, para una demostración separada:

```text
java -jar target/reciclo-0.0.1-SNAPSHOT.jar --reciclo.datos.archivo=data/demo-residuos.json
```

La carpeta `data/` está ignorada por Git. La semilla sí se versiona; los datos de cada ejecución no se incorporan al repositorio. Si necesitas editar el archivo manualmente, detén primero la aplicación y vuelve a iniciarla después para cargar los cambios.

Esta solución está pensada para **un proceso de la aplicación por archivo**. No es una base de datos ni coordina escrituras entre varios procesos. Los ids se generan en el servidor: al arrancar, el siguiente se calcula como el máximo existente más uno. Por ello, después de eliminar registros y reiniciar, algunos ids eliminados pueden reutilizarse. Ejecuta los reinicios desde el mismo directorio y con la misma ruta configurada para recuperar tus datos.

## Contrato y endpoints de la API

POST y PUT reciben `Content-Type: application/json`:

| Campo | Regla |
| --- | --- |
| `id` | Entero generado por el servidor. No necesitas enviarlo: un id recibido no decide la asignación ni cambia otros registros; PUT utiliza el id de la ruta. El JSON del disco sí conserva los ids. |
| `tipoResiduo` | Obligatorio: `PLASTICO`, `PAPEL_CARTON`, `VIDRIO`, `METAL`, `ORGANICO` u `OTROS`. |
| `cantidad` | Número obligatorio mayor que cero; admite decimales. |
| `unidadMedida` | Texto obligatorio, distinto de vacío o espacios; por ejemplo `kg` o `unidades`. |
| `fecha` | Fecha válida obligatoria en formato `yyyy-MM-dd`. |
| `descripcion` | Opcional, hasta 500 caracteres; puede omitirse o ser `null`. |

Los tipos se escriben exactamente como se muestran, en mayúsculas y sin tildes. `PAPEL_CARTON` representa Papel / Cartón. No hay conversiones entre unidades ni restricciones de fecha pasada o futura.

Ejemplo incluido en `docs/registro.json`:

```json
{
  "tipoResiduo": "PLASTICO",
  "cantidad": 2.5,
  "unidadMedida": "kg",
  "fecha": "2026-09-09",
  "descripcion": "Botellas limpias"
}
```

La respuesta añade el `id` asignado. PUT reemplaza los campos de un registro existente, mantiene el id y exige los mismos campos obligatorios que POST; omitir la descripción la deja en `null`. `docs/actualizacion.json` contiene otro ejemplo válido.

| Método y ruta | Respuesta |
| --- | --- |
| `GET /api/residuos` | `200` y lista JSON; `[]` si no hay registros. |
| `GET /api/residuos/{id}` | `200` y registro; `404` si no existe. |
| `POST /api/residuos` | `201`, registro JSON y cabecera `Location` con su URL. |
| `PUT /api/residuos/{id}` | `200` y registro actualizado; `404` si no existe. |
| `DELETE /api/residuos/{id}` | `204`, sin cuerpo; `404` si no existe. |

Las entradas inválidas, el JSON malformado, una fecha imposible, un tipo no permitido o un id de ruta no numérico producen `400`. Los errores de la API son JSON comprensible, sin trazas internas. Por ejemplo:

```json
{"mensaje":"Entrada inválida.","errores":{"cantidad":"La cantidad debe ser mayor que cero."}}
```

## Probar la API y comprobar un reinicio

En un **segundo terminal PowerShell**, situado en la raíz del repositorio, ejecuta lo siguiente. Las peticiones modifican el archivo configurado para la aplicación; para una demostración separada puedes usar el archivo alternativo del apartado anterior.

```powershell
# Listar: 200; la primera ejecución muestra los tres ejemplos
curl.exe -i http://localhost:8080/api/residuos

# Crear y conservar el id realmente devuelto; normalmente será 4 tras la primera inicialización
$creado = curl.exe -sS -X POST http://localhost:8080/api/residuos -H "Content-Type: application/json" --data-binary "@docs/registro.json" | ConvertFrom-Json
$idResiduo = $creado.id
$creado | ConvertTo-Json

# Consultar: 200
curl.exe -i "http://localhost:8080/api/residuos/$idResiduo"

# Actualizar: 200
curl.exe -i -X PUT "http://localhost:8080/api/residuos/$idResiduo" -H "Content-Type: application/json" --data-binary "@docs/actualizacion.json"
```

Detén la aplicación con `Ctrl+C` en el primer terminal y arráncala de nuevo con el mismo comando y directorio. Mantén abierto el segundo terminal para conservar `$idResiduo`. Comprueba que la modificación sigue disponible:

```powershell
# Tras reiniciar: 200 y datos actualizados
curl.exe -i "http://localhost:8080/api/residuos/$idResiduo"

# Eliminar: 204; consultar después: 404
curl.exe -i -X DELETE "http://localhost:8080/api/residuos/$idResiduo"
curl.exe -i "http://localhost:8080/api/residuos/$idResiduo"

# Cuerpo sin campos obligatorios: 400
curl.exe -i -X POST http://localhost:8080/api/residuos -H "Content-Type: application/json" --data-binary "{}"
```

`-i` muestra el estado y las cabeceras. El POST de creación omite esa opción para poder leer el JSON con PowerShell. En Bash, utiliza `curl` en lugar de `curl.exe` y el id devuelto directamente en las rutas; `ConvertFrom-Json`, `ConvertTo-Json` y las asignaciones anteriores son propias de PowerShell. En Postman, utiliza las mismas rutas y copia los ejemplos al cuerpo **raw / JSON**.

## Pruebas y verificación

Las pruebas de JUnit conservan los casos de servicio y API de la base anterior. Los casos de esta ampliación comprueban el arranque del contexto, las páginas y los formularios, la semilla, el guardado y la recarga de cambios, el archivo vacío y el rechazo de JSON corrupto. MockMvc permite revisar HTTP y HTML sin abrir un navegador.

**Las pruebas utilizan archivos temporales con `@TempDir`; no modifican `data/residuos.json` ni los datos reales de la demostración.** Los informes se generan en `target/surefire-reports/`.

Como antecedente histórico, el **9 de septiembre de 2026**, con **Java 17.0.17**, la base REST en memoria completó 42 pruebas. Se documentó un ciclo TDD real: `crearAsignaIdDelServidorYPermiteConsultar` falló porque la creación devolvía `null`; tras implementar creación y consulta, el mismo comando `mvn -B -ntp -Dtest=RegistroResiduoServiceTest test` pasó. Esta evidencia corresponde a la versión anterior, no verifica la ampliación a Java 21, JSON y Thymeleaf.

En el ciclo TDD de persistencia JSON se ejecutaron 14 pruebas —6 de repositorio y 8 de servicio—: antes de implementar hubo 5 fallos y 0 errores; después aprobaron las 14. Las 6 pruebas de interfaz fallaron inicialmente porque faltaban las rutas y vistas; después aprobaron las 6. La revisión añadió una prueba que primero falló al permitir campos inválidos desde el disco y pasó al aplicar la validación del modelo durante la carga. El conjunto final tiene **55 pruebas: 34 REST, 6 web, 8 de servicio y 7 de persistencia**.

| Verificación actual | Resultado real |
| --- | --- |
| Entorno | 10 de septiembre de 2026, Eclipse Temurin 21.0.3, Maven 3.9.11, Windows. |
| Compilación y pruebas | `mvn -B -ntp clean verify`: `BUILD SUCCESS`, JAR generado; 55 pruebas, 0 fallos, 0 errores, 0 omitidas. |
| Cierre del ciclo TDD de interfaz | `mvn -B -ntp test`: las 6 pruebas web pasaron después de implementar controlador y plantillas. Permanecen verdes en la verificación final. |
| Arranque | `java -jar target/reciclo-0.0.1-SNAPSHOT.jar` usando JDK 21: Tomcat escuchó en 8080. El último arranque registró `Started ReCicloApplication in 2.163 seconds`. |
| CRUD HTTP real | GET `200`; POST `201` con `Location` hacia `/api/residuos/4`; PUT `200`; POST inválido `400`; DELETE `204` sin cuerpo; GET del eliminado `404`. |
| Persistencia tras reiniciar | En `data/residuos.json`, el id 4 actualizado a METAL / 7.25 kg mantuvo sus datos después de reiniciar. Se eliminó y un segundo reinicio confirmó `404` y los tres ejemplos iniciales intactos. |
| Páginas y formularios | Las seis páginas y el CSS respondieron `200`. Revisión visual en anchos de escritorio y estrecho; en el navegador se comprobó validación, registro y edición. También se comprobó la eliminación por POST de formulario. Sin JavaScript. |
| Limitaciones de verificación | Sin bloqueos pendientes. No se ejecutaron los ejemplos en Bash ni en Postman; las peticiones reales se comprobaron con PowerShell y el navegador integrado. |

Se conservaron las 42 pruebas existentes y se añadieron únicamente 13 casos para esta ampliación. Los registros temporales de verificación fueron retirados; la aplicación se dejó encendida con los tres ejemplos.

## Dependencias y relación con el curso

| Dependencia | Propósito |
| --- | --- |
| Spring Boot 3.5.16 | Arranque, configuración automática y versiones compatibles. |
| `spring-boot-starter-web` | Spring MVC, API REST y servidor embebido; incorpora Jackson transitivamente. |
| Jackson / `ObjectMapper` | Serialización de la API y lectura/escritura del archivo de residuos. |
| `spring-boot-starter-validation` | Validación de las entradas de la API y los formularios. |
| `spring-boot-starter-thymeleaf` | Renderizado de las páginas HTML en el servidor. |
| `spring-boot-starter-test` | JUnit, AssertJ, pruebas Spring y MockMvc; Mockito disponible si se necesita. |
| `spring-boot-maven-plugin` | Ejecución y empaquetado del JAR. |

La base demuestra arquitectura Spring Boot, configuración del entorno, controladores, servicio, inyección de dependencias, métodos HTTP, respuestas JSON y pruebas. El archivo JSON y las páginas sencillas permiten exponer el funcionamiento sin instalar una base de datos ni incorporar herramientas JavaScript.

## Continuidad del equipo y estado previo

| Integrante | Trabajo que puede asumir |
| --- | --- |
| 1 | Mantener el contrato REST, las validaciones y los ejemplos de peticiones. |
| 2 | Revisar el servicio y la persistencia; preparar el diseño de una futura base de datos. |
| 3 | Mantener las vistas y los formularios HTML, conservando su sencillez. |
| 4 | Mantener las pruebas y la documentación y preparar la demostración reproducible. |

Usuarios, autenticación, base de datos, centros de reciclaje, categorías, estadísticas y reportes quedan pendientes para futuros avances. No se incorporan autenticación, seguridad, servicios externos, mapas, notificaciones, gamificación, pagos ni otros módulos en esta versión.

La revisión anterior encontró un repositorio con sólo un README y creó la base REST. Al comenzar esta ampliación ya existían el proyecto Maven con Java 17 y Spring Boot 3.5.16, el modelo, las capas, las validaciones y las pruebas: se conserva ese trabajo y se amplía dentro de los mismos paquetes. La selección de Java debe revisarse tanto en `PATH` como en `JAVA_HOME`, porque el entorno anterior usaba versiones distintas para `java` y Maven. No se realizan commits ni push automáticos.

El proceso anterior mantenía abierto el JAR y bloqueó el primer intento de `clean verify`; se detuvo y la compilación posterior pasó. No se encontraron implementaciones de otros integrantes que fuera necesario reemplazar.

## Archivos de esta ampliación

Respecto de la base REST anterior, se modificaron 10 archivos:

- `pom.xml`, `.gitignore`, `README.md` y `src/main/resources/application.properties`.
- `model/RegistroResiduo.java`, `model/TipoResiduo.java`, `repository/RegistroResiduoRepository.java` y `controller/ApiExceptionHandler.java`, dentro de `src/main/java/pe/reciclo/`.
- `controller/RegistroResiduoControllerTest.java` y `service/RegistroResiduoServiceTest.java`, dentro de `src/test/java/pe/reciclo/`.

Se crearon 12 archivos:

- `src/main/java/pe/reciclo/controller/ResiduoWebController.java`.
- `src/main/resources/residuos.json` y `src/main/resources/static/css/estilos.css`.
- En `src/main/resources/templates/`: `fragmentos.html`, `inicio.html`, `error.html`, `residuos/lista.html`, `residuos/formulario.html`, `residuos/detalle.html` y `residuos/eliminar.html`.
- En `src/test/java/pe/reciclo/`: `controller/ResiduoWebControllerTest.java` y `repository/RegistroResiduoRepositoryTest.java`.

Además, la ejecución genera `data/residuos.json`, ignorado por Git. El controlador REST y el servicio existentes conservan sus rutas, nombres y responsabilidades. Todos los cambios siguen locales, sin commit ni push.
