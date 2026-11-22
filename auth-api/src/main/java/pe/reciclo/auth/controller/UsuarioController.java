package pe.reciclo.auth.controller;

import jakarta.validation.Valid;
import pe.reciclo.auth.dto.ActualizarUsuarioRequest;
import pe.reciclo.auth.dto.CambiarEstadoRequest;
import pe.reciclo.auth.dto.CambiarRolRequest;
import pe.reciclo.auth.dto.UsuarioResponse;
import pe.reciclo.auth.model.Rol;
import pe.reciclo.auth.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

/** Administración de cuentas: sólo ADMIN (ver SecurityConfig). */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    @PutMapping("/{id}/rol")
    public ResponseEntity<UsuarioResponse> cambiarRol(@PathVariable Long id,
                                                      @Valid @RequestBody CambiarRolRequest body,
                                                      Authentication admin) {
        Rol nuevoRol;
        try {
            nuevoRol = Rol.valueOf(body.rol().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol inválido. Usa ADMIN o USER.");
        }
        return ResponseEntity.ok(usuarioService.cambiarRol(id, nuevoRol, admin.getName()));
    }

    @PutMapping("/{id}")
    public UsuarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarUsuarioRequest body) {
        return usuarioService.actualizar(id, body);
    }

    @PutMapping("/{id}/estado")
    public UsuarioResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoRequest body,
                                         Authentication admin) {
        return usuarioService.cambiarEstado(id, body.activo(), admin.getName());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, Authentication admin) {
        usuarioService.eliminar(id, admin.getName());
        return ResponseEntity.noContent().build();
    }
}
