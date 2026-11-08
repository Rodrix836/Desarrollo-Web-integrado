package pe.reciclo.model;

public record UsuarioSesion(
        String username,
        String nombre,
        Rol rol,
        String empresa
) {}
