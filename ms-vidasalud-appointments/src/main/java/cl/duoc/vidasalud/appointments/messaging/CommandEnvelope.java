package cl.duoc.vidasalud.appointments.messaging;

import java.util.Map;

/**
 * Envelope comun de los comandos publicados en RabbitMQ.
 * Misma estructura que lee ms-vidasalud-notify.
 */
public record CommandEnvelope(
        String eventId,
        String type,
        String timestamp,
        String traceId,
        String correlationId,
        String source,
        Map<String, Object> payload
) {
}
