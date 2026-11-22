package pe.reciclo.residuos.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.reciclo.residuos.dto.SolicitudCrear;
import pe.reciclo.residuos.dto.SolicitudDecision;
import pe.reciclo.residuos.dto.SolicitudRespuesta;
import pe.reciclo.residuos.exception.ConflictoException;
import pe.reciclo.residuos.exception.RegistroNoEncontradoException;
import pe.reciclo.residuos.exception.SolicitudNoEncontradaException;
import pe.reciclo.residuos.model.EstadoPublicacion;
import pe.reciclo.residuos.model.EstadoSolicitud;
import pe.reciclo.residuos.model.SolicitudIntercambio;
import pe.reciclo.residuos.repository.RegistroResiduoRepository;
import pe.reciclo.residuos.repository.SolicitudIntercambioRepository;
import pe.reciclo.residuos.security.Actor;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * Flujo de intercambio: USER solicita un material disponible y ADMIN acepta o rechaza.
 * Los permisos por rol se aplican en SecurityConfig; aquí sólo las reglas del negocio.
 */
@Service
@Transactional(readOnly = true)
public class SolicitudIntercambioService {

    private final SolicitudIntercambioRepository repository;
    private final RegistroResiduoRepository residuos;
    private final Clock clock;

    public SolicitudIntercambioService(SolicitudIntercambioRepository repository,
                                       RegistroResiduoRepository residuos, Clock clock) {
        this.repository = repository;
        this.residuos = residuos;
        this.clock = clock;
    }

    @Transactional
    public SolicitudRespuesta crear(SolicitudCrear datos, Actor actor) {
        var residuo = residuos.findById(datos.residuoId())
                .orElseThrow(() -> new RegistroNoEncontradoException(datos.residuoId()));
        if (residuo.estado(LocalDate.now(clock)) != EstadoPublicacion.DISPONIBLE) {
            // Un material no publicado se comporta como inexistente para el USER.
            throw new RegistroNoEncontradoException(datos.residuoId());
        }
        if (repository.existsByResiduoIdAndSolicitanteAndEstado(residuo.getId(), actor.usuario(),
                EstadoSolicitud.PENDIENTE)) {
            throw new ConflictoException("Ya tienes una solicitud pendiente para este material.");
        }
        var solicitud = new SolicitudIntercambio(residuo, actor, datos.ofrece(), datos.cantidadOfrecida(),
                datos.mensaje());
        return repository.save(solicitud).aRespuesta();
    }

    public List<SolicitudRespuesta> mias(Actor actor) {
        return repository.findBySolicitanteOrderByIdDesc(actor.usuario()).stream()
                .map(SolicitudIntercambio::aRespuesta).toList();
    }

    public List<SolicitudRespuesta> todas() {
        return repository.findAllByOrderByIdDesc().stream().map(SolicitudIntercambio::aRespuesta).toList();
    }

    @Transactional
    public SolicitudRespuesta decidir(long id, SolicitudDecision decision, Actor actor) {
        var solicitud = buscar(id);
        if (decision.estado() != EstadoSolicitud.ACEPTADA && decision.estado() != EstadoSolicitud.RECHAZADA) {
            throw new ConflictoException("La respuesta debe ser ACEPTADA o RECHAZADA.");
        }
        verificarPendiente(solicitud);
        solicitud.cambiarEstado(decision.estado(), decision.respuesta(), actor.usuario());
        if (decision.estado() == EstadoSolicitud.ACEPTADA) {
            residuos.findById(solicitud.getResiduoId()).ifPresent(r -> {
                r.retirarDelCatalogo();
                r.registrarCambio(actor);
            });
            repository.findByResiduoIdAndEstado(solicitud.getResiduoId(), EstadoSolicitud.PENDIENTE).stream()
                    .filter(otra -> !otra.getId().equals(solicitud.getId()))
                    .forEach(otra -> otra.cambiarEstado(EstadoSolicitud.RECHAZADA,
                            "El material fue asignado a otra solicitud.", actor.usuario()));
        }
        return solicitud.aRespuesta();
    }

    @Transactional
    public SolicitudRespuesta cancelar(long id, Actor actor) {
        var solicitud = buscar(id);
        if (!solicitud.esSolicitante(actor)) {
            // No se revela si la solicitud de otra persona existe.
            throw new SolicitudNoEncontradaException(id);
        }
        verificarPendiente(solicitud);
        solicitud.cambiarEstado(EstadoSolicitud.CANCELADA, "Cancelada por el solicitante.", actor.usuario());
        return solicitud.aRespuesta();
    }

    private void verificarPendiente(SolicitudIntercambio solicitud) {
        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new ConflictoException("La solicitud ya fue "
                    + solicitud.getEstado().name().toLowerCase(Locale.ROOT) + ".");
        }
    }

    private SolicitudIntercambio buscar(long id) {
        return repository.findById(id).orElseThrow(() -> new SolicitudNoEncontradaException(id));
    }
}
