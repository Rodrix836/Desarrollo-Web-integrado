package pe.reciclo.auth.dto;

import pe.reciclo.auth.model.Rol;
import pe.reciclo.auth.model.Usuario;

public record UsuarioResponse(
        Long id,
        String username,
        String nombre,
        Rol rol,
        String empresa
) {
    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(
                u.getId(),
                u.getUsername(),
                u.getNombre(),
                u.getRol(),
                u.getEmpresa()
        );
    }
}