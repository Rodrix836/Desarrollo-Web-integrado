package pe.reciclo.auth.service;

import pe.reciclo.auth.dto.ActualizarUsuarioRequest;
import pe.reciclo.auth.dto.UsuarioResponse;
import pe.reciclo.auth.model.Rol;
import pe.reciclo.auth.model.Usuario;
import pe.reciclo.auth.repository.UsuarioRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UsuarioResponse> listarTodos() {
        return usuarioRepository.findAll(Sort.by("id")).stream()
                .map(UsuarioResponse::from)
                .toList();
    }

    public UsuarioResponse obtenerPorUsername(String username) {
        return UsuarioResponse.from(buscarPorUsername(username));
    }

    @Transactional
    public UsuarioResponse cambiarRol(Long id, Rol nuevoRol, String actual) {
        Usuario u = buscar(id);
        if (u.getUsername().equals(actual) && nuevoRol != Rol.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes quitarte tu propio rol de administrador.");
        }
        u.setRol(nuevoRol);
        usuarioRepository.save(u);
        return UsuarioResponse.from(u);
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, ActualizarUsuarioRequest datos) {
        Usuario u = buscar(id);
        u.setNombre(datos.nombre().trim());
        return UsuarioResponse.from(usuarioRepository.save(u));
    }

    @Transactional
    public UsuarioResponse cambiarEstado(Long id, boolean activo, String actual) {
        Usuario u = buscar(id);
        if (!activo && u.getUsername().equals(actual)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes desactivar tu propia cuenta.");
        }
        u.setActivo(activo);
        return UsuarioResponse.from(usuarioRepository.save(u));
    }

    @Transactional
    public void eliminar(Long id, String actual) {
        Usuario u = buscar(id);
        if (u.getUsername().equals(actual)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes eliminar tu propia cuenta.");
        }
        usuarioRepository.delete(u);
    }

    @Transactional
    public UsuarioResponse actualizarPerfil(String username, String nombre) {
        Usuario u = buscarPorUsername(username);
        u.setNombre(nombre.trim());
        return UsuarioResponse.from(usuarioRepository.save(u));
    }

    @Transactional
    public void cambiarPassword(String username, String actual, String nueva) {
        Usuario u = buscarPorUsername(username);
        if (!passwordEncoder.matches(actual, u.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña actual no es correcta.");
        }
        if (actual.equals(nueva)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nueva contraseña debe ser distinta.");
        }
        u.setPasswordHash(passwordEncoder.encode(nueva));
        usuarioRepository.save(u);
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private Usuario buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }
}
