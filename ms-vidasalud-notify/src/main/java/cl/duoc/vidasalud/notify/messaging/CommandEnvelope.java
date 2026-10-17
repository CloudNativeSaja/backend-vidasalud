package cl.duoc.vidasalud.notify.messaging;

import java.util.Map;

/**
 * Envelope comun de todos los comandos que viajan por RabbitMQ.
 *
 * @param eventId       identificador unico del mensaje (clave de idempotencia)
 * @param type          tipo de comando, ej. EMAIL_CITA_CONFIRMADA
 * @param timestamp     instante de emision (ISO-8601)
 * @param traceId       traza de la operacion que origino el comando
 * @param correlationId agrupa todos los comandos de una misma atencion
 * @param source        microservicio productor
 * @param payload       datos del negocio (atencion, paciente, box, etc.)
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

    /** Lee un campo del payload como texto ("" si no existe). */
    public String payloadValue(String key) {
        if (payload == null || payload.get(key) == null) {
            return "";
        }
        return String.valueOf(payload.get(key));
    }
}
