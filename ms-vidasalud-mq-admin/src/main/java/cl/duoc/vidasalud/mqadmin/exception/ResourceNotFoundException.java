package cl.duoc.vidasalud.mqadmin.exception;

/**
 * Recurso inexistente en RabbitMQ (HTTP 404).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
