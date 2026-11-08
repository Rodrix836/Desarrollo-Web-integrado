package pe.reciclo.dto;

import pe.reciclo.model.Rol;
import pe.reciclo.model.Usuario;

public record AuthResponse(
        String token,
        String tipo,
        Long id,
        String username,
        String email,
        String nombre,
        Rol rol,
        String empresa
) {
    public static AuthResponse of(String token, Usuario u) {
        return new AuthResponse(
                token, "Bearer", u.getId(), u.getUsername(), u.getEmail(),
                u.getNombre(), u.getRol(), u.getEmpresa()
        );
    }

    public static AuthResponse sinToken(Usuario u) {
        return new AuthResponse(
                null, "Bearer", u.getId(), u.getUsername(), u.getEmail(),
                u.getNombre(), u.getRol(), u.getEmpresa()
        );
    }
}
