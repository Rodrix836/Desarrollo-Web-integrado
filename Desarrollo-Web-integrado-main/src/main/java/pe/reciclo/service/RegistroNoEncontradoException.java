package pe.reciclo.service;

public class RegistroNoEncontradoException extends RuntimeException {

    public RegistroNoEncontradoException(long id) {
        super("No existe un registro de residuo con id " + id + ".");
    }
}
