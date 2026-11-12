package pe.reciclo.auth.controller;

import jakarta.validation.Valid;
import pe.reciclo.auth.dto.AuthResponse;
import pe.reciclo.auth.dto.LoginRequest;
import pe.reciclo.auth.dto.RegisterRequest;
import pe.reciclo.auth.dto.UsuarioResponse;
import pe.reciclo.auth.service.AuthService;
import pe.reciclo.auth.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UsuarioService usuarioService;

    public AuthController(AuthService authService, UsuarioService usuarioService) {
        this.authService = authService;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(usuarioService.obtenerPorUsername(authentication.getName()));
    }
}