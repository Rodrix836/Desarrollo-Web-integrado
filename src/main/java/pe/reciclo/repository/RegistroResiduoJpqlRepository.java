package pe.reciclo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.reciclo.model.RegistroResiduoEntity;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface RegistroResiduoJpqlRepository extends JpaRepository<RegistroResiduoEntity, Long> {

    @Query("""
            SELECT r FROM RegistroResiduoEntity r
            WHERE LOWER(r.empresa) LIKE LOWER(CONCAT('%', :empresa, '%'))
            """)
    List<RegistroResiduoEntity> buscarPorEmpresa(@Param("empresa") String empresa);

    @Query("""
            SELECT r FROM RegistroResiduoEntity r
            WHERE r.cantidad > :cantidad
            """)
    List<RegistroResiduoEntity> buscarCantidadMayorQue(@Param("cantidad") BigDecimal cantidad);

    @Query("""
            SELECT r FROM RegistroResiduoEntity r
            ORDER BY r.cantidad DESC
            """)
    List<RegistroResiduoEntity> listarOrdenadosPorCantidad();
}
