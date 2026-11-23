package cl.duoc.vidasalud.report.exception;

/** El evento recibido no cumple el contrato (campos faltantes o invalidos). */
public class InvalidEventException extends RuntimeException {

    public InvalidEventException(String message) {
        super(message);
    }
}
