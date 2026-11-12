package pe.reciclo.auth.controller;

import pe.reciclo.auth.dto.UsuarioResponse;
import pe.reciclo.auth.model.Rol;
import pe.reciclo.auth.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    public ResponseEntity<UsuarioResponse> cambiarRol(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String rolStr = body.get("rol");
        if (rolStr == null || rolStr.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Rol nuevoRol;
        try {
            nuevoRol = Rol.valueOf(rolStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(usuarioService.cambiarRol(id, nuevoRol));
    }
}