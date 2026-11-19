package pe.reciclo.residuos.dto;

import pe.reciclo.residuos.model.EstadoPublicacion;
import pe.reciclo.residuos.model.TipoResiduo;
import java.time.LocalDate;

public record AvisoIntercambio(
        Long id, TipoResiduo tipoResiduo, String empresa, LocalDate fechaPublicacion,
        EstadoPublicacion estadoPublicacion, String mensaje
) {
}
