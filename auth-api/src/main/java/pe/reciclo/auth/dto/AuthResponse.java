package pe.reciclo.auth.dto;

public record AuthResponse(
        String token,
        String username,
        String rol,
        String nombre
) {
}