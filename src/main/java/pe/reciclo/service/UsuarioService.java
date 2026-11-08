package pe.reciclo.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pe.reciclo.dto.UsuarioResponse;
import pe.reciclo.model.Rol;
import pe.reciclo.model.Usuario;
import pe.reciclo.repository.UsuarioRepository;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public List<UsuarioResponse> listarTodos() {
        return usuarioRepository.findAll().stream().map(UsuarioResponse::from).toList();
    }

    public UsuarioResponse obtenerPorId(Long id) {
        return UsuarioResponse.from(buscar(id));
    }

    public UsuarioResponse cambiarRol(Long id, Rol nuevoRol) {
        Usuario usuario = buscar(id);
        usuario.setRol(nuevoRol);
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    public void desactivar(Long id) {
        Usuario usuario = buscar(id);
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }
}
