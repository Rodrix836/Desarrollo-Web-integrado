package pe.reciclo.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.reciclo.dto.AuthResponse;
import pe.reciclo.dto.RegisterRequest;
import pe.reciclo.service.AuthService;

/**
 * API de autenticación (Integrante 1).
 * Compatible con el login anterior (campos usuario/clave) y con username/password.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Login API.
     * Acepta JSON: { "usuario": "...", "clave": "..." }
     * o también: { "username": "...", "password": "..." }
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req) {
        String user = req.usuario() != null ? req.usuario() : req.username();
        String pass = req.clave() != null ? req.clave() : req.password();
        if (user == null || pass == null || user.isBlank() || pass.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(authService.loginApi(user, pass));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(Authentication authentication) {
        return ResponseEntity.ok(authService.me(authentication.getName()));
    }
}
