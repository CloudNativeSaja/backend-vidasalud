package cl.duoc.vidasalud.kafkaadmin.exception;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.kafka.common.errors.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/** Traduce los errores a respuestas JSON claras con el codigo HTTP correcto. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBodyValidation(MethodArgumentNotValidException ex, WebRequest request) {
        List<String> details = ex.getBindingResult().getAllErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Datos de entrada invalidos", details, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es un JSON valido", List.of(), request);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiError> handleInvalid(InvalidRequestException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of(), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, WebRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), List.of(), request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex, WebRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), List.of(), request);
    }

    /** Kafka no responde (brokers caidos o sin red). */
    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<ApiError> handleKafkaDown(TimeoutException ex, WebRequest request) {
        log.error("Kafka no disponible: {}", ex.getMessage());
        return build(HttpStatus.SERVICE_UNAVAILABLE, "Kafka no esta disponible", List.of(), request);
    }

    @ExceptionHandler(org.apache.kafka.common.KafkaException.class)
    public ResponseEntity<ApiError> handleKafka(org.apache.kafka.common.KafkaException ex, WebRequest request) {
        log.error("Kafka rechazo la operacion: {}", ex.getMessage());
        return build(HttpStatus.BAD_GATEWAY, "Kafka rechazo la operacion: " + ex.getMessage(), List.of(), request);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, List<String> details, WebRequest request) {
        ApiError error = new ApiError(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                details,
                request.getDescription(false).replace("uri=", ""));
        return ResponseEntity.status(status).body(error);
    }
}
