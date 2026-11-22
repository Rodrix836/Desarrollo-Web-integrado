package pe.reciclo.residuos.dto;

import pe.reciclo.residuos.model.EstadoSolicitud;
import pe.reciclo.residuos.model.TipoResiduo;
import java.math.BigDecimal;
import java.time.Instant;

public record SolicitudRespuesta(
        Long id, Long residuoId, TipoResiduo residuoTipo, String empresaDestino, String solicitante,
        TipoResiduo ofrece, BigDecimal cantidadOfrecida, String mensaje, EstadoSolicitud estado,
        String respuesta, String respondidoPor, Instant creadoEn, Instant actualizadoEn
) {
}
