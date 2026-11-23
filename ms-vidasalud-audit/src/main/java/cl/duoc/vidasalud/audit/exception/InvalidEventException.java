package cl.duoc.vidasalud.audit.exception;

/** El evento recibido no cumple el contrato (campos faltantes o invalidos). */
public class InvalidEventException extends RuntimeException {

    public InvalidEventException(String message) {
        super(message);
    }
}
