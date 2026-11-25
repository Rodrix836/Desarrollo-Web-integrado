package pe.reciclo.residuos.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.reciclo.residuos.dto.AvisoIntercambio;
import pe.reciclo.residuos.dto.Estadisticas;
import pe.reciclo.residuos.dto.Pagina;
import pe.reciclo.residuos.dto.ResiduoRespuesta;
import pe.reciclo.residuos.dto.ResiduoSolicitud;
import pe.reciclo.residuos.exception.DatosIntercambioInvalidosException;
import pe.reciclo.residuos.exception.RegistroNoEncontradoException;
import pe.reciclo.residuos.model.EstadoPublicacion;
import pe.reciclo.residuos.model.EstadoSolicitud;
import pe.reciclo.residuos.model.RegistroResiduo;
import pe.reciclo.residuos.model.TipoResiduo;
import pe.reciclo.residuos.repository.RegistroResiduoRepository;
import pe.reciclo.residuos.repository.SolicitudIntercambioRepository;
import pe.reciclo.residuos.security.Actor;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;

@Service
@Transactional(readOnly = true)
public class RegistroResiduoService {

    private final RegistroResiduoRepository repository;
    private final SolicitudIntercambioRepository solicitudes;
    private final Clock clock;

    @Autowired
    public RegistroResiduoService(RegistroResiduoRepository repository,
                                  SolicitudIntercambioRepository solicitudes, Clock clock) {
        this.repository = repository;
        this.solicitudes = solicitudes;
        this.clock = clock;
    }

    /** Constructor para pruebas unitarias sin solicitudes de intercambio. */
    public RegistroResiduoService(RegistroResiduoRepository repository, Clock clock) {
        this(repository, null, clock);
    }

    public List<ResiduoRespuesta> listar(boolean administrador, EstadoPublicacion estado, TipoResiduo preferencia) {
        return listar(administrador ? Actor.ADMIN_SISTEMA : Actor.USUARIO_ANONIMO, estado, preferencia);
    }

    /** ADMIN ve todo; USER sólo el catálogo disponible. */
    public List<ResiduoRespuesta> listar(Actor actor, EstadoPublicacion estado, TipoResiduo preferencia) {
        LocalDate hoy = LocalDate.now(clock);
        return repository.findAll(Sort.by("id")).stream()
                .filter(r -> visible(r, actor, hoy))
                .map(r -> r.aRespuesta(hoy))
                .filter(r -> estado == null || r.estadoPublicacion() == estado)
                .filter(r -> preferencia == null || r.intercambioPor().equals(preferencia.name()))
                .toList();
    }

    public Pagina<ResiduoRespuesta> pagina(Actor actor, EstadoPublicacion estado, TipoResiduo preferencia,
                                           int pagina, int tamanio) {
        int tam = Math.max(1, Math.min(tamanio, 100));
        return Pagina.de(listar(actor, estado, preferencia), Math.max(0, pagina), tam);
    }

    public ResiduoRespuesta consultar(long id, boolean administrador) {
        return consultar(id, administrador ? Actor.ADMIN_SISTEMA : Actor.USUARIO_ANONIMO);
    }

    public ResiduoRespuesta consultar(long id, Actor actor) {
        var registro = buscar(id);
        LocalDate hoy = LocalDate.now(clock);
        if (!visible(registro, actor, hoy)) {
            throw new RegistroNoEncontradoException(id);
        }
        return registro.aRespuesta(hoy);
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

    /** Resumen en kilos de los registros que el usuario puede ver. */
    public Estadisticas estadisticas(Actor actor) {
        var visibles = listar(actor, null, null);
        var enKg = visibles.stream().filter(r -> "kg".equals(r.unidadMedida())).toList();
        BigDecimal total = enKg.stream().map(ResiduoRespuesta::cantidad).reduce(BigDecimal.ZERO, BigDecimal::add);
        long disponibles = visibles.stream()
                .filter(r -> r.estadoPublicacion() == EstadoPublicacion.DISPONIBLE).count();
        var porTipo = agrupar(enKg, r -> r.tipoResiduo().name(), r -> r.tipoResiduo().etiqueta());
        var porEmpresa = agrupar(enKg.stream().filter(r -> !r.empresa().isBlank()).toList(),
                r -> r.empresa().trim().toLowerCase(Locale.ROOT), r -> r.empresa().trim()).stream()
                .sorted(Comparator.comparing(Estadisticas.Grupo::kg).reversed())
                .limit(10).toList();
        var porMes = agrupar(enKg, r -> r.fecha().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                r -> r.fecha().format(DateTimeFormatter.ofPattern("MM/yyyy"))).stream()
                .sorted(Comparator.comparing(Estadisticas.Grupo::clave)).toList();
        long pendientes = solicitudes == null || !actor.admin()
                ? 0 : solicitudes.countByEstado(EstadoSolicitud.PENDIENTE);
        return new Estadisticas(visibles.size(), disponibles, total, porTipo, porEmpresa, porMes, pendientes);
    }

    private List<Estadisticas.Grupo> agrupar(List<ResiduoRespuesta> registros,
                                            Function<ResiduoRespuesta, String> clave,
                                            Function<ResiduoRespuesta, String> etiqueta) {
        Map<String, Estadisticas.Grupo> grupos = new LinkedHashMap<>();
        for (var r : registros) {
            String k = clave.apply(r);
            grupos.merge(k, new Estadisticas.Grupo(k, etiqueta.apply(r), 1, r.cantidad()),
                    (a, b) -> new Estadisticas.Grupo(a.clave(), a.etiqueta(), a.registros() + 1, a.kg().add(b.kg())));
        }
        return List.copyOf(grupos.values());
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
    public ResiduoRespuesta crear(ResiduoSolicitud solicitud, Actor actor) {
        var nuevo = new RegistroResiduo(preparar(solicitud, null));
        nuevo.registrarAutor(actor);
        return repository.save(nuevo).aRespuesta(LocalDate.now(clock));
    }

    @Transactional
    public ResiduoRespuesta actualizar(long id, ResiduoSolicitud solicitud) {
        var existente = buscar(id);
        existente.actualizarDesde(preparar(solicitud, existente.aRespuesta(LocalDate.now(clock))));
        return existente.aRespuesta(LocalDate.now(clock));
    }

    @Transactional
    public ResiduoRespuesta actualizar(long id, ResiduoSolicitud solicitud, Actor actor) {
        var existente = buscar(id);
        existente.actualizarDesde(preparar(solicitud, existente.aRespuesta(LocalDate.now(clock))));
        existente.registrarCambio(actor);
        return existente.aRespuesta(LocalDate.now(clock));
    }

    @Transactional
    public void eliminar(long id) {
        repository.delete(buscar(id));
    }

    @Transactional
    public void eliminar(long id, Actor actor) {
        var existente = buscar(id);
        if (solicitudes != null) {
            solicitudes.findByResiduoIdAndEstado(id, EstadoSolicitud.PENDIENTE).forEach(s ->
                    s.cambiarEstado(EstadoSolicitud.CANCELADA, "El material fue retirado del inventario.",
                            actor.usuario()));
        }
        repository.delete(existente);
    }

    /** ADMIN ve todo el inventario; USER sólo el catálogo disponible. */
    private boolean visible(RegistroResiduo registro, Actor actor, LocalDate hoy) {
        return actor.admin() || registro.estado(hoy) == EstadoPublicacion.DISPONIBLE;
    }

    private ResiduoSolicitud preparar(ResiduoSolicitud s, ResiduoRespuesta anterior) {
        LocalDate hoy = LocalDate.now(clock);
        Map<String, String> errores = new TreeMap<>();
        if (s.tipoResiduo() != null && !s.tipoResiduo().vigente()) {
            errores.put("tipoResiduo", "Selecciona un tipo de residuo vigente: plástico, papel y cartón, vidrio, metal u orgánico.");
        }
        if (!"kg".equals(s.unidadMedida()) && (anterior == null || !Objects.equals(s.unidadMedida(), anterior.unidadMedida()))) {
            errores.put("unidadMedida", "Los materiales nuevos se registran en kilos (kg). Conserva la unidad de los registros históricos.");
        }
        if (s.disponible()) {
            if (s.empresa().isBlank()) {
                errores.put("empresa", "Indica la empresa propietaria para habilitar el intercambio.");
            }
            boolean catalogada = Arrays.stream(TipoResiduo.values()).filter(TipoResiduo::vigente)
                    .anyMatch(t -> t.name().equals(s.intercambioPor()));
            boolean historicaSinCambios = anterior != null
                    && !TipoResiduo.OTROS.name().equals(s.intercambioPor())
                    && s.intercambioPor().equals(anterior.intercambioPor());
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
