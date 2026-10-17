package cl.duoc.vidasalud.appointments.messaging;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import cl.duoc.vidasalud.appointments.messaging.config.MessagingProperties;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Publica un comando en RabbitMQ con el envelope comun.
 *
 * Mensajes persistentes (delivery mode 2) en exchanges durables: sobreviven
 * a un reinicio del broker. Si RabbitMQ no esta disponible se registra el
 * error y NO se interrumpe la operacion de negocio.
 */
@Component
public class CommandPublisher {

    private static final Logger log = LoggerFactory.getLogger(CommandPublisher.class);
    private static final String TRACE_HEADER = "x-trace-id";

    private final RabbitTemplate rabbitTemplate;
    private final JsonMapper jsonMapper;
    private final MessagingProperties properties;

    public CommandPublisher(RabbitTemplate rabbitTemplate, JsonMapper jsonMapper, MessagingProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.jsonMapper = jsonMapper;
        this.properties = properties;
    }

    /**
     * @return true si el mensaje se entrego al broker; false si fallo la publicacion
     */
    public boolean publish(
            String exchange,
            String routingKey,
            CommandType type,
            String traceId,
            String correlationId,
            Map<String, Object> payload) {

        CommandEnvelope envelope = new CommandEnvelope(
                UUID.randomUUID().toString(),
                type.name(),
                Instant.now().toString(),
                traceId,
                correlationId,
                properties.source(),
                payload);

        try {
            Message message = MessageBuilder.withBody(jsonMapper.writeValueAsBytes(envelope))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setContentEncoding("UTF-8")
                    .setMessageId(envelope.eventId())
                    .setCorrelationId(correlationId)
                    .setType(envelope.type())
                    .setAppId(envelope.source())
                    .setTimestamp(new Date())
                    .setHeader(TRACE_HEADER, traceId)
                    .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                    .build();

            rabbitTemplate.send(exchange, routingKey, message, new CorrelationData(envelope.eventId()));

            log.info("[PUBLISHER] Publicado | exchange={} routingKey={} type={} eventId={} correlationId={}",
                    exchange, routingKey, envelope.type(), envelope.eventId(), correlationId);
            return true;

        } catch (AmqpException | JacksonException ex) {
            log.error("[PUBLISHER] No se pudo publicar (la atencion se guardo igual) | exchange={} routingKey={} type={} motivo={}",
                    exchange, routingKey, envelope.type(), ex.getMessage());
            return false;
        }
    }
}
