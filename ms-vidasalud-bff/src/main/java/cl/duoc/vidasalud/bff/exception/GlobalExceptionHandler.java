package cl.duoc.vidasalud.bff.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Traduce los errores de los microservicios internos para que el frontend reciba
 * el codigo y el mensaje reales (400, 404, 409...) en lugar de un 500 generico.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** El microservicio respondio con un error 4xx/5xx: se reenvia tal cual (codigo y cuerpo). */
    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<String> handleDownstreamError(RestClientResponseException ex) {

        log.warn("Microservicio respondio {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());

        String body = ex.getResponseBodyAsString();

        if (body == null || body.isBlank()) {
            body = "{\"message\":\"" + ex.getStatusText() + "\"}";
        }

        return ResponseEntity
            .status(ex.getStatusCode())
            .contentType(MediaType.APPLICATION_JSON)
            .body(body);
    }

    /** El microservicio no esta disponible (caido, sin red, timeout). */
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDownstreamUnavailable(ResourceAccessException ex) {

        log.error("Microservicio interno no disponible", ex);

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("timestamp", LocalDateTime.now().toString());
        error.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        error.put("error", "Service Unavailable");
        error.put("message", "El servicio interno no esta disponible. Intente nuevamente.");

        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(error);
    }
}
