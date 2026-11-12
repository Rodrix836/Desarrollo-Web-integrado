# Auth de ReCiclo

Revisión de la entrega de Adrián integrada en main. Servicio Java 21 en 8081.

Desde la raíz del repositorio inicia el servicio con:

```powershell
powershell.exe -ExecutionPolicy Bypass -File .\scripts\iniciar-api.ps1 -Servicio auth
```

El script configura una clave local compartida con Residuos y conserva la base en auth-api/data.
Para ejecutar manualmente, configura RECICLO_JWT_SECRET con al menos 32 bytes UTF-8.

POST /api/auth/login recibe `{"username":"admin","password":"admin123"}`.
POST /api/auth/register recibe username, password y nombre y crea un usuario USER.
GET /api/auth/me requiere Bearer.
GET /api/usuarios y PUT /api/usuarios/{id}/rol requieren ADMIN.
El PUT recibe `{"rol":"USER"}`.

La base vacía crea admin/admin123, user/user123 y empresa/empresa123.
La cuenta EMPRESA no tiene los permisos USER/ADMIN del catálogo.
La firma se fija en HS256 para coincidir con Residuos. Los roles incluidos en un JWT se mantienen hasta su vencimiento. Después de un cambio de rol, inicia sesión otra vez.

Pruebas reales en src/test/java se ejecutan con:
```powershell
mvn.cmd -f auth-api/pom.xml clean verify
```

Lee la [guía Windows](../docs/GUIA_FRONTEND_WINDOWS.txt) para el frontend completo.
