package pe.reciclo.auth.service;

import pe.reciclo.auth.dto.UsuarioResponse;
import pe.reciclo.auth.model.Rol;
import pe.reciclo.auth.model.Usuario;
import pe.reciclo.auth.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public List<UsuarioResponse> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioResponse::from)
                .toList();
    }

    public UsuarioResponse obtenerPorUsername(String username) {
        Usuario u = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        return UsuarioResponse.from(u);
    }

    public UsuarioResponse cambiarRol(Long id, Rol nuevoRol) {
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        u.setRol(nuevoRol);
        usuarioRepository.save(u);
        return UsuarioResponse.from(u);
    }
}