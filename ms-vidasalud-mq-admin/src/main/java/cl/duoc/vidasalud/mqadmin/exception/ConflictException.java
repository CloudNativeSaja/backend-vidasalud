package cl.duoc.vidasalud.mqadmin.exception;

/**
 * Conflicto con el estado actual del broker: ya existe, esta protegido o no esta vacio (HTTP 409).
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
