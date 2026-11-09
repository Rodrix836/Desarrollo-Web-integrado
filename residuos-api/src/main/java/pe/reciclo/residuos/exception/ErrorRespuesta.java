package pe.reciclo.residuos.exception;

import org.springframework.http.HttpStatusCode;

import java.time.Instant;
import java.util.Map;

public record ErrorRespuesta(String mensaje, Map<String, String> errores, Instant timestamp, int status) {

    public static ErrorRespuesta de(HttpStatusCode estado, String mensaje, Map<String, String> errores) {
        return new ErrorRespuesta(mensaje, errores, Instant.now(), estado.value());
    }
}
