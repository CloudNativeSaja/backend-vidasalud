package cl.duoc.vidasalud.mqadmin.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Publica un comando de prueba (con el envelope comun) para verificar el ruteo.
 *
 * Ejemplo:
 * <pre>
 * {
 *   "exchange": "cmd.topic",
 *   "routingKey": "email.prueba",
 *   "type": "EMAIL_PRUEBA",
 *   "payload": { "appointmentId": "99", "patientId": "FALLA-TEMPORAL" }
 * }
 * </pre>
 */
public record PublishMessageRequest(

        @NotBlank(message = "El exchange es obligatorio")
        @Pattern(regexp = ValidationPatterns.RESOURCE_NAME,
                message = "El exchange " + ValidationPatterns.RESOURCE_NAME_MESSAGE)
        String exchange,

        @NotNull(message = "La routing key es obligatoria")
        @Pattern(regexp = "^[A-Za-z0-9._-]{0,255}$",
                message = "La routing key de publicacion solo admite letras, numeros, '.', '_' y '-'")
        String routingKey,

        @NotBlank(message = "El tipo de comando es obligatorio")
        @Pattern(regexp = "^[A-Z][A-Z0-9_]{2,59}$",
                message = "El tipo debe estar en MAYUSCULAS_CON_GUION_BAJO (3 a 60 caracteres)")
        String type,

        @NotNull(message = "El payload es obligatorio")
        @Size(max = 50, message = "El payload admite como maximo 50 campos")
        Map<String, Object> payload

) {
}
