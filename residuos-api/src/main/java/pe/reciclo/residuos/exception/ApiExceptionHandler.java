package pe.reciclo.residuos.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.Errors;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;
import java.util.TreeMap;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final String ID_INVALIDO = "El id debe ser un número entero positivo dentro del rango de Long.";

    @ExceptionHandler(RegistroNoEncontradoException.class)
    ResponseEntity<ErrorRespuesta> noEncontrado(RegistroNoEncontradoException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ErrorRespuesta> cambioConcurrente() {
        return error(HttpStatus.CONFLICT, "Otro usuario modificó el registro. Vuelve a consultar los datos.", Map.of());
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ErrorRespuesta> errorPersistencia(DataAccessException ex) {
        LOG.error("Falló el acceso a la base de residuos.", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo completar la operación en la base de datos.", Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorRespuesta> errorInesperado(Exception ex) {
        LOG.error("Falló una solicitud a la API de Residuos.", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado.", Map.of());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errores = new TreeMap<>();
        agregarErrores(ex.getBindingResult(), errores);
        return new ResponseEntity<>(ErrorRespuesta.de(status, "Entrada inválida.", errores), headers, status);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errores = new TreeMap<>();
        ex.getParameterValidationResults().forEach(resultado -> {
            if (resultado instanceof ParameterErrors cuerpo) {
                agregarErrores(cuerpo, errores);
            } else {
                errores.put("id", ID_INVALIDO);
            }
        });
        String mensaje = errores.size() == 1 && errores.containsKey("id")
                ? "Identificador inválido." : "Entrada inválida.";
        return new ResponseEntity<>(ErrorRespuesta.de(status, mensaje, errores), headers, status);
    }

    private void agregarErrores(Errors origen, Map<String, String> errores) {
        origen.getFieldErrors().forEach(campo ->
                errores.putIfAbsent(campo.getField().equals("empresaValida") ? "empresa" : campo.getField(),
                        campo.getDefaultMessage()));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String mensaje = "JSON inválido. Revisa el formato del cuerpo, la fecha (yyyy-MM-dd) y el tipo de residuo.";
        return new ResponseEntity<>(ErrorRespuesta.de(status, mensaje, Map.of()), headers, status);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        if (ex instanceof MethodArgumentTypeMismatchException argumento && argumento.getName().equals("id")) {
            return new ResponseEntity<>(
                    ErrorRespuesta.de(status, "Identificador inválido.", Map.of("id", ID_INVALIDO)), headers, status);
        }
        return super.handleTypeMismatch(ex, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String mensaje = switch (status.value()) {
            case 404 -> "La ruta solicitada no existe.";
            case 405 -> "El método HTTP no está disponible para esta ruta.";
            case 415 -> "El tipo de contenido de la solicitud no es compatible.";
            default -> "No se pudo procesar la solicitud.";
        };
        return super.handleExceptionInternal(ex, ErrorRespuesta.de(status, mensaje, Map.of()), headers, status, request);
    }

    private ResponseEntity<ErrorRespuesta> error(HttpStatus estado, String mensaje, Map<String, String> errores) {
        return ResponseEntity.status(estado).body(ErrorRespuesta.de(estado, mensaje, errores));
    }
}
