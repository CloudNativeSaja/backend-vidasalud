package cl.duoc.vidasalud.kafkaadmin.exception;

/** El topico o el consumer group no existe. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
