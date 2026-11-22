package pe.reciclo.residuos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.reciclo.residuos.model.EstadoSolicitud;

public record SolicitudDecision(
        @NotNull(message = "Indica si aceptas o rechazas la solicitud.") EstadoSolicitud estado,
        @Size(max = 300, message = "La respuesta admite hasta 300 caracteres.") String respuesta
) {
    public SolicitudDecision {
        respuesta = respuesta == null ? "" : respuesta.trim();
    }
}
