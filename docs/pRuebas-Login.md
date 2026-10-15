@baseUrl = http://localhost:8080

### Iniciar sesión 
# @name login
POST {{baseUrl}}/api/auth/login
Content-Type: application/json

{
  "usuario": "admin",
  "clave": "admin123"
}

### Guardamos el token
@token = {{login.response.body.$.token}}

### GET con token -> 200 OK
GET {{baseUrl}}/api/residuos
Authorization: Bearer {{token}}

### POST como admin -> 201 Created
POST {{baseUrl}}/api/residuos
Authorization: Bearer {{token}}
Content-Type: application/json

{
  "empresa": "Empresa Demo",
  "tipo": "PLASTICO",
  "cantidadKg": 15.5,
  "fecha": "2026-10-14"
}