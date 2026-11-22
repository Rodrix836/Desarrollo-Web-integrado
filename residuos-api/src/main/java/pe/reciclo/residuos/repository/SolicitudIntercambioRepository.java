package pe.reciclo.residuos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.reciclo.residuos.model.EstadoSolicitud;
import pe.reciclo.residuos.model.SolicitudIntercambio;

import java.util.List;

public interface SolicitudIntercambioRepository extends JpaRepository<SolicitudIntercambio, Long> {

    List<SolicitudIntercambio> findBySolicitanteOrderByIdDesc(String solicitante);

    List<SolicitudIntercambio> findByResiduoIdAndEstado(Long residuoId, EstadoSolicitud estado);

    boolean existsByResiduoIdAndSolicitanteAndEstado(Long residuoId, String solicitante, EstadoSolicitud estado);

    long countByEstado(EstadoSolicitud estado);

    List<SolicitudIntercambio> findAllByOrderByIdDesc();
}
