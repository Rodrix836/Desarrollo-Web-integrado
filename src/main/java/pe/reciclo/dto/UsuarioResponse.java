package pe.reciclo.dto;

import pe.reciclo.model.Rol;
import pe.reciclo.model.Usuario;

import java.time.LocalDateTime;

public record UsuarioResponse(
        Long id,
        String username,
        String email,
        String nombre,
        Rol rol,
        String empresa,
        boolean activo,
        LocalDateTime creadoEn
) {
    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(
                u.getId(), u.getUsername(), u.getEmail(), u.getNombre(),
                u.getRol(), u.getEmpresa(), u.isActivo(), u.getCreadoEn()
        );
    }
}
