package cl.duoc.vidasalud.mqadmin.exception;

/**
 * Parametros invalidos (HTTP 400).
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
