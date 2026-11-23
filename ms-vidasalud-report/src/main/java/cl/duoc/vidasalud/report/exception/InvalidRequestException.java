package cl.duoc.vidasalud.report.exception;

/** Parametros de consulta invalidos (ej. un rango de tiempo desconocido). */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
