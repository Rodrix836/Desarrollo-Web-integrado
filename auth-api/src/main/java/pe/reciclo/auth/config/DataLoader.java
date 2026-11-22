package pe.reciclo.auth.config;

import pe.reciclo.auth.model.Rol;
import pe.reciclo.auth.model.Usuario;
import pe.reciclo.auth.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;

    public DataLoader(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JdbcTemplate jdbc) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        // El rol EMPRESA se eliminó: las cuentas de bases anteriores pasan a USER.
        int migradas = jdbc.update("UPDATE usuarios SET rol = 'USER' WHERE rol NOT IN ('ADMIN', 'USER')");
        if (migradas > 0) {
            System.out.println(">>> Cuentas con roles antiguos migradas a USER: " + migradas);
        }
        if (usuarioRepository.count() == 0) {
            usuarioRepository.save(new Usuario("admin", passwordEncoder.encode("admin123"),
                    "Administrador ReCiclo", Rol.ADMIN));
            usuarioRepository.save(new Usuario("user", passwordEncoder.encode("user123"),
                    "Usuario Normal", Rol.USER));
            System.out.println(">>> Usuarios semilla creados: admin / user");
        }
    }
}
