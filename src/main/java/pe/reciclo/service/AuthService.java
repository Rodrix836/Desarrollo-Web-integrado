package pe.reciclo.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pe.reciclo.model.Rol;
import pe.reciclo.model.UsuarioSesion;

import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final Map<String, CuentaDemo> cuentas;

    public AuthService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        this.cuentas = Map.of(
                "admin", new CuentaDemo(
                        passwordEncoder.encode("admin123"),
                        "Administrador ReCiclo",
                        Rol.ADMIN,
                        ""
                ),
                "empresa", new CuentaDemo(
                        passwordEncoder.encode("empresa123"),
                        "Empresa Demo",
                        Rol.EMPRESA,
                        "Empresa Demo"
                )
        );
    }

    public Optional<UsuarioSesion> autenticar(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }

        String usuario = username.trim().toLowerCase();
        CuentaDemo cuenta = cuentas.get(usuario);

        if (cuenta == null || !passwordEncoder.matches(password, cuenta.passwordHash())) {
            return Optional.empty();
        }

        return Optional.of(new UsuarioSesion(
                usuario,
                cuenta.nombre(),
                cuenta.rol(),
                cuenta.empresa()
        ));
    }

    private record CuentaDemo(
            String passwordHash,
            String nombre,
            Rol rol,
            String empresa
    ) {}
}
