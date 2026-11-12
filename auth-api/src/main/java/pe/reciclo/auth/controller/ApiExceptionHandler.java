package pe.reciclo.auth.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import java.util.TreeMap;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> estado(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(
                Map.of("mensaje", ex.getReason() == null ? "No se pudo procesar la solicitud." : ex.getReason(),
                        "status", ex.getStatusCode().value()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validacion(MethodArgumentNotValidException ex) {
        var errores = new TreeMap<String, String>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errores.putIfAbsent(error.getField(), "Revisa este campo y su longitud."));
        return ResponseEntity.badRequest().body(Map.of("mensaje", "Revisa los datos ingresados.", "errores", errores));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<?> json() {
        return ResponseEntity.badRequest().body(Map.of("mensaje", "El cuerpo JSON no es válido."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> duplicado() {
        return ResponseEntity.status(409).body(Map.of("mensaje", "El nombre de usuario ya está registrado."));
    }
}
