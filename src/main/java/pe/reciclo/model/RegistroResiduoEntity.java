package pe.reciclo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Entidad persistente compartida por el CRUD y las consultas JPQL. */
@Entity
public class RegistroResiduoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    private TipoResiduo tipoResiduo;
    // H2 conserva los decimales sin imponer la escala 2 predeterminada de JPA.
    @Column(columnDefinition = "DECFLOAT")
    private BigDecimal cantidad;
    @Column(columnDefinition = "CHARACTER LARGE OBJECT")
    private String unidadMedida;
    private LocalDate fecha;
    @Column(length = 500)
    private String descripcion;
    @Column(length = 100)
    private String empresa;
    @Column(length = 300)
    private String intercambioPor;
    private Boolean disponible;

    protected RegistroResiduoEntity() {
    }

    public RegistroResiduoEntity(RegistroResiduo registro) {
        actualizarDesde(registro);
    }

    public void actualizarDesde(RegistroResiduo registro) {
        tipoResiduo = registro.tipoResiduo();
        cantidad = registro.cantidad();
        unidadMedida = registro.unidadMedida();
        fecha = registro.fecha();
        descripcion = registro.descripcion();
        empresa = registro.empresa();
        intercambioPor = registro.intercambioPor();
        disponible = registro.disponible();
    }

    public RegistroResiduo toRegistroResiduo() {
        return new RegistroResiduo(id, tipoResiduo, cantidad, unidadMedida, fecha,
                descripcion, empresa, intercambioPor, disponible);
    }
}
