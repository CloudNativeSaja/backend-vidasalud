package cl.duoc.vidasalud.mqadmin.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * Solicitud para crear una cola.
 *
 * Ejemplo:
 * <pre>
 * {
 *   "name": "q.cmd.sms",
 *   "durable": true,
 *   "type": "classic",
 *   "deadLetterExchange": "cmd.dead.dlx",
 *   "withDeadLetterQueue": true,
 *   "messageTtlMs": 60000
 * }
 * </pre>
 */
public record CreateQueueRequest(

        @NotBlank(message = "El nombre de la cola es obligatorio")
        @Pattern(regexp = ValidationPatterns.RESOURCE_NAME,
                message = "El nombre de la cola " + ValidationPatterns.RESOURCE_NAME_MESSAGE)
        String name,

        /** true por defecto: la cola sobrevive a un reinicio del broker. */
        Boolean durable,

        @Pattern(regexp = "classic|quorum", message = "El tipo de cola debe ser 'classic' o 'quorum'")
        String type,

        @Pattern(regexp = ValidationPatterns.RESOURCE_NAME,
                message = "El dead letter exchange " + ValidationPatterns.RESOURCE_NAME_MESSAGE)
        String deadLetterExchange,

        @Pattern(regexp = ValidationPatterns.ROUTING_KEY,
                message = "La dead letter routing key " + ValidationPatterns.ROUTING_KEY_MESSAGE)
        String deadLetterRoutingKey,

        /** Si es true crea tambien "<name>.dlq" enlazada al dead letter exchange. */
        Boolean withDeadLetterQueue,

        @Positive(message = "El TTL debe ser mayor a 0")
        @Max(value = 1_209_600_000L, message = "El TTL maximo es 14 dias (1209600000 ms)")
        Long messageTtlMs,

        @Positive(message = "El largo maximo debe ser mayor a 0")
        @Max(value = 1_000_000L, message = "El largo maximo permitido es 1000000 mensajes")
        Long maxLength

) {

    public boolean durableOrDefault() {
        return durable == null || durable;
    }

    public String typeOrDefault() {
        return type == null ? "classic" : type;
    }

    public boolean withDeadLetterQueueOrDefault() {
        return Boolean.TRUE.equals(withDeadLetterQueue);
    }

    @AssertTrue(message = "deadLetterRoutingKey y withDeadLetterQueue requieren deadLetterExchange")
    public boolean isDeadLetterConfigValid() {
        boolean usesDeadLetter = deadLetterRoutingKey != null || withDeadLetterQueueOrDefault();
        return !usesDeadLetter || (deadLetterExchange != null && !deadLetterExchange.isBlank());
    }

    @AssertTrue(message = "Una cola quorum debe ser durable")
    public boolean isQuorumDurable() {
        return !"quorum".equals(type) || durableOrDefault();
    }
}
