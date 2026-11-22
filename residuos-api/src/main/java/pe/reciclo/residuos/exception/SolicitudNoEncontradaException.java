package pe.reciclo.residuos.exception;

public class SolicitudNoEncontradaException extends RuntimeException {
    public SolicitudNoEncontradaException(long id) {
        super("No existe una solicitud de intercambio con id " + id + ".");
    }
}
