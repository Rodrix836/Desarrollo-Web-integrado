package pe.reciclo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import pe.reciclo.model.RegistroResiduoEntity;

/** Operaciones CRUD compartidas con el repositorio JPQL existente. */
@NoRepositoryBean
public interface RegistroResiduoJpaRepository extends JpaRepository<RegistroResiduoEntity, Long> {
}
