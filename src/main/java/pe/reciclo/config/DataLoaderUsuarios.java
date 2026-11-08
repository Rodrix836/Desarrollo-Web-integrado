package pe.reciclo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pe.reciclo.model.Rol;
import pe.reciclo.model.Usuario;
import pe.reciclo.repository.UsuarioRepository;

/**
 * Carga usuarios de demostración si la tabla está vacía (Integrante 1).
 */
@Component
public class DataLoaderUsuarios implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoaderUsuarios.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataLoaderUsuarios(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            log.info("Usuarios ya existen en BD ({}). No se recargan demos.", usuarioRepository.count());
            return;
        }

        log.info("Cargando usuarios de demostración (Integrante 1)...");

        usuarioRepository.save(new Usuario(
                "admin", "admin@reciclo.pe",
                passwordEncoder.encode("admin123"),
                "Administrador ReCiclo", Rol.ADMIN, null));

        usuarioRepository.save(new Usuario(
                "empresa", "empresa@reciclo.pe",
                passwordEncoder.encode("empresa123"),
                "Empresa Demo", Rol.EMPRESA, "Empresa Demo"));

        usuarioRepository.save(new Usuario(
                "user", "user@reciclo.pe",
                passwordEncoder.encode("user123"),
                "Usuario Consulta", Rol.USER, null));

        log.info("Usuarios demo: admin/admin123, empresa/empresa123, user/user123");
    }
}
