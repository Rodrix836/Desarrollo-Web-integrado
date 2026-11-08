package pe.reciclo.controller;

/**
 * Soporta el formato original (usuario/clave) y el formato estándar (username/password).
 */
public record LoginRequest(
        String usuario,
        String clave,
        String username,
        String password
) {
    public LoginRequest(String usuario, String clave) {
        this(usuario, clave, null, null);
    }
}
