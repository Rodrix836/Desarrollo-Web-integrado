package pe.reciclo.residuos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import pe.reciclo.residuos.dto.SolicitudRespuesta;
import pe.reciclo.residuos.security.Actor;

import java.math.BigDecimal;
import java.time.Instant;

/** Pedido de un USER para intercambiar un material publicado; el ADMIN lo acepta o rechaza. */
@Entity
@Table(indexes = {
        @Index(name = "idx_solicitud_residuo", columnList = "residuoId"),
        @Index(name = "idx_solicitud_solicitante", columnList = "solicitante")
})
public class SolicitudIntercambio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false)
    private Long residuoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoResiduo residuoTipo;

    /** Empresa que publicó el material, copiada al crear la solicitud. */
    @Column(length = 100)
    private String empresaDestino;

    @Column(length = 50, nullable = false)
    private String solicitante;

    @Enumerated(EnumType.STRING)
    private TipoResiduo ofrece;

    @Column(columnDefinition = "DECFLOAT")
    private BigDecimal cantidadOfrecida;

    @Column(length = 500)
    private String mensaje;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSolicitud estado;

    @Column(length = 300)
    private String respuesta;

    @Column(length = 50)
    private String respondidoPor;

    private Instant creadoEn;

    private Instant actualizadoEn;

    protected SolicitudIntercambio() {
    }

    public SolicitudIntercambio(RegistroResiduo residuo, Actor solicitante, TipoResiduo ofrece,
                                BigDecimal cantidadOfrecida, String mensaje) {
        this.residuoId = residuo.getId();
        this.residuoTipo = residuo.getTipoResiduo();
        this.empresaDestino = residuo.getEmpresa();
        this.solicitante = solicitante.usuario();
        this.ofrece = ofrece;
        this.cantidadOfrecida = cantidadOfrecida;
        this.mensaje = mensaje;
        this.estado = EstadoSolicitud.PENDIENTE;
        this.respuesta = "";
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

    public boolean esSolicitante(Actor actor) {
        return solicitante.equals(actor.usuario());
    }

    public void cambiarEstado(EstadoSolicitud nuevo, String respuesta, String usuario) {
        this.estado = nuevo;
        this.respuesta = respuesta == null ? "" : respuesta;
        this.respondidoPor = usuario;
    }

    public Long getId() { return id; }

    public Long getResiduoId() { return residuoId; }

    public EstadoSolicitud getEstado() { return estado; }

    public SolicitudRespuesta aRespuesta() {
        return new SolicitudRespuesta(id, residuoId, residuoTipo, empresaDestino, solicitante,
                ofrece, cantidadOfrecida, mensaje, estado, respuesta, respondidoPor, creadoEn, actualizadoEn);
    }
}
