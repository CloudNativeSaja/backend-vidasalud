package cl.duoc.vidasalud.kafkaadmin.exception;

/** Solicitud invalida (ej. una configuracion no permitida o un valor fuera de rango). */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
