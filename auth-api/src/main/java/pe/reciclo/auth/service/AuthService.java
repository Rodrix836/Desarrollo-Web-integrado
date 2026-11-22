package pe.reciclo.auth.service;

import pe.reciclo.auth.dto.AuthResponse;
import pe.reciclo.auth.dto.LoginRequest;
import pe.reciclo.auth.dto.RegisterRequest;
import pe.reciclo.auth.model.Rol;
import pe.reciclo.auth.model.Usuario;
import pe.reciclo.auth.repository.UsuarioRepository;
import pe.reciclo.auth.security.JwtService;
import pe.reciclo.auth.security.LimiteIntentosLogin;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService implements UserDetailsService {

    private static final String CREDENCIALES_INVALIDAS = "Credenciales inválidas";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LimiteIntentosLogin limite;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       LimiteIntentosLogin limite) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.limite = limite;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario u = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        return User.builder()
                .username(u.getUsername())
                .password(u.getPasswordHash())
                .roles(u.getRol().name())
                .disabled(!u.isActivo())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        if (limite.bloqueado(request.username())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos fallidos. Espera " + limite.minutosDeBloqueo()
                            + " minutos antes de volver a intentar.");
        }
        Usuario usuario = usuarioRepository.findByUsername(request.username()).orElse(null);
        if (usuario == null || !passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            limite.registrarFallo(request.username());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, CREDENCIALES_INVALIDAS);
        }
        if (!usuario.isActivo()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Tu cuenta está desactivada. Comunícate con el administrador.");
        }
        limite.reiniciar(request.username());
        return respuesta(usuario);
    }

    public AuthResponse register(RegisterRequest request) {
        if (usuarioRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El username ya existe");
        }

        Usuario nuevo = new Usuario(
                request.username(),
                passwordEncoder.encode(request.password()),
                request.nombre(),
                Rol.USER
        );

        usuarioRepository.save(nuevo);
        return respuesta(nuevo);
    }

    /** Emite un token nuevo con los datos actuales (el rol puede haber cambiado). */
    public AuthResponse refresh(String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La sesión ya no es válida."));
        if (!usuario.isActivo()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tu cuenta está desactivada.");
        }
        return respuesta(usuario);
    }

    private AuthResponse respuesta(Usuario usuario) {
        String token = jwtService.generarToken(usuario.getUsername(), usuario.getRol().name());
        return new AuthResponse(token, usuario.getUsername(), usuario.getRol().name(), usuario.getNombre(),
                jwtService.getExpirationMs());
    }
}
