package pe.reciclo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import pe.reciclo.service.RegistroNoEncontradoException;

import java.util.Map;
import java.util.TreeMap;
import java.io.UncheckedIOException;

@RestControllerAdvice(assignableTypes = RegistroResiduoController.class)
public class ApiExceptionHandler {

    @ExceptionHandler(RegistroNoEncontradoException.class)
    public ResponseEntity<ErrorRespuesta> noEncontrado(RegistroNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorRespuesta(ex.getMessage(), Map.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new TreeMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errores.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ErrorRespuesta("Entrada inválida.", errores));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorRespuesta> jsonInvalido() {
        return ResponseEntity.badRequest().body(new ErrorRespuesta(
                "JSON inválido. Envía un objeto con cantidad numérica, fecha válida (yyyy-MM-dd) "
                        + "y tipoResiduo: PLASTICO, PAPEL_CARTON, VIDRIO, METAL, ORGANICO u OTROS.",
                Map.of()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorRespuesta> identificadorInvalido() {
        return ResponseEntity.badRequest().body(new ErrorRespuesta("Identificador inválido.",
                Map.of("id", "El id debe ser un número entero dentro del rango de Long.")));
    }

    @ExceptionHandler(UncheckedIOException.class)
    public ResponseEntity<ErrorRespuesta> almacenamientoNoDisponible() {
        return ResponseEntity.internalServerError().body(new ErrorRespuesta(
                "No se pudieron guardar los cambios. Revisa el archivo de datos e inténtalo nuevamente.",
                Map.of()));
    }

    public record ErrorRespuesta(String mensaje, Map<String, String> errores) {
    }
}
