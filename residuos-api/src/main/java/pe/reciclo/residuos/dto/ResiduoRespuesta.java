package pe.reciclo.residuos.dto;

import pe.reciclo.residuos.model.TipoResiduo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ResiduoRespuesta(
        Long id, TipoResiduo tipoResiduo, BigDecimal cantidad, String unidadMedida,
        LocalDate fecha, String descripcion, String empresa, String intercambioPor, Boolean disponible
) {
}
