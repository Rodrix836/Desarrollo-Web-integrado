package pe.reciclo.residuos.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.reciclo.residuos.dto.AvisoIntercambio;
import pe.reciclo.residuos.dto.ResiduoRespuesta;
import pe.reciclo.residuos.dto.ResiduoSolicitud;
import pe.reciclo.residuos.exception.DatosIntercambioInvalidosException;
import pe.reciclo.residuos.exception.RegistroNoEncontradoException;
import pe.reciclo.residuos.model.EstadoPublicacion;
import pe.reciclo.residuos.model.RegistroResiduo;
import pe.reciclo.residuos.model.TipoResiduo;
import pe.reciclo.residuos.repository.RegistroResiduoRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

@Service
@Transactional(readOnly = true)
public class RegistroResiduoService {

    private final RegistroResiduoRepository repository;
    private final Clock clock;

    public RegistroResiduoService(RegistroResiduoRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public List<ResiduoRespuesta> listar(boolean administrador, EstadoPublicacion estado, TipoResiduo preferencia) {
        LocalDate hoy = LocalDate.now(clock);
        return repository.findAll(Sort.by("id")).stream().map(r -> r.aRespuesta(hoy))
                .filter(r -> administrador || r.estadoPublicacion() == EstadoPublicacion.DISPONIBLE)
                .filter(r -> estado == null || r.estadoPublicacion() == estado)
                .filter(r -> preferencia == null || r.intercambioPor().equals(preferencia.name()))
                .toList();
    }

    public ResiduoRespuesta consultar(long id, boolean administrador) {
        var respuesta = buscar(id).aRespuesta(LocalDate.now(clock));
        if (!administrador && respuesta.estadoPublicacion() != EstadoPublicacion.DISPONIBLE) {
            throw new RegistroNoEncontradoException(id);
        }
        return respuesta;
    }

    public List<AvisoIntercambio> avisos() {
        LocalDate hoy = LocalDate.now(clock);
        return repository.findAll().stream().map(r -> r.aRespuesta(hoy))
                .filter(r -> r.disponible() && r.fechaPublicacion() != null && !r.fechaPublicacion().isBefore(hoy))
                .sorted(Comparator.comparing(ResiduoRespuesta::fechaPublicacion).thenComparing(ResiduoRespuesta::id))
                .map(r -> new AvisoIntercambio(r.id(), r.tipoResiduo(), r.empresa(), r.fechaPublicacion(),
                        r.estadoPublicacion(), mensajeAviso(r, hoy)))
                .toList();
    }

    private String mensajeAviso(ResiduoRespuesta r, LocalDate hoy) {
        String material = r.tipoResiduo().etiqueta() + " de " + r.empresa();
        if (r.fechaPublicacion().equals(hoy)) {
            return material + " está disponible para intercambio desde hoy.";
        }
        return material + " estará disponible para intercambio el "
                + r.fechaPublicacion().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".";
    }

    @Transactional
    public ResiduoRespuesta crear(ResiduoSolicitud solicitud) {
        var datos = preparar(solicitud, null);
        return repository.save(new RegistroResiduo(datos)).aRespuesta(LocalDate.now(clock));
    }

    @Transactional
    public ResiduoRespuesta actualizar(long id, ResiduoSolicitud solicitud) {
        var existente = buscar(id);
        existente.actualizarDesde(preparar(solicitud, existente.aRespuesta(LocalDate.now(clock))));
        return existente.aRespuesta(LocalDate.now(clock));
    }

    @Transactional
    public void eliminar(long id) {
        repository.delete(buscar(id));
    }

    private ResiduoSolicitud preparar(ResiduoSolicitud s, ResiduoRespuesta anterior) {
        LocalDate hoy = LocalDate.now(clock);
        Map<String, String> errores = new TreeMap<>();
        if (!"kg".equals(s.unidadMedida()) && (anterior == null || !Objects.equals(s.unidadMedida(), anterior.unidadMedida()))) {
            errores.put("unidadMedida", "Los materiales nuevos se registran en kilos (kg). Conserva la unidad de los registros históricos.");
        }
        if (s.disponible()) {
            if (s.empresa().isBlank()) {
                errores.put("empresa", "Indica la empresa propietaria para habilitar el intercambio.");
            }
            boolean catalogada = Arrays.stream(TipoResiduo.values()).anyMatch(t -> t.name().equals(s.intercambioPor()));
            boolean historicaSinCambios = anterior != null && s.intercambioPor().equals(anterior.intercambioPor());
            if (s.intercambioPor().isBlank() || (!catalogada && !historicaSinCambios)) {
                errores.put("intercambioPor", "Selecciona un material válido como preferencia de intercambio.");
            }
            if (s.fechaPublicacion() == null) {
                errores.put("fechaPublicacion", "Selecciona la fecha de publicación para el intercambio.");
            } else if (s.fechaPublicacion().isBefore(hoy)
                    && (anterior == null || !anterior.disponible()
                        || !s.fechaPublicacion().equals(anterior.fechaPublicacion()))) {
                errores.put("fechaPublicacion", "La nueva fecha de publicación debe ser hoy o una fecha futura.");
            }
        }
        if (!errores.isEmpty()) {
            throw new DatosIntercambioInvalidosException(errores);
        }
        String empresa = s.empresa();
        if (!s.disponible()) {
            empresa = anterior == null ? "" : anterior.empresa();
        }
        LocalDate fecha = s.fecha();
        if (fecha == null) {
            fecha = anterior == null ? hoy : anterior.fecha();
        }
        return new ResiduoSolicitud(s.tipoResiduo(), s.cantidad(), s.unidadMedida(), fecha, s.descripcion(),
                empresa, s.disponible() ? s.intercambioPor() : "", s.disponible(),
                s.disponible() ? s.fechaPublicacion() : null);
    }

    private RegistroResiduo buscar(long id) {
        return repository.findById(id).orElseThrow(() -> new RegistroNoEncontradoException(id));
    }
}
