package cl.duoc.vidasalud.mqadmin.dto;

/**
 * Estado de una cola (datos de RabbitMQ Management).
 */
public record QueueResponse(
        String name,
        String type,
        boolean durable,
        long messages,
        long messagesReady,
        long messagesUnacknowledged,
        long consumers,
        String node,
        String deadLetterExchange,
        String deadLetterRoutingKey,
        boolean protectedResource
) {
}
