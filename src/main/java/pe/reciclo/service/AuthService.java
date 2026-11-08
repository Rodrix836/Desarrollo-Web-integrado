package pe.reciclo.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pe.reciclo.dto.AuthResponse;
import pe.reciclo.dto.RegisterRequest;
import pe.reciclo.model.Rol;
import pe.reciclo.model.Usuario;
import pe.reciclo.model.UsuarioSesion;
import pe.reciclo.repository.UsuarioRepository;
import pe.reciclo.security.JwtService;

import java.util.Optional;

/**
 * Servicio de autenticación (Integrante 1).
 * - autenticar(): usado por el login web (sesión)
 * - loginApi / register / me: usados por la API REST + JWT
 */
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Login web (Thymeleaf / sesión) — mantiene contrato original */
    public Optional<UsuarioSesion> autenticar(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }

        return usuarioRepository.findByUsername(username.trim().toLowerCase())
                .filter(Usuario::isActivo)
                .filter(u -> passwordEncoder.matches(password, u.getPassword()))
                .map(u -> new UsuarioSesion(
                        u.getUsername(),
                        u.getNombre(),
                        u.getRol(),
                        u.getEmpresa() != null ? u.getEmpresa() : ""
                ));
    }

    /** Login API → JWT */
    public AuthResponse loginApi(String username, String password) {
        Usuario usuario = usuarioRepository.findByUsername(username.trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));

        if (!usuario.isActivo()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario desactivado");
        }
        if (!passwordEncoder.matches(password, usuario.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }

        String token = jwtService.generarToken(usuario.getUsername(), usuario.getRol().name());
        return AuthResponse.of(token, usuario);
    }

    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim().toLowerCase();
        String email = request.email().trim().toLowerCase();

        if (usuarioRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El username ya está en uso");
        }
        if (usuarioRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El email ya está registrado");
        }

        Rol rol = request.rol() != null ? request.rol() : Rol.USER;
        if (rol == Rol.EMPRESA && (request.empresa() == null || request.empresa().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las empresas deben indicar el nombre de la empresa");
        }

        Usuario nuevo = new Usuario(
                username,
                email,
                passwordEncoder.encode(request.password()),
                request.nombre().trim(),
                rol,
                request.empresa() != null ? request.empresa().trim() : null
        );

        Usuario guardado = usuarioRepository.save(nuevo);
        String token = jwtService.generarToken(guardado.getUsername(), guardado.getRol().name());
        return AuthResponse.of(token, guardado);
    }

    public AuthResponse me(String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        return AuthResponse.sinToken(usuario);
    }
}
