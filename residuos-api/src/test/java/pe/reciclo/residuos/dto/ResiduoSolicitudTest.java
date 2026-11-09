package pe.reciclo.residuos.dto;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import pe.reciclo.residuos.model.TipoResiduo;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ResiduoSolicitudTest {

    @Test
    void admiteCamposOpcionalesAusentesYAplicaLosValoresPredeterminados() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var solicitud = solicitud(null);
            assertThat(factory.getValidator().validate(solicitud)).isEmpty();
            assertThat(solicitud.empresa()).isEmpty();
            assertThat(solicitud.intercambioPor()).isEmpty();
            assertThat(solicitud.disponible()).isFalse();
        }
    }

    @Test
    void exigeEmpresaSiElResiduoEstaDisponible() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(solicitud(true)))
                    .singleElement().satisfies(error -> {
                        assertThat(error.getPropertyPath().toString()).isEqualTo("empresaValida");
                        assertThat(error.getMessage()).contains("empresa propietaria");
                    });
        }
    }

    private static ResiduoSolicitud solicitud(Boolean disponible) {
        return new ResiduoSolicitud(TipoResiduo.VIDRIO, BigDecimal.ONE, "kg",
                LocalDate.of(2026, 9, 9), null, " ", null, disponible);
    }
}
