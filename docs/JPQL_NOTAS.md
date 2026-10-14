# Implementación JPQL - ReCiclo

## 1. Qué hice

Implementé tres consultas JPQL: buscar por empresa, buscar por cantidad mayor y ordenar por cantidad descendente.
JPQL consulta entidades y atributos Java en lugar de trabajar directamente con tablas y columnas.
Las consultas están en una interfaz Spring Data JPA que extiende `JpaRepository` y utiliza `@Query` y `@Param`.
Un Controller recibe las peticiones GET y un Service llama al Repository.
Como el proyecto todavía guarda sus datos en JSON, añadí únicamente JPA, H2 y una entidad para ejecutar estas consultas.
Antes de cada consulta se copia la lista actual a H2 en memoria; el archivo JSON sigue siendo el origen de los datos.

La copia temporal se reconstruye en cada petición JPQL, incluyendo altas, cambios y eliminaciones del CRUD existente.
Se consulta la lista capturada al comenzar esa petición. Esta preparación es para la demostración académica con pocos registros;
no es una migración de la persistencia. H2 no necesita instalación y su contenido temporal desaparece al cerrar la aplicación.

## 2. Archivo principal

`src/main/java/pe/reciclo/repository/RegistroResiduoJpqlRepository.java`

**Aquí se encuentra mi implementación principal de JPQL.**

La interfaz lleva `@Repository` y extiende `JpaRepository<RegistroResiduoEntity, Long>`.
`RegistroResiduoEntity` es la entidad Java; `empresa` y `cantidad` son sus atributos Java.
El modelo existente `RegistroResiduo` se conserva como respuesta JSON.

## 3. Consultas implementadas

### Buscar por empresa

Método: `buscarPorEmpresa(@Param("empresa") String empresa)`.

```java
@Query("""
        SELECT r FROM RegistroResiduoEntity r
        WHERE LOWER(r.empresa) LIKE LOWER(CONCAT('%', :empresa, '%'))
        """)
List<RegistroResiduoEntity> buscarPorEmpresa(@Param("empresa") String empresa);
```

El parámetro `empresa` permite encontrar coincidencias parciales sin distinguir mayúsculas y minúsculas.
Por ejemplo, `ecoenv` encuentra `EcoEnvases`. `@Param` vincula el argumento Java con `:empresa`.
La consulta utiliza los comodines de `LIKE`: `%` representa cualquier secuencia y `_` un carácter.

### Cantidad mayor que

Método: `buscarCantidadMayorQue(@Param("cantidad") BigDecimal cantidad)`.

```java
@Query("""
        SELECT r FROM RegistroResiduoEntity r
        WHERE r.cantidad > :cantidad
        """)
List<RegistroResiduoEntity> buscarCantidadMayorQue(@Param("cantidad") BigDecimal cantidad);
```

El parámetro `cantidad` es numérico y admite decimales. Devuelve únicamente cantidades estrictamente mayores:
si se envía `3`, un registro con cantidad `3` queda excluido. No realiza conversiones de unidades.

### Ordenados por cantidad

Método: `listarOrdenadosPorCantidad()`; no recibe parámetros.

```java
@Query("""
        SELECT r FROM RegistroResiduoEntity r
        ORDER BY r.cantidad DESC
        """)
List<RegistroResiduoEntity> listarOrdenadosPorCantidad();
```

Devuelve todos los registros de mayor a menor cantidad. Entre cantidades iguales no se exige un orden particular.
Las tres consultas son JPQL: usan la entidad Java, no nombres de tablas ni consultas nativas.

## 4. Flujo de mi implementación

```text
RegistroResiduoJpqlController
↓
RegistroResiduoJpqlService
↓
RegistroResiduoJpqlRepository
↓
JPQL
↓
Base de datos H2 en memoria
```

El Service prepara la copia de los registros actuales, llama a la consulta y devuelve los resultados como `RegistroResiduo`.

## 5. Endpoints para demostrar

Con JDK 21 y Maven, desde la raíz del proyecto:

```text
mvn test
mvn clean package
java -jar target/reciclo-0.0.1-SNAPSHOT.jar
```

Abre las siguientes URL en el navegador o utiliza el método GET en Postman:

| Endpoint | Resultado con los tres registros originales |
| --- | --- |
| `GET http://localhost:8080/api/residuos/jpql/empresa?empresa=ecoenv` | EcoEnvases, id 1, cantidad 2. |
| `GET http://localhost:8080/api/residuos/jpql/cantidad?cantidad=3` | Papelera Andina, id 2, cantidad 5. Excluye cantidad 3. |
| `GET http://localhost:8080/api/residuos/jpql/ordenados` | Cantidades 5, 3, 2; ids 2, 3, 1. |

También puedes enviar `cantidad=10`: con los datos originales devuelve `[]`, porque ninguno supera 10.
`empresa=Papelera%20Andina` demuestra un nombre con espacios. `empresa=Empresa%20Demo` devuelve `[]`
si no existe un registro de esa empresa. Los resultados dependen de los datos actuales de `data/residuos.json`.
Las respuestas conservan todos los campos del registro. Un parámetro obligatorio ausente o una cantidad no numérica devuelve 400.

Para repetir la demostración con la semilla sin modificar tus datos habituales, inicia el JAR con
`--reciclo.datos.archivo=data/demo-jpql.json`, usando una ruta que todavía no exista.
La aplicación reutiliza su mecanismo existente de carga de los tres registros originales.

## 6. Cómo explicarlo al profesor

“Mi parte del proyecto fue JPQL. JPQL es un lenguaje de consultas de JPA que trabaja con entidades y atributos Java,
en lugar de tablas y columnas directamente. Lo implementé en RegistroResiduoJpqlRepository usando JpaRepository,
@Query y @Param. La primera consulta recibe parte del nombre de una empresa; la segunda busca cantidades mayores
que un valor, y la tercera ordena de mayor a menor. El Controller recibe la petición, el Service llama al Repository
y JPA ejecuta la consulta en H2. Como este proyecto todavía usa JSON, se prepara una copia temporal de sus registros
para demostrar JPQL con los datos actuales.”

## 7. Qué mostrar durante la exposición

1. **PRIMERO (20 segundos):** abre `src/main/java/pe/reciclo/repository/RegistroResiduoJpqlRepository.java`.
   Señala `@Repository` y `extends JpaRepository<RegistroResiduoEntity, Long>`.
2. **SEGUNDO (30 segundos):** muestra `buscarPorEmpresa`, su `@Query`, `r.empresa` y `@Param("empresa")`.
3. **TERCERO (20 segundos):** ejecuta `GET http://localhost:8080/api/residuos/jpql/empresa?empresa=ecoenv`.
4. **CUARTO (20 segundos):** muestra el JSON de EcoEnvases. Ejecuta los endpoints de cantidad con `3`
   y de ordenados para mostrar cantidad 5 y luego la secuencia 5, 3, 2, usando los registros originales.

Arranca la aplicación antes de la exposición. La demostración dura aproximadamente 90 segundos.
