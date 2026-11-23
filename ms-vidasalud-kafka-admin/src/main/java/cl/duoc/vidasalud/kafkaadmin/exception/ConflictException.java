package cl.duoc.vidasalud.kafkaadmin.exception;

/** La operacion choca con el estado actual (ej. el topico ya existe o es un topico protegido). */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
