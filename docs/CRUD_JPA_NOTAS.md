# CRUD con JPA / Hibernate - ReCiclo

## Qué se implementó

El CRUD de residuos ahora guarda, consulta, actualiza y elimina directamente en H2 mediante Spring Data JPA y Hibernate.
La base de datos es la fuente principal de datos y se conserva en archivo al cerrar la aplicación.
Los identificadores se generan con @GeneratedValue(strategy = GenerationType.IDENTITY).
Los endpoints, las validaciones y el formato JSON del CRUD se mantienen.

## Dónde está el código principal

src/main/java/pe/reciclo/service/RegistroResiduoService.java

- listar(): repository.findAll()
- consultar(id): repository.findById(id)
- crear(registro): repository.save(new RegistroResiduoEntity(registro))
- actualizar(id, registro): findById(id), actualizarDesde(registro) y save(existente)
- eliminar(id): existsById(id) y deleteById(id)

POST ignora cualquier ID enviado por el cliente y obtiene uno generado por la base de datos.
PUT utiliza el ID de la ruta, conserva ese ID y devuelve 404 si no existe.
DELETE devuelve 404 si el registro no existe.

## Repositorio compartido

src/main/java/pe/reciclo/repository/RegistroResiduoJpaRepository.java

Esta interfaz extiende JpaRepository<RegistroResiduoEntity, Long>.
Lleva @NoRepositoryBean porque es la interfaz base del repositorio existente.
RegistroResiduoJpqlRepository hereda de ella, conserva las tres @Query y es el único repositorio concreto que Spring crea.
El CRUD y JPQL acceden a la misma entidad y a la misma base H2.

Flujo:
Controller CRUD -> Service CRUD -> RegistroResiduoJpaRepository -> JPA/Hibernate -> H2

## Persistencia y carga inicial

Configuración de src/main/resources/application.properties:

spring.datasource.url=jdbc:h2:file:./data/reciclodb
spring.jpa.hibernate.ddl-auto=update

La base se almacena en data/reciclodb.mv.db, relativa a la carpeta desde donde arrancas la aplicación.
Para conservar los mismos datos, inicia siempre desde la raíz del proyecto y usa la misma URL de H2.

CargaInicialResiduos consulta repository.count() al arrancar.
Si hay datos en H2, no lee ni importa JSON.
Si H2 está vacía, usa data/residuos.json cuando existe; en caso contrario, usa la semilla original incluida en recursos.
Valida todos los registros antes de guardarlos. El archivo JSON no se sobrescribe.
Los IDs de la semilla se ignoran: JPA/Hibernate genera nuevos IDs; pueden diferir de los del antiguo archivo.
El CRUD ya no usa el repositorio de archivos ni un contador manual de IDs.

Con la carga de semilla activada, una base completamente vacía vuelve a recibir la semilla al arrancar.
Para mantenerla vacía o desactivar la importación inicial, usa:
--reciclo.datos.cargar-semilla=false

## Qué ocurrió con JPQL

Las tres consultas @Query, sus nombres y parámetros se conservaron exactamente.
El controlador JPQL, el modelo de respuesta RegistroResiduo y las seis pruebas JPQL existentes no se editaron.
Las consultas no se movieron ni se reemplazaron.

Fue imprescindible adaptar RegistroResiduoJpqlService:
se retiró prepararDatos(), que antes borraba H2 y la recargaba desde JSON en cada consulta.
Mantener ese código con una base persistente habría borrado o reemplazado los datos del CRUD.
El servicio ahora ejecuta las mismas consultas sobre los datos ya guardados en H2.
También se ajustó la herencia de RegistroResiduoJpqlRepository para compartir la interfaz CRUD, sin cambiar sus @Query.

docs/JPQL_NOTAS.md conserva las notas del avance anterior. Sus referencias a una copia temporal de JSON
describen la implementación anterior; para el almacenamiento actual corresponde esta nota de CRUD.

## Cómo encender el servidor

Requiere Java 21 y Maven. Desde la raíz del proyecto:

mvn test
mvn clean package
mvn spring-boot:run

También puedes iniciar el JAR generado:
java -jar target/reciclo-0.0.1-SNAPSHOT.jar

Página: http://localhost:8080
API: http://localhost:8080/api/residuos
Para detener el servidor: Ctrl+C.

## Demostración del CRUD y del reinicio

1. En Postman, envía POST http://localhost:8080/api/residuos
   con Content-Type: application/json y este cuerpo:

{
  "tipoResiduo": "VIDRIO",
  "cantidad": 18.125,
  "unidadMedida": "kg",
  "fecha": "2026-10-14",
  "empresa": "Empresa Persistente",
  "descripcion": "Registro para demostrar JPA",
  "intercambioPor": "Papel",
  "disponible": true
}

2. Copia el ID devuelto por POST; no supongas que siempre será 1.
3. Ejecuta GET http://localhost:8080/api/residuos/ID
4. Detén completamente Spring Boot con Ctrl+C.
5. Vuelve a arrancarlo desde la misma carpeta.
6. Ejecuta el mismo GET: el residuo debe conservar el ID y todos sus datos.
7. Prueba PUT a la misma URL con el cuerpo completo y otra cantidad.
8. Prueba DELETE a la misma URL: devuelve 204; un GET posterior devuelve 404.

## Consultas JPQL conservadas

- GET /api/residuos/jpql/empresa?empresa=ecoenv
  Buscar por empresa, con coincidencia parcial e ignorando mayúsculas/minúsculas.
- GET /api/residuos/jpql/cantidad?cantidad=3
  Cantidades estrictamente mayores que 3.
- GET /api/residuos/jpql/ordenados
  Registros ordenados por cantidad descendente.

## Verificación realizada

- mvn test: OK, 72 pruebas, sin fallos ni errores.
- mvn clean package: OK, JAR generado y 72 pruebas aprobadas.
- mvn spring-boot:run: OK.
- GET: 200; GET por ID: 200; POST: 201; PUT: 200; DELETE: 204.
- PUT de ID inexistente: 404, sin crear un registro.
- POST con ID del cliente: usa un ID generado nuevo.
- Decimales como 12.5001 y 20.0001: conservados.
- Las tres consultas JPQL: OK antes y después del reinicio; no alteraron IDs ni datos.
- Persistencia: POST creó el ID 5 con empresa "Persistencia reinicio" y cantidad 18.125.
  Se detuvo completamente Spring Boot y se arrancó el JAR con la misma base de prueba.
  GET recuperó el mismo ID, empresa y cantidad; la semilla no se duplicó.
- La generación de IDs continuó correctamente después del reinicio.
- Las pruebas HTTP usaron el puerto 18089 y una base H2 de prueba separada.
- Los archivos de datos y los registros de ejecución no se incorporan a Git.

Se conservaron las seis pruebas JPQL.
Las ocho pruebas del almacenamiento JSON retirado se sustituyeron por cinco pruebas de carga inicial.
Los diez casos de Service se adaptaron a H2 y se añadió uno que confirma que el Service y JpaRepository comparten los mismos datos.
Las pruebas automatizadas usan bases H2 en memoria aisladas; la prueba de reinicio se realizó con H2 en archivo.

## Explicación breve para la exposición

"Ahora el CRUD de ReCiclo usa Spring Data JPA y Hibernate. El Controller recibe la petición y el Service
utiliza JpaRepository para consultar, guardar, actualizar y eliminar los residuos.
Los IDs los genera la base de datos. H2 está configurada en archivo y con ddl-auto=update,
por eso los datos sobreviven cuando cierro y vuelvo a iniciar Spring Boot.
Las tres consultas JPQL existentes se mantienen y consultan la misma entidad y la misma base de datos."

Para mostrar el código, abre primero RegistroResiduoService.java, después RegistroResiduoJpaRepository.java
y finalmente RegistroResiduoEntity.java para señalar @Entity, @Id y @GeneratedValue.
