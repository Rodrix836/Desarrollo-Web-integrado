# Integrante 1 — Auth integrado en ReCiclo

## Qué se implementó dentro del proyecto original

1. **Usuarios en base de datos (JPA)**  
   - Entidad `Usuario` + `UsuarioRepository`  
   - Ya no se usan usuarios solo en memoria

2. **API de autenticación ampliada**
   - `POST /api/auth/login` → JWT + datos del usuario  
   - `POST /api/auth/register` → registro  
   - `GET  /api/auth/me` → perfil (requiere token)

3. **Gestión de usuarios (solo ADMIN)**
   - `GET/PUT/DELETE /api/usuarios/**`

4. **Spring Security**
   - `CustomUserDetailsService` lee usuarios de la BD  
   - Roles: `ADMIN`, `EMPRESA`, `USER`  
   - CORS para Angular (`localhost:4200`)

5. **Login web (Thymeleaf)**  
   - Sigue funcionando con `AuthService.autenticar()`  
   - Usa los mismos usuarios de la BD

## Credenciales demo

| Usuario  | Clave       | Rol     |
|----------|-------------|---------|
| admin    | admin123    | ADMIN   |
| empresa  | empresa123  | EMPRESA |
| user     | user123     | USER    |

## Archivos principales (tu responsabilidad)

- `model/Usuario.java`, `model/Rol.java`
- `repository/UsuarioRepository.java`
- `security/CustomUserDetailsService.java`, `JwtService`, `JwtAuthenticationFilter`
- `service/AuthService.java`, `UsuarioService.java`
- `controller/AuthController.java`, `UsuarioController.java`
- `config/SecurityConfig.java`, `DataLoaderUsuarios.java`, `CorsConfig.java`
- `dto/*`

El resto (residuos, JPQL, web empresa) corresponde a los otros integrantes.
