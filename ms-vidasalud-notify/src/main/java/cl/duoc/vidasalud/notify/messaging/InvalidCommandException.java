package cl.duoc.vidasalud.notify.messaging;

/**
 * Error NO recuperable: el mensaje esta mal formado o sus datos son invalidos.
 * Reintentar no sirve, por lo que el mensaje va directo a la DLQ.
 */
public class InvalidCommandException extends RuntimeException {

    public InvalidCommandException(String message) {
        super(message);
    }

    public InvalidCommandException(String message, Throwable cause) {
        super(message, cause);
    }
}
