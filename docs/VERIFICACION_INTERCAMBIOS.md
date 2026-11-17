# Verificación de intercambios programados

Fecha: 16 de noviembre de 2026. Entorno: Windows, JDK 21.0.3, Node 22.17.1, npm 10.9.2 y Google Chrome instalado.

## Resultados ejecutados

| Verificación | Resultado |
| --- | --- |
| mvn.cmd -f auth-api/pom.xml clean verify | 6 pruebas; 0 fallos, errores u omisiones. |
| mvn.cmd -f residuos-api/pom.xml clean verify | 53 pruebas; 0 fallos, errores u omisiones. |
| mvn.cmd -f consultas-api/pom.xml clean verify | 6 pruebas; 0 fallos, errores u omisiones. |
| npm.cmd run build, desde frontend | Compilación de producción correcta. |
| npm.cmd run test:e2e, con las tres APIs y Angular activos | 6 recorridos de navegador correctos. |

Las suites se ejecutaron sobre bases de prueba, sin modificar la base de la copia habitual del usuario. Las pruebas antiguas del proyecto raíz Thymeleaf quedan fuera de estas tres suites; estos resultados no afirman que ese proyecto anterior pase todas sus pruebas.

## Ciclo de prueba previo a implementación

Se añadió primero un escenario de integración para crear un material programado y comprobar su visibilidad al cambiar el día de Lima. Antes de implementar el cambio falló en la creación: esperaba 201 y recibió 400 porque el contrato anterior todavía exigía la fecha histórica. Después se implementó fechaPublicacion y la asignación de fecha histórica en el servidor, y el escenario pasó junto con la suite ampliada.

## Reglas cubiertas

- Campos condicionados al check; guardado sin publicar y normalización de solicitudes contradictorias.
- Empresa, preferencia y publicación obligatorias al habilitar; rechazo de preferencias no catalogadas y nuevas fechas pasadas.
- Cantidad positiva y decimal, kg en nuevas altas y conservación de unidades históricas.
- USER sin acceso al listado ni al detalle completo de materiales privados o futuros.
- Aviso futuro de resumen; publicación al cruzar 04:59:59Z a 05:00:00Z en America/Lima.
- Conservación de la fecha pasada de un material ya publicado.
- Identidad estable del aviso al reprogramar, cancelar y eliminar.
- Registro histórico habilitado sin fecha de publicación; preferencia antigua y unidad preservadas.
- Filtros ADMIN que no amplían los permisos USER; filtros de Consultas y reenvío Bearer.
- Navegador: ADMIN CRUD, USER, registro, roles, formulario condicionado, filtros conservados y ciclo completo de programación.
- Vista móvil de avisos a 390 px sin desbordamiento horizontal.

## Migración y reinicio reales

Se preparó una base H2 de archivo con el esquema anterior, sin columna fecha_publicacion, y un registro con fecha 2020-01-02, unidad toneladas y preferencia Cobre limpio. Al arrancar el nuevo JAR, Hibernate añadió la columna nullable; el registro siguió visible para USER como DISPONIBLE con sus valores originales.

También se creó un material con cantidad 8.123456 y publicación futura, se reiniciaron los procesos Auth y Residuos usando los mismos archivos H2 y se inició una sesión nueva. Se recuperaron la cantidad exacta, la fecha y el estado PROGRAMADO; el aviso seguía apareciendo una sola vez para ese ID.

## Repetir

Los comandos, cuentas, rutas, política de compatibilidad y pasos manuales están en [GUIA_INTERCAMBIOS_PROGRAMADOS.txt](GUIA_INTERCAMBIOS_PROGRAMADOS.txt). Las pruebas de calendario utilizan Clock inyectable; no hay que cambiar el reloj de Windows.
