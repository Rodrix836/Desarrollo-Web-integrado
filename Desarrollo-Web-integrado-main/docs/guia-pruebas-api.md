# Guía de pruebas de la API REST de ReCiclo

## 1. Requisitos

Con la aplicación ejecutándose en `http://localhost:8080`, se puede probar la API con:

- `curl` desde PowerShell o una terminal.
- Postman, importando `ReCiclo.postman_collection.json`.

## 2. Métodos HTTP implementados

| Método | Endpoint | Uso |
|---|---|---|
| GET | `/api/residuos` | Listar todos los residuos |
| GET | `/api/residuos/{id}` | Consultar un residuo |
| POST | `/api/residuos` | Crear un residuo |
| PUT | `/api/residuos/{id}` | Actualizar un residuo |
| DELETE | `/api/residuos/{id}` | Eliminar un residuo |

## 3. Pruebas con curl

### GET

```powershell
curl.exe -i http://localhost:8080/api/residuos
```

### POST

```powershell
curl.exe -i -X POST http://localhost:8080/api/residuos `
  -H "Content-Type: application/json" `
  --data-binary "@docs/registro.json"
```

### GET por id

```powershell
curl.exe -i http://localhost:8080/api/residuos/1
```

### PUT

```powershell
curl.exe -i -X PUT http://localhost:8080/api/residuos/1 `
  -H "Content-Type: application/json" `
  --data-binary "@docs/actualizacion.json"
```

### DELETE

```powershell
curl.exe -i -X DELETE http://localhost:8080/api/residuos/1
```

## 4. Estados esperados

- `200 OK`: consulta o actualización correcta.
- `201 Created`: creación correcta.
- `204 No Content`: eliminación correcta.
- `400 Bad Request`: datos inválidos.
- `404 Not Found`: residuo inexistente.

La API devuelve JSON y valida los campos obligatorios mediante Bean Validation.
