package cl.duoc.vidasalud.notify.messaging;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import cl.duoc.vidasalud.notify.config.MessagingProperties;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Politica unica de confirmacion (ACK) y manejo de errores de los consumidores.
 *
 * <pre>
 * Situacion                                  Decision                         Resultado
 * ------------------------------------------ -------------------------------- --------------------------
 * Procesado OK                               basicAck                         se elimina de la cola
 * eventId ya procesado (duplicado)           basicAck (sin reprocesar)        idempotencia
 * JSON invalido / faltan campos              basicNack(requeue=false)         DLQ inmediato
 * InvalidCommandException (dato invalido)    basicNack(requeue=false)         DLQ inmediato
 * RecoverableProcessingException,            republica con x-retry-count+1    reintento con espera
 *   intento < max-attempts                     y basicAck del original
 * RecoverableProcessingException,            basicNack(requeue=false)         DLQ
 *   intento = max-attempts
 * Cualquier otro error inesperado            basicNack(requeue=false)         DLQ (no se arriesga loop)
 * </pre>
 *
 * Nunca se usa requeue=true: asi un mensaje "envenenado" no queda en loop
 * infinito bloqueando la cola. Todo NACK queda registrado en el log con
 * eventId, tipo, cola y motivo; RabbitMQ agrega el header x-death al llegar
 * a la DLQ.
 */
@Component
public class MessageAckHandler {

    /** Header propio que cuenta los reintentos ya realizados. */
    public static final String RETRY_COUNT_HEADER = "x-retry-count";

    private static final Logger log = LoggerFactory.getLogger(MessageAckHandler.class);

    private final JsonMapper jsonMapper;
    private final RabbitTemplate rabbitTemplate;
    private final ProcessedEventRegistry registry;
    private final MessagingProperties.Consumer consumerProperties;

    public MessageAckHandler(
            JsonMapper jsonMapper,
            RabbitTemplate rabbitTemplate,
            ProcessedEventRegistry registry,
            MessagingProperties properties) {

        this.jsonMapper = jsonMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.registry = registry;
        this.consumerProperties = properties.consumer();
    }

    /**
     * Procesa un mensaje y decide ACK, reintento o NACK hacia la DLQ.
     *
     * @param domain  nombre del dominio para los logs (EMAIL, ADMISSION, RECORD)
     * @param message mensaje AMQP crudo recibido por el listener
     * @param channel canal AMQP para confirmar manualmente
     * @param handler logica de negocio del dominio
     */
    public void process(String domain, Message message, Channel channel, CommandHandler handler) {

        MessageProperties props = message.getMessageProperties();
        long deliveryTag = props.getDeliveryTag();
        int attempt = currentAttempt(props);

        CommandEnvelope command;
        try {
            command = parse(message);
        } catch (InvalidCommandException ex) {
            log.error("[{}] NACK -> DLQ | mensaje invalido | cola={} motivo={}",
                    domain, props.getConsumerQueue(), ex.getMessage());
            nack(channel, deliveryTag, domain);
            return;
        }

        if (registry.alreadyProcessed(command.eventId())) {
            log.warn("[{}] ACK (duplicado ignorado) | eventId={} type={}",
                    domain, command.eventId(), command.type());
            ack(channel, deliveryTag, domain);
            return;
        }

        try {
            log.info("[{}] Recibido | eventId={} type={} correlationId={} intento={}/{}",
                    domain, command.eventId(), command.type(), command.correlationId(),
                    attempt, consumerProperties.maxAttempts());

            handler.handle(command);

            registry.markProcessed(command.eventId());
            ack(channel, deliveryTag, domain);
            log.info("[{}] ACK | eventId={} type={}", domain, command.eventId(), command.type());

        } catch (RecoverableProcessingException ex) {

            if (attempt < consumerProperties.maxAttempts()) {
                log.warn("[{}] Error recuperable, se reintentara | eventId={} intento={}/{} motivo={}",
                        domain, command.eventId(), attempt, consumerProperties.maxAttempts(), ex.getMessage());
                retry(message, channel, deliveryTag, attempt, domain);
            } else {
                log.error("[{}] NACK -> DLQ | reintentos agotados | eventId={} type={} intentos={} motivo={}",
                        domain, command.eventId(), command.type(), attempt, ex.getMessage());
                nack(channel, deliveryTag, domain);
            }

        } catch (InvalidCommandException ex) {
            log.error("[{}] NACK -> DLQ | error no recuperable | eventId={} type={} motivo={}",
                    domain, command.eventId(), command.type(), ex.getMessage());
            nack(channel, deliveryTag, domain);

        } catch (RuntimeException ex) {
            log.error("[{}] NACK -> DLQ | error inesperado | eventId={} type={} motivo={}",
                    domain, command.eventId(), command.type(), ex.getMessage(), ex);
            nack(channel, deliveryTag, domain);
        }
    }

    // -----------------------------------------------------------------
    // Pasos internos
    // -----------------------------------------------------------------

    private CommandEnvelope parse(Message message) {
        CommandEnvelope command;
        try {
            command = jsonMapper.readValue(message.getBody(), CommandEnvelope.class);
        } catch (JacksonException ex) {
            throw new InvalidCommandException("JSON invalido: " + ex.getMessage(), ex);
        }
        if (command == null || isBlank(command.eventId()) || isBlank(command.type())) {
            throw new InvalidCommandException("El envelope debe incluir eventId y type");
        }
        return command;
    }

    /** Intento actual = reintentos previos (header) + 1. */
    private int currentAttempt(MessageProperties props) {
        Object header = props.getHeaders().get(RETRY_COUNT_HEADER);
        if (header instanceof Number number) {
            return number.intValue() + 1;
        }
        return 1;
    }

    /**
     * Reintento controlado: espera (backoff lineal), vuelve a publicar una copia
     * en la MISMA cola (exchange por defecto + nombre de la cola) con el header
     * x-retry-count incrementado y confirma el original.
     */
    private void retry(Message message, Channel channel, long deliveryTag, int attempt, String domain) {
        String queue = message.getMessageProperties().getConsumerQueue();
        try {
            Thread.sleep(consumerProperties.retryDelayMs() * attempt);

            Message copy = MessageBuilder.fromMessage(message)
                    .setHeader(RETRY_COUNT_HEADER, attempt)
                    .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                    .build();

            rabbitTemplate.send("", queue, copy);
            ack(channel, deliveryTag, domain);

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("[{}] Reintento interrumpido, NACK -> DLQ | cola={}", domain, queue);
            nack(channel, deliveryTag, domain);
        } catch (RuntimeException ex) {
            log.error("[{}] No se pudo republicar el reintento, NACK -> DLQ | cola={} motivo={}",
                    domain, queue, ex.getMessage());
            nack(channel, deliveryTag, domain);
        }
    }

    private void ack(Channel channel, long deliveryTag, String domain) {
        try {
            channel.basicAck(deliveryTag, false);
        } catch (IOException ex) {
            log.error("[{}] No se pudo enviar ACK (deliveryTag={}): {}", domain, deliveryTag, ex.getMessage());
        }
    }

    /** NACK sin requeue: RabbitMQ lo enruta al DLX configurado en la cola. */
    private void nack(Channel channel, long deliveryTag, String domain) {
        try {
            channel.basicNack(deliveryTag, false, false);
        } catch (IOException ex) {
            log.error("[{}] No se pudo enviar NACK (deliveryTag={}): {}", domain, deliveryTag, ex.getMessage());
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
