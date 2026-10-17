package cl.duoc.vidasalud.mqadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Solicitud para crear un exchange.
 *
 * Ejemplo: { "name": "cmd.fanout", "type": "fanout", "durable": true }
 */
public record CreateExchangeRequest(

        @NotBlank(message = "El nombre del exchange es obligatorio")
        @Pattern(regexp = ValidationPatterns.RESOURCE_NAME,
                message = "El nombre del exchange " + ValidationPatterns.RESOURCE_NAME_MESSAGE)
        String name,

        @NotBlank(message = "El tipo de exchange es obligatorio")
        @Pattern(regexp = "(?i)direct|topic|fanout|headers",
                message = "El tipo de exchange debe ser direct, topic, fanout o headers")
        String type,

        Boolean durable

) {

    public boolean durableOrDefault() {
        return durable == null || durable;
    }
}
