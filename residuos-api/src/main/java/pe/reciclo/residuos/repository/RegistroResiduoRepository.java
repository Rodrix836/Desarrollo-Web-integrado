package pe.reciclo.residuos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.reciclo.residuos.model.RegistroResiduo;

public interface RegistroResiduoRepository extends JpaRepository<RegistroResiduo, Long> {
}
