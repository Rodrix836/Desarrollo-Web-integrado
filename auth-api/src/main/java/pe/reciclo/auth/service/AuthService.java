package pe.reciclo.auth.service;

import pe.reciclo.auth.dto.AuthResponse;
import pe.reciclo.auth.dto.LoginRequest;
import pe.reciclo.auth.dto.RegisterRequest;
import pe.reciclo.auth.model.Rol;
import pe.reciclo.auth.model.Usuario;
import pe.reciclo.auth.repository.UsuarioRepository;
import pe.reciclo.auth.security.JwtService;
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

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario u = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        return User.builder()
                .username(u.getUsername())
                .password(u.getPasswordHash())
                .roles(u.getRol().name())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByUsername(request.username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));

        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }

        String token = jwtService.generarToken(usuario.getUsername(), usuario.getRol().name());
        return new AuthResponse(token, usuario.getUsername(), usuario.getRol().name(), usuario.getNombre());
    }

    public AuthResponse register(RegisterRequest request) {
        if (usuarioRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El username ya existe");
        }

        Usuario nuevo = new Usuario(
                request.username(),
                passwordEncoder.encode(request.password()),
                request.nombre(),
                Rol.USER,
                null
        );

        usuarioRepository.save(nuevo);

        String token = jwtService.generarToken(nuevo.getUsername(), nuevo.getRol().name());
        return new AuthResponse(token, nuevo.getUsername(), nuevo.getRol().name(), nuevo.getNombre());
    }
}