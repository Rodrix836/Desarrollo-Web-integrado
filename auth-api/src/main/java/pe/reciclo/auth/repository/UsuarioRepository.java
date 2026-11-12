package pe.reciclo.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.reciclo.auth.model.Usuario;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);
    boolean existsByUsername(String username);
}