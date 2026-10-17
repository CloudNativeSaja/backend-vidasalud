package cl.duoc.vidasalud.mqadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Solicitud para enlazar una cola a un exchange.
 *
 * Ejemplo: { "exchange": "cmd.topic", "queue": "q.cmd.sms", "routingKey": "sms.*" }
 */
public record BindingRequest(

        @NotBlank(message = "El exchange es obligatorio")
        @Pattern(regexp = ValidationPatterns.RESOURCE_NAME,
                message = "El exchange " + ValidationPatterns.RESOURCE_NAME_MESSAGE)
        String exchange,

        @NotBlank(message = "La cola es obligatoria")
        @Pattern(regexp = ValidationPatterns.RESOURCE_NAME,
                message = "La cola " + ValidationPatterns.RESOURCE_NAME_MESSAGE)
        String queue,

        @NotNull(message = "La routing key es obligatoria (use \"\" para fanout)")
        @Pattern(regexp = ValidationPatterns.ROUTING_KEY,
                message = "La routing key " + ValidationPatterns.ROUTING_KEY_MESSAGE)
        String routingKey

) {
}
