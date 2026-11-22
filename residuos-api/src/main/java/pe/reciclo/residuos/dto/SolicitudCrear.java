package pe.reciclo.residuos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import pe.reciclo.residuos.model.TipoResiduo;
import java.math.BigDecimal;

public record SolicitudCrear(
        @NotNull(message = "Indica el residuo que quieres solicitar.")
        @Positive(message = "El id del residuo debe ser positivo.") Long residuoId,
        TipoResiduo ofrece,
        @Positive(message = "La cantidad ofrecida debe ser mayor que cero.") BigDecimal cantidadOfrecida,
        @Size(max = 500, message = "El mensaje admite hasta 500 caracteres.") String mensaje
) {
    public SolicitudCrear {
        mensaje = mensaje == null ? "" : mensaje.trim();
    }
}
