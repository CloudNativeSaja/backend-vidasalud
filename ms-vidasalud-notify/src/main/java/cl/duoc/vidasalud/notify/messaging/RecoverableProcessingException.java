package cl.duoc.vidasalud.notify.messaging;

/**
 * Error RECUPERABLE: falla temporal (ej. proveedor de email caido, timeout).
 * El mensaje se reintenta hasta max-attempts; si sigue fallando va a la DLQ.
 */
public class RecoverableProcessingException extends RuntimeException {

    public RecoverableProcessingException(String message) {
        super(message);
    }

    public RecoverableProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
