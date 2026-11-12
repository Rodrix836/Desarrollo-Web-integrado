package pe.reciclo.consultas;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.Map;

@RestControllerAdvice
public class ConsultaExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> estado(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("mensaje", ex.getReason()));
    }

    @ExceptionHandler(RestClientResponseException.class)
    ResponseEntity<?> upstream(RestClientResponseException ex) {
        int status = ex.getStatusCode().value();
        if (status == 401 || status == 403) {
            return ResponseEntity.status(status).body(Map.of("mensaje", "Tu sesión no permite esta consulta."));
        }
        return noDisponible();
    }

    @ExceptionHandler(RestClientException.class)
    ResponseEntity<?> noDisponible() {
        return ResponseEntity.status(502).body(Map.of("mensaje", "No se pudo consultar la API de Residuos. Inténtalo nuevamente."));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<?> parametro() {
        return ResponseEntity.badRequest().body(Map.of("mensaje", "Revisa los filtros de la consulta."));
    }
}
