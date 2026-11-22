package pe.reciclo.residuos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;
import pe.reciclo.residuos.dto.ResiduoRespuesta;
import pe.reciclo.residuos.dto.ResiduoSolicitud;
import pe.reciclo.residuos.security.Actor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
public class RegistroResiduo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoResiduo tipoResiduo;

    // DECFLOAT en H2 evita el redondeo a dos decimales de NUMERIC con la escala predeterminada.
    @Column(nullable = false, columnDefinition = "DECFLOAT")
    private BigDecimal cantidad;

    @Column(nullable = false, columnDefinition = "CHARACTER LARGE OBJECT")
    private String unidadMedida;

    @Column(nullable = false)
    private LocalDate fecha;

    private LocalDate fechaPublicacion;

    @Column(length = 500)
    private String descripcion;

    @Column(length = 100, nullable = false)
    private String empresa;

    @Column(length = 300, nullable = false)
    private String intercambioPor;

    @Column(nullable = false)
    private Boolean disponible;

    /** Usuario (sub del JWT) que registró el material; null en registros anteriores. */
    @Column(length = 50)
    private String creadoPor;

    private Instant creadoEn;

    @Column(length = 50)
    private String actualizadoPor;

    private Instant actualizadoEn;

    protected RegistroResiduo() {
    }

    public RegistroResiduo(ResiduoSolicitud solicitud) {
        actualizarDesde(solicitud);
    }

    public void actualizarDesde(ResiduoSolicitud solicitud) {
        tipoResiduo = solicitud.tipoResiduo();
        cantidad = solicitud.cantidad();
        unidadMedida = solicitud.unidadMedida();
        fecha = solicitud.fecha();
        fechaPublicacion = solicitud.fechaPublicacion();
        descripcion = solicitud.descripcion();
        empresa = solicitud.empresa();
        intercambioPor = solicitud.intercambioPor();
        disponible = solicitud.disponible();
    }

    @PrePersist
    void alCrear() {
        creadoEn = Instant.now();
        actualizadoEn = creadoEn;
    }

    @PreUpdate
    void alActualizar() {
        actualizadoEn = Instant.now();
    }

    public void registrarAutor(Actor actor) {
        creadoPor = actor.usuario();
        actualizadoPor = actor.usuario();
    }

    public void registrarCambio(Actor actor) {
        actualizadoPor = actor.usuario();
    }

    /** Al aceptar un intercambio el material deja de mostrarse en el catálogo. */
    public void retirarDelCatalogo() {
        disponible = false;
    }

    public Long getId() { return id; }

    public TipoResiduo getTipoResiduo() { return tipoResiduo; }

    public String getEmpresa() { return empresa; }

    public EstadoPublicacion estado(LocalDate hoy) {
        if (!Boolean.TRUE.equals(disponible)) return EstadoPublicacion.NO_PUBLICADO;
        if (fechaPublicacion != null && fechaPublicacion.isAfter(hoy)) return EstadoPublicacion.PROGRAMADO;
        // Los registros publicados antes de la programación mantienen su visibilidad.
        return EstadoPublicacion.DISPONIBLE;
    }

    public ResiduoRespuesta aRespuesta(LocalDate hoy) {
        return new ResiduoRespuesta(id, tipoResiduo, cantidad, unidadMedida, fecha,
                descripcion, empresa, intercambioPor, disponible, fechaPublicacion, estado(hoy),
                creadoPor, creadoEn, actualizadoPor, actualizadoEn);
    }
}
