package pe.reciclo.auth.config;

import pe.reciclo.auth.model.Rol;
import pe.reciclo.auth.model.Usuario;
import pe.reciclo.auth.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() == 0) {
            usuarioRepository.save(new Usuario(
                    "admin",
                    passwordEncoder.encode("admin123"),
                    "Administrador ReCiclo",
                    Rol.ADMIN,
                    null
            ));
            usuarioRepository.save(new Usuario(
                    "empresa",
                    passwordEncoder.encode("empresa123"),
                    "Empresa Demo",
                    Rol.EMPRESA,
                    "Empresa Demo"
            ));
            usuarioRepository.save(new Usuario(
                    "user",
                    passwordEncoder.encode("user123"),
                    "Usuario Normal",
                    Rol.USER,
                    null
            ));
            System.out.println(">>> Usuarios semilla creados: admin / empresa / user");
        }
    }
}