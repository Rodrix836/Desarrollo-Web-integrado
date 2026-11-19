package pe.reciclo.residuos.exception;

import java.util.Map;

public class DatosIntercambioInvalidosException extends RuntimeException {
    private final Map<String, String> errores;

    public DatosIntercambioInvalidosException(Map<String, String> errores) {
        super("Revisa los datos del intercambio.");
        this.errores = Map.copyOf(errores);
    }

    public Map<String, String> errores() { return errores; }
}
